/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pcj.NodesDescription;
import org.pcj.Shared;
import org.pcj.StartPoint;
import org.pcj.internal.futures.WaitObject;
import org.pcj.internal.message.MessageBye;
import org.pcj.internal.message.MessageHello;
import org.pcj.internal.network.LoopbackSocketChannel;

/**
 * Internal class for external PCJ class.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public abstract class InternalPCJ {

    private static final Logger LOGGER = Logger.getLogger(InternalPCJ.class.getName());
    private static final String PCJ_VERSION;
    private static final String PCJ_BUILD_DATE;
    private static SocketChannel node0Socket;
    private static Networker networker;
    private static NodeData nodeData;

    /* Suppress default constructor for noninstantiability. 
     * Have to be protected to allow inheritance */
    protected InternalPCJ() {
        throw new AssertionError();
    }

    static {
        Package p = InternalPCJ.class.getPackage();
        PCJ_VERSION = p.getImplementationVersion() == null ? "UNKNOWN" : p.getImplementationVersion();
        PCJ_BUILD_DATE = p.getImplementationTitle() == null ? "UNKNOWN" : p.getImplementationTitle();
    }

    protected static void start(Class<? extends StartPoint> startPoint,
            NodesDescription nodesFile,
            List<Class<? extends Enum<? extends Shared>>> storages) {
        NodeInfo node0 = nodesFile.getNode0();
        NodeInfo currentJvm = nodesFile.getCurrentJvm();
        int allNodesThreadCount = nodesFile.getAllNodesThreadCount();
        start(startPoint, node0, currentJvm, allNodesThreadCount, storages);
    }

    protected static void start(Class<? extends StartPoint> startPointClass,
            NodeInfo node0, NodeInfo currentJvm, int allNodesThreadCount,
            List<Class<? extends Enum<? extends Shared>>> storages) {

        if (currentJvm == null) {
            throw new IllegalArgumentException("There is no entry for PCJ threads for current JVM");
        }
        if (node0 == null) {
            throw new IllegalArgumentException("There is no node0 entry in nodes description file.");
        }

        boolean isCurrentJvmNode0 = node0.isLocalAddress()
                && node0.getPort() == currentJvm.getPort();

        if (isCurrentJvmNode0) {
            LOGGER.log(Level.INFO, "PCJ version {0} built on {1}.",
                    new Object[]{PCJ_VERSION, PCJ_BUILD_DATE});
        }

        networker = new Networker();
        networker.startup();
        try {
            /* Getting all interfaces */
            Queue<InetAddress> inetAddresses = getHostAllNetworkInferfaces();

            /* Binding on all interfaces */
            bindOnAllNetworkInterfaces(inetAddresses, currentJvm.getPort());

            ScheduledFuture<?> exitTimer = sheduleExitTimer(Thread.currentThread());

            /* connecting to node0 */
            connectToNode0(isCurrentJvmNode0, node0);

            /* Prepare initial data */
            nodeData = new NodeData(isCurrentJvmNode0);
            nodeData.getSocketChannelByPhysicalId().put(0, node0Socket);

            if (isCurrentJvmNode0) {
                nodeData.getNode0Data().setAllNodesThreadCount(allNodesThreadCount);
            }

            /* send HELLO message */
            helloPhase(currentJvm.getPort(), currentJvm.getThreadIds());

            exitTimer.cancel(true);

            long nanoTime = System.nanoTime();
            /* Starting execution */
            if (isCurrentJvmNode0) {
                LOGGER.log(Level.INFO, "Starting {0} with {1,number,#} {1,choice,1#thread|1<threads}...",
                        new Object[]{startPointClass.getName(),
                            nodeData.getGroupById(InternalCommonGroup.GLOBAL_GROUP_ID).threadCount()});
            }


            /* Preparing PcjThreads*/
            Set<PcjThread> pcjThreads = preparePcjThreads(startPointClass, currentJvm.getThreadIds(), storages);
            pcjThreads.forEach(pcjThread -> nodeData.getPcjThreads().put(pcjThread.getThreadId(), pcjThread));

            /* Starting PcjThreads*/
            pcjThreads.forEach(pcjThread -> pcjThread.start());

            /* Waiting for all threads complete */
            waitForPcjThreadsComplete(pcjThreads);

            if (isCurrentJvmNode0) {
                long timer = (System.nanoTime() - nanoTime) / 1_000_000_000L;
                long h = timer / (60 * 60);
                long m = (timer / 60) % 60;
                long s = (timer % 60);

                LOGGER.log(Level.INFO, "Completed {0} with {1,number,#} {1,choice,1#thread|1<threads}"
                        + " after {2,number,#}h {3,number,#}m {4,number,#}s.",
                        new Object[]{
                            startPointClass.getName(),
                            nodeData.getGroupById(InternalCommonGroup.GLOBAL_GROUP_ID).threadCount(),
                            h, m, s
                        });
            }

            /* finishing */
            byePhase();
        } finally {
            networker.shutdown();
        }
    }

    private static ScheduledFuture<?> sheduleExitTimer(Thread thread) {
        ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(1);
        timer.setRemoveOnCancelPolicy(true);
        timer.setExecuteExistingDelayedTasksAfterShutdownPolicy(true);

        ScheduledFuture<?> exitTimer = timer.schedule(() -> thread.interrupt(),
                Configuration.INIT_MAXTIME, TimeUnit.SECONDS);

        timer.shutdown();

        return exitTimer;
    }

    private static void waitForPcjThreadsComplete(Set<PcjThread> pcjThreads) {
        while (pcjThreads.isEmpty() == false) {
            try {
                for (Iterator<PcjThread> it = pcjThreads.iterator(); it.hasNext();) {
                    PcjThread pcjThread = it.next();
                    pcjThread.join(100);
                    if (pcjThread.isAlive() == false) {
                        Throwable t = pcjThread.getThrowable();
                        if (t != null) {
                            LOGGER.log(Level.SEVERE, "Exception occurs in thread: " + pcjThread.getName(), t);
                        }
                        it.remove();
                    }
                }
            } catch (InterruptedException ex) {
                throw new RuntimeException("Interruption occurs while waiting for joining PcjThread", ex);
            }
        }
    }

    private static Set<PcjThread> preparePcjThreads(Class<? extends StartPoint> startPointClass, int[] threadIds,
            List<Class<? extends Enum<? extends Shared>>> storages) {
        InternalCommonGroup globalGroup = nodeData.getGroupById(InternalCommonGroup.GLOBAL_GROUP_ID);
        Set<PcjThread> pcjThreads = new HashSet<>();

        for (int threadId : threadIds) {
            InternalGroup threadGlobalGroup = new InternalGroup(threadId, globalGroup);
            PcjThreadData pcjThreadData = new PcjThreadData(threadGlobalGroup);
            PcjThread pcjThread = new PcjThread(threadId, startPointClass, pcjThreadData, storages);

            pcjThreads.add(pcjThread);
        }

        return pcjThreads;
    }

    private static Queue<InetAddress> getHostAllNetworkInferfaces() throws UncheckedIOException {
        try {
            return Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
                    .flatMap(iface -> Collections.list(iface.getInetAddresses()).stream())
                    .collect(Collectors.toCollection(ArrayDeque::new));
        } catch (SocketException ex) {
            LOGGER.log(Level.SEVERE, "Unable to get network interfaces", ex);
            throw new UncheckedIOException(ex);
        }
    }

    private static void bindOnAllNetworkInterfaces(Queue<InetAddress> inetAddresses, int bindingPort) throws UncheckedIOException {
        for (int attempt = 0; attempt <= Configuration.RETRY_COUNT; ++attempt) {
            for (Iterator<InetAddress> it = inetAddresses.iterator(); it.hasNext();) {
                InetAddress inetAddress = it.next();

                try {
                    networker.bind(inetAddress, bindingPort, Configuration.BACKLOG_COUNT);
                    it.remove();
                } catch (IOException ex) {
                    if (attempt <= Configuration.RETRY_COUNT) {
                        LOGGER.log(Level.WARNING,
                                "({0,number,#} attempt of {1,number,#}) Binding on port {2,number,#} failed: {3}. Retrying.",
                                new Object[]{
                                    attempt + 1,
                                    Configuration.RETRY_COUNT + 1,
                                    bindingPort,
                                    ex.getMessage()});
                    } else {
                        throw new UncheckedIOException(String.format("Binding on port %s failed!", bindingPort), ex);
                    }
                }
            }

            if (inetAddresses.isEmpty()) {
                LOGGER.fine("Binding on all interfaces successfuly completed.");
                return;
            } else {
                try {
                    Thread.sleep(Configuration.RETRY_DELAY * 1000 + (int) (Math.random() * 1000));
                } catch (InterruptedException ex) {
                    throw new RuntimeException("Interruption occurs while waiting for binding retry.", ex);
                }
            }
        }
        throw new IllegalStateException("Unreachable code.");
    }

    private static void connectToNode0(boolean isCurrentJvmNode0, NodeInfo node0) throws RuntimeException {
        if (isCurrentJvmNode0 == true) {
            node0Socket = LoopbackSocketChannel.getInstance();

            return;
        } else {
            try {
                LOGGER.fine("Waiting 300-500ms before attempting to connect to node0 to ensure binding completion.");
                Thread.sleep(300 + (int) (Math.random() * 200));
            } catch (InterruptedException ex) {
                throw new RuntimeException("Interruption occurs while waiting before attempting to connect to node0.", ex);
            }
            for (int attempt = 0; attempt <= Configuration.RETRY_COUNT; ++attempt) {
                try {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(Level.FINE, "Connecting to node0 ({0}:{1,number,#})...",
                                new Object[]{node0.getHostname(), node0.getPort()});
                    }

                    InetAddress inetAddressNode0 = InetAddress.getByName(node0.getHostname());
                    node0Socket = networker.connectTo(inetAddressNode0, node0.getPort());

                    if (LOGGER.isLoggable(Level.FINER)) {
                        LOGGER.log(Level.FINER, "Connected to node0 ({0}:{1,number,#}): {2}",
                                new Object[]{node0.getHostname(), node0.getPort(), Objects.toString(node0Socket)});
                    }

                    return;
                } catch (IOException ex) {
                    if (attempt < Configuration.RETRY_COUNT) {
                        LOGGER.log(Level.WARNING,
                                "({0,number,#} attempt of {1,number,#}) Connecting to node0 ({2}:{3,number,#}) failed: {4,number,#}. Retrying.",
                                new Object[]{attempt + 1, Configuration.RETRY_COUNT + 1,
                                    node0.getHostname(), node0.getPort(), ex.getMessage()});
                        try {
                            Thread.sleep(Configuration.RETRY_DELAY * 1000 + (int) (Math.random() * 1000));
                        } catch (InterruptedException e) {
                            throw new RuntimeException("Interruption occurs while waiting for connection retry.", e);
                        }
                    } else {
                        throw new RuntimeException(String.format("Connecting to node0 (%s:%d) failed!",
                                node0.getHostname(), node0.getPort()), ex);
                    }
                } catch (InterruptedException ex) {
                    throw new RuntimeException(
                            String.format("Interruption occurs while connecting to node0 (%s:%d).",
                                    node0.getHostname(), node0.getPort()),
                            ex);
                }
            }
        }
        throw new IllegalStateException("Unreachable code.");

    }

    private static void helloPhase(int port, int[] threadIds) throws UncheckedIOException {
        MessageHello messageHello = new MessageHello(port, threadIds);

        WaitObject sync = nodeData.getGlobalWaitObject();
        sync.lock();
        try {
            networker.send(node0Socket, messageHello);

            /* waiting for HELLO_GO */
            sync.await();
        } catch (InterruptedException ex) {
            throw new RuntimeException("Interruption occurs while waiting for finish HELLO phase", ex);
        } finally {
            sync.unlock();
        }
    }

    private static void byePhase() throws UncheckedIOException {
        /* Sending BYE message to node0 */
        MessageBye messageBye = new MessageBye(nodeData.getPhysicalId());
        WaitObject finishedObject = nodeData.getGlobalWaitObject();
        finishedObject.lock();
        try {
            networker.send(node0Socket, messageBye);

            /* waiting for BYE_COMPLETED */
            finishedObject.await();
        } catch (InterruptedException ex) {
            throw new RuntimeException("Interruption occurs while waiting for MESSAGE_BYE_COMPLETED phase", ex);
        } finally {
            finishedObject.unlock();
        }
    }

    public static Networker getNetworker() {
        return networker;
    }

    public static NodeData getNodeData() {
        return nodeData;
    }

    public static SocketChannel getNode0Socket() {
        return node0Socket;
    }

//    protected static int getPhysicalNodeId() {
//        return nodeData.physicalId;
//    }
//    protected static InternalCommonGroup join(int myNodeId, String groupName) {
//        InternalCommonGroup group = PcjThread.threadGroup(groupName);
//        if (group != null) {
//            return group;
//        }
//
//        synchronized (pcjNodeData.internalGroupsById) {
//            group = pcjNodeData.internalGroupsByName.get(groupName);
//        }
//        MessageGroupJoinQuery msgQuery = new MessageGroupJoinQuery();
//        msgQuery.setGroupName(groupName);
//
//        try {
//            int masterPhysicalId;
//            int groupId;
//            if (group == null) {
//                Object[] objs = networker.sendWait(node0Socket, msgQuery);
//                masterPhysicalId = (int) objs[0];
//                groupId = (int) objs[1];
//            } else {
//                masterPhysicalId = group.getPhysicalMaster();
//                groupId = group.getGroupId();
//            }
//
//            MessageGroupJoinRequest msgJoin = new MessageGroupJoinRequest();
//            msgJoin.setGroupId(groupId);
//            msgJoin.setGlobaNodelId(myNodeId);
//            msgJoin.setGroupName(groupName);
//            group = networker.sendWait(pcjNodeData.physicalNodes.get(masterPhysicalId), msgJoin);
//
//            BitMask mask = group.getJoinBitmask(group.myId());
//            synchronized (mask) {
//                while (mask.isSet() == false) {
//                    try {
//                        mask.wait();
//                    } catch (InterruptedException ex) {
//                        throw new RuntimeException(ex);
//                    }
//                }
//            }
//            return group;
//        } catch (IOException ex) {
//            throw new RuntimeException(ex);
//        }
//    }
}
