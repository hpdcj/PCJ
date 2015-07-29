/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal;

import org.pcj.internal.utils.BlackholePrintStream;
import org.pcj.internal.utils.Version;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.pcj.Group;
import org.pcj.internal.message.MessageFinished;
import org.pcj.internal.message.MessageGroupJoinQuery;
import org.pcj.internal.message.MessageGroupJoinRequest;
import org.pcj.internal.message.MessageHello;
import org.pcj.internal.network.LoopbackSocketChannel;
import org.pcj.internal.storage.InternalStorage;
import org.pcj.internal.utils.BitMask;
import org.pcj.internal.utils.Configuration;
import org.pcj.internal.utils.ExitTimer;
import org.pcj.internal.utils.NetworkUtils;
import org.pcj.internal.utils.NodeInfo;
import org.pcj.internal.utils.NodesFile;
import org.pcj.internal.utils.Utilities;
import org.pcj.internal.utils.WaitObject;

/**
 * Internal (with common ClassLoader) class for external PCJ class.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public abstract class InternalPCJ {

    final private static PrintStream stdout = System.out;
    final private static PrintStream stderr = System.err;
    private static SocketChannel node0Socket;
    private static Networker networker;
    private static WorkerData workerData;

    // Suppress default constructor for noninstantiability
    // have to be protected to allow to inheritance
    protected InternalPCJ() {
        throw new AssertionError();
    }

    protected static void deploy(Class<? extends InternalStartPoint> startPoint,
            Class<? extends InternalStorage> storage, NodesFile nodesFile) {
        NodeInfo node0 = nodesFile.getNode0();
        NodeInfo localNode = nodesFile.getLocalNode();
        Integer clientsCount = nodesFile.getClientsCount();

        DeployPCJ deploy = new DeployPCJ(node0, clientsCount, startPoint, storage);

        for (NodeInfo node : nodesFile.getAllNodes()) {
            if (node.equals(localNode)) {
                continue;
            }
            try {
                if (node.isLocalNode()) {
                    deploy.runJVM(node);
                } else {
                    deploy.runSSH(node);
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        if (localNode != null) {
            deploy.runPCJ(localNode);
        }

        try {
            deploy.waitFor();
        } catch (InterruptedException ex) {
            ex.printStackTrace(System.err);
        }
    }

    protected static void start(Class<? extends InternalStartPoint> startPoint,
            Class<? extends InternalStorage> storage,
            NodesFile nodesFile) {
        NodeInfo node0 = nodesFile.getNode0();
        NodeInfo localNode = nodesFile.getLocalNode();

        try {
            start(startPoint, storage, node0, localNode, nodesFile.getClientsCount());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected static void start(Class<? extends InternalStartPoint> startPoint,
            Class<? extends InternalStorage> storage,
            NodeInfo node0, // node0 - work also as manager
            NodeInfo localNode,
            Integer clientsCount) throws IOException {
        if (localNode == null) {
            throw new NullPointerException("localNode is null");
        }
        if (node0 == null) {
            throw new NullPointerException("node0 is null");
        }

        ConcurrentMap<Integer, PcjThreadLocalData> localData;
        boolean isNode0 = NetworkUtils.isLocalAddress(node0.getHostname())
                && node0.getPort() == localNode.getPort();

        if (isNode0) {
            System.err.println("PCJ version " + Version.version + " built on " + Version.builtDate + ".");
        }

        int[] localIds = localNode.getLocalIds();
        Arrays.sort(localIds);

        /* create InternalPCJ InternalStorage and Threads */
        Thread[] nodeThreads = new Thread[localIds.length];

        boolean theSame = startPoint.equals(storage);
        localData = new ConcurrentHashMap<>();
        InternalGroup globalGroup = new InternalGroup(0, "");
        try {
            for (int i = 0; i < localIds.length; ++i) {
                int localId = localIds[i];

                ClassLoader classLoader = new PcjClassLoader();

                InternalStartPoint lStartPoint;

                /* create storage */
                @SuppressWarnings("unchecked")
                Class<? extends InternalStorage> classStorage = (Class<? extends InternalStorage>) classLoader.loadClass(storage.getName());
                Constructor<? extends InternalStorage> constructorStorage = classStorage.getDeclaredConstructor();
                constructorStorage.setAccessible(true);
                InternalStorage lStorage = constructorStorage.newInstance();

                /* create startpoint */
                if (theSame) {
                    lStartPoint = (InternalStartPoint) lStorage;
                } else {
                    @SuppressWarnings("unchecked")
                    Class<? extends InternalStartPoint> classStartPoint = (Class<? extends InternalStartPoint>) classLoader.loadClass(startPoint.getName());
                    Constructor<? extends InternalStartPoint> constructorStartPoint = classStartPoint.getDeclaredConstructor();
                    constructorStartPoint.setAccessible(true);
                    lStartPoint = constructorStartPoint.newInstance();
                }

                /* create placeholders for groups */
                Map<Integer, InternalGroup> groups = new HashMap<>();
                Map<String, InternalGroup> groupsByName = new HashMap<>();

                /* create globalGroup */
                Class<?> groupClass = classLoader.loadClass(Group.class.getName());
                Constructor<?> constructor = groupClass.getDeclaredConstructor(int.class, InternalGroup.class);
                constructor.setAccessible(true);
                InternalGroup lGroup = (InternalGroup) constructor.newInstance(localId, globalGroup);

                /* put globalGroup to placeholders for groups */
                groups.put(0, lGroup);
                groupsByName.put("", lGroup);

                /* create PcjThreadLocalData and PcjThread*/
                PcjThreadLocalData data = new PcjThreadLocalData(classLoader, lStorage, groups, groupsByName);
                localData.put(localId, data);
                nodeThreads[i] = new PcjThread(localId, lStartPoint, data);
                nodeThreads[i].setContextClassLoader(classLoader);
            }
        } catch (final ClassNotFoundException | InstantiationException | IllegalAccessException |
                NoSuchMethodException | IllegalArgumentException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }

        final ExitTimer timer = new ExitTimer();
        try {
            /* creating timer for interrupting after maximum startup time */
            if (Configuration.WAIT_TIME > 0) {
                String localHostname;
                try {
                    localHostname = InetAddress.getLocalHost().getHostName();
                } catch (UnknownHostException ex) {
                    localHostname = "<" + ex.getLocalizedMessage() + ">";
                }
                timer.schedule(Configuration.WAIT_TIME * 1000, localHostname + Arrays.toString(localIds) + ": Waiting too log for connection. Exiting!");
            }

            /* WorkerData contain physicalId, localData map and globalGroup reference */
            workerData = new WorkerData(localIds, localData, globalGroup, isNode0 ? clientsCount : null);

            Worker worker = new Worker(workerData);
            networker = new Networker(worker);
            networker.startup();

            worker.setNetworker(networker);

            /* binding... */
            for (int attempt = 0; attempt <= Configuration.RETRY_COUNT; ++attempt) {
                try {
                    for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                        for (InetAddress inetAddress : Collections.list(networkInterface.getInetAddresses())) {
                            networker.bind(inetAddress, localNode.getPort());
                        }
                    }
//                    networker.bind(null, localNode.getPort());
                    break;
                } catch (IOException ex) {
                    Utilities.sleep(Configuration.RETRY_DELAY * 100);
                    if (attempt + 1 >= Configuration.RETRY_COUNT) {
                        throw new RuntimeException("Binding on port " + localNode.getPort() + " failed!", ex);
                    }
                }
            }

            /* try to connect to node0 */
            if (isNode0 == false) {
                /* node0 - manager */
                Utilities.sleep(250);
                node0Socket = networker.connectTo(InetAddress.getByName(node0.getHostname()), node0.getPort(), Configuration.RETRY_COUNT, Configuration.RETRY_DELAY);
            } else {
                node0Socket = LoopbackSocketChannel.getInstance();
            }

            /* send HELLO message */
            MessageHello msg = new MessageHello();
            msg.setPort(localNode.getPort());
            msg.setNodeIds(localIds);
            WaitObject sync = globalGroup.getSyncObject();
            sync.lock();
            try {
                networker.send(node0Socket, msg);

                /* wait for STARTUP */
                try {
                    sync.await();
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            } finally {
                sync.unlock();
            }
        } finally {
            /* startup finished destroying timer */
            if (Configuration.WAIT_TIME > 0) {
                timer.stop();
            }
        }

        if (isNode0) {
            System.err.println("Starting " + startPoint.getName() + " with " + clientsCount + " thread(s)...");
        }

        try {
            if (isNode0 == false || Configuration.REDIRECT_NODE0 == true) {
                if (Configuration.REDIRECT_OUT) {
                    System.setOut(new BlackholePrintStream());
                }
                if (Configuration.REDIRECT_ERR) {
                    System.setErr(new BlackholePrintStream());
                }
            }

            /* run InternalPCJ Threads */
            for (int i = 0; i < localIds.length; ++i) {
                nodeThreads[i].start();
            }

            /* wait for end of threads */
            try {
                for (int i = 0; i < localIds.length; ++i) {
                    nodeThreads[i].join();
                }
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }

            MessageFinished msg = new MessageFinished();
            synchronized (workerData.finishObject) {
                networker.send(node0Socket, msg);
                try {
                    workerData.finishObject.wait();
                } catch (InterruptedException ex) {
                    ex.printStackTrace(System.err);
                }
            }
//        } catch (Throwable t) {
//            System.setOut(stdout);
//            System.setErr(stderr);
        } finally {
            System.setOut(stdout);
            System.setErr(stderr);
            networker.shutdown();
        }
    }

    static Networker getNetworker() {
        return networker;
    }

    static WorkerData getWorkerData() {
        return workerData;
    }

    protected static SocketChannel getNode0Socket() {
        return node0Socket;
    }

    protected static int getPhysicalNodeId() {
        return workerData.physicalId;
    }

    protected static InternalGroup join(int myNodeId, String groupName) {
        InternalGroup group = PcjThread.threadGroup(groupName);
        if (group != null) {
            return group;
        }

        synchronized (workerData.internalGroupsById) {
            group = workerData.internalGroupsByName.get(groupName);
        }
        MessageGroupJoinQuery msgQuery = new MessageGroupJoinQuery();
        msgQuery.setGroupName(groupName);

        try {
            int masterPhysicalId;
            int groupId;
            if (group == null) {
                Object[] objs = networker.sendWait(node0Socket, msgQuery);
                masterPhysicalId = (int) objs[0];
                groupId = (int) objs[1];
            } else {
                masterPhysicalId = group.getPhysicalMaster();
                groupId = group.getGroupId();
            }

            MessageGroupJoinRequest msgJoin = new MessageGroupJoinRequest();
            msgJoin.setGroupId(groupId);
            msgJoin.setGlobaNodelId(myNodeId);
            msgJoin.setGroupName(groupName);
            group = networker.sendWait(workerData.physicalNodes.get(masterPhysicalId), msgJoin);

            BitMask mask = group.getJoinBitmask(group.myId());
            synchronized (mask) {
                while (mask.isSet() == false) {
                    try {
                        mask.wait();
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
            return group;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @return the stdout
     */
    protected static PrintStream getStdout() {
        return stdout;
    }

    /**
     * @return the stderr
     */
    protected static PrintStream getStderr() {
        return stderr;
    }
}
