/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pcj.NodesDescription;
import org.pcj.PcjFuture;
import org.pcj.PcjRuntimeException;
import org.pcj.StartPoint;
import org.pcj.internal.message.alive.AliveState;
import org.pcj.internal.message.bye.ByeState;
import org.pcj.internal.message.hello.HelloMessage;
import org.pcj.internal.message.hello.HelloState;
import org.pcj.internal.message.join.GroupJoinRequestMessage;
import org.pcj.internal.message.join.GroupJoinStates;
import org.pcj.internal.message.join.GroupQueryMessage;
import org.pcj.internal.message.join.GroupQueryStates;
import org.pcj.internal.network.LoopbackSocketChannel;

/**
 * Internal class for external PCJ class.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public abstract class InternalPCJ {

    private static final Logger LOGGER = Logger.getLogger(InternalPCJ.class.getName());
    private static final String PCJ_VERSION;
    private static Networker networker;
    private static NodeData nodeData;

    static {
        Package p = InternalPCJ.class.getPackage();
        PCJ_VERSION = p.getImplementationVersion() == null ? "UNKNOWN" : p.getImplementationVersion();
    }

    /**
     * Suppress default constructor for noninstantiability.
     * <p>
     * Have to be protected to allow inheritance - to give rights to its protected methods.
     */
    protected InternalPCJ() {
        throw new AssertionError();
    }

    protected static void start(Class<? extends StartPoint> startPoint,
                                NodesDescription nodesFile) {
        NodeInfo node0 = nodesFile.getNode0();
        NodeInfo currentJvm = nodesFile.getCurrentJvm();
        int allNodesThreadCount = nodesFile.getAllNodesThreadCount();
        start(startPoint, node0, currentJvm, allNodesThreadCount);
    }

    protected static void start(Class<? extends StartPoint> startPointClass,
                                NodeInfo node0, NodeInfo currentJvm, int allNodesThreadCount) {
        if (currentJvm == null) {
            throw new IllegalArgumentException("There is no entry for PCJ threads for current JVM");
        }
        if (node0 == null) {
            throw new IllegalArgumentException("There is no node0 entry in nodes description file.");
        }

        boolean isCurrentJvmNode0 = node0.getPort() == currentJvm.getPort() && node0.isLocalAddress();

        if (isCurrentJvmNode0) {
            LOGGER.log(Level.INFO, "PCJ version {0}", PCJ_VERSION);
        }

        networker = prepareNetworker();
        networker.startup();
        try {
            /* Prepare initial data */
            nodeData = new NodeData();
            nodeData.setHelloState(new HelloState(allNodesThreadCount));
            if (isCurrentJvmNode0) {
                nodeData.setNode0Data(new NodeData.Node0Data());
            }

            /* Getting all interfaces */
            Queue<InetAddress> inetAddresses = getHostAllNetworkInferfaces();

            /* Binding on all interfaces */
            bindOnAllNetworkInterfaces(inetAddresses, currentJvm.getPort());

            /* connecting to node0 */
            SocketChannel node0Socket = connectToNode0(node0, isCurrentJvmNode0);
            nodeData.setNode0Socket(node0Socket);

            /* send HELLO message */
            helloPhase(currentJvm.getPort(), currentJvm.getThreadIds());

            nodeData.setHelloState(null);

            /* Starting execution */
            if (isCurrentJvmNode0) {
                LOGGER.log(Level.INFO, "Starting {0}"
                                               + " with {1,number,#} {1,choice,1#thread|1<threads}"
                                               + " (on {2,number,#} {2,choice,1#node|1<nodes})...",
                        new Object[]{startPointClass.getName(),
                                nodeData.getCommonGroupById(InternalCommonGroup.GLOBAL_GROUP_ID).threadCount(),
                                nodeData.getTotalNodeCount(),});
            }
            long nanoTime = System.nanoTime();

            Map<Integer, PcjThread> pcjThreads = null;
            AliveState aliveState = nodeData.getAliveState();
            try {
                aliveState.start(Thread.currentThread());

                /* Preparing PcjThreads */
                Semaphore notificationObject = new Semaphore(0);

                pcjThreads = preparePcjThreads(startPointClass, currentJvm.getThreadIds(), notificationObject);
                nodeData.updatePcjThreads(pcjThreads);

                /* Starting PcjThreads */
                pcjThreads.values().forEach(PcjThread::start);

                /* Waiting for all threads complete */
                waitForPcjThreadsComplete(pcjThreads.values(), notificationObject);

                if (!Thread.interrupted()) {
                    /* finishing */
                    byePhase();
                }
            } catch (InterruptedException ex) {
                pcjThreads.values().forEach(Thread::interrupt);
            } finally {
                aliveState.stop();

                /* Finishing asyncTaskWorkers */
                if (pcjThreads != null) {
                    pcjThreads.values().stream().map(PcjThread::getAsyncWorkers).forEach(PcjThread.AsyncWorkers::shutdown);
                }
            }

            if (isCurrentJvmNode0) {
                long timerNano = (System.nanoTime() - nanoTime);
                long ms = (timerNano / 1_000_000) % 1000;

                long timer = timerNano / 1_000_000_000L;
                long h = timer / (60 * 60);
                long m = (timer / 60) % 60;
                long s = (timer % 60);

                LOGGER.log(Level.INFO, "{0} {1}"
                                               + " with {2,number,#} {2,choice,1#thread|1<threads}"
                                               + " (on {3,number,#} {3,choice,1#node|1<nodes})"
                                               + " after {4,number,#}h {5,number,#}m {6,number,#}s {7,number,#}ms.",
                        new Object[]{
                                !aliveState.isAborted() ? "Completed" : "Aborted",
                                startPointClass.getName(),
                                nodeData.getCommonGroupById(InternalCommonGroup.GLOBAL_GROUP_ID).threadCount(),
                                nodeData.getTotalNodeCount(),
                                h, m, s, ms
                        });
            }
        } finally {
            networker.shutdown();
        }
    }

    private static Networker prepareNetworker() {
        BlockingQueue<Runnable> blockingQueue;
        if (Configuration.NET_WORKERS_QUEUE_SIZE > 0) {
            blockingQueue = new ArrayBlockingQueue<>(Configuration.NET_WORKERS_QUEUE_SIZE);
        } else if (Configuration.NET_WORKERS_QUEUE_SIZE == 0) {
            blockingQueue = new SynchronousQueue<>();
        } else {
            blockingQueue = new LinkedBlockingQueue<>();
        }

        ThreadGroup threadGroup = new ThreadGroup("NetworkerGroup");

        ExecutorService workers = new WorkerPoolExecutor(
                Configuration.NET_WORKERS_COUNT,
                threadGroup, "Networker-Worker-",
                blockingQueue,
                new ThreadPoolExecutor.CallerRunsPolicy());

        return new Networker(threadGroup, workers);
    }

    private static Queue<InetAddress> getHostAllNetworkInferfaces() throws UncheckedIOException {
        try {
            return Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
                           .flatMap(iface -> Collections.list(iface.getInetAddresses()).stream())
                           .distinct()
                           .collect(Collectors.toCollection(ArrayDeque::new));
        } catch (SocketException ex) {
            LOGGER.log(Level.SEVERE, "Unable to get network interfaces", ex);
            throw new UncheckedIOException(ex);
        }
    }

    private static void bindOnAllNetworkInterfaces(Queue<InetAddress> inetAddresses, int bindingPort) throws UncheckedIOException {
        for (int attempt = 0; attempt <= Configuration.INIT_RETRY_COUNT; ++attempt) {
            for (Iterator<InetAddress> it = inetAddresses.iterator(); it.hasNext(); ) {
                InetAddress inetAddress = it.next();

                try {
                    networker.bind(inetAddress, bindingPort, Configuration.INIT_BACKLOG_COUNT);
                    it.remove();
                } catch (IOException ex) {
                    if (attempt < Configuration.INIT_RETRY_COUNT) {
                        LOGGER.log(Level.WARNING,
                                "({0,number,#} attempt of {1,number,#}) Binding on port {2,number,#} of {3} failed: {4}. Retrying.",
                                new Object[]{
                                        attempt + 1,
                                        Configuration.INIT_RETRY_COUNT + 1,
                                        bindingPort,
                                        inetAddress,
                                        ex.getMessage()});
                    } else {
                        throw new UncheckedIOException(String.format("Binding on port %s failed!", bindingPort), ex);
                    }
                }
            }

            if (inetAddresses.isEmpty()) {
                LOGGER.fine("Binding on all interfaces successfully completed.");
                return;
            } else {
                try {
                    Thread.sleep(Configuration.INIT_RETRY_DELAY * 1000 + (int) (Math.random() * 1000));
                } catch (InterruptedException ex) {
                    throw new PcjRuntimeException("Interruption occurred while waiting for binding retry.");
                }
            }
        }
        throw new IllegalStateException("Unreachable code.");
    }

    private static SocketChannel connectToNode0(NodeInfo node0, boolean isCurrentJvmNode0) throws PcjRuntimeException {
        if (isCurrentJvmNode0) {
            return LoopbackSocketChannel.getInstance();
        } else {
            try {
                LOGGER.fine("Waiting 300-500ms before attempting to connect to node0 to ensure binding completion.");
                Thread.sleep(300 + (int) (Math.random() * 200));
            } catch (InterruptedException ex) {
                throw new PcjRuntimeException("Interruption occurred while waiting before attempting to connect to node0.");
            }
            for (int attempt = 0; attempt <= Configuration.INIT_RETRY_COUNT; ++attempt) {
                try {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(Level.FINE, "Connecting to node0 ({0}:{1,number,#})...",
                                new Object[]{node0.getHostname(), node0.getPort()});
                    }

                    InetAddress inetAddressNode0 = InetAddress.getByName(node0.getHostname());
                    SocketChannel node0Socket = networker.connectTo(inetAddressNode0, node0.getPort());

                    if (LOGGER.isLoggable(Level.FINER)) {
                        LOGGER.log(Level.FINER, "Connected to node0 ({0}:{1,number,#}): {2}",
                                new Object[]{node0.getHostname(), node0.getPort(), Objects.toString(node0Socket)});
                    }

                    return node0Socket;
                } catch (IOException ex) {
                    if (attempt < Configuration.INIT_RETRY_COUNT) {
                        LOGGER.log(Level.WARNING,
                                "({0,number,#} attempt of {1,number,#}) Connecting to node0 ({2}:{3,number,#}) failed: {4}. Retrying.",
                                new Object[]{attempt + 1, Configuration.INIT_RETRY_COUNT + 1,
                                        node0.getHostname(), node0.getPort(), ex.getMessage()});
                        try {
                            Thread.sleep(Configuration.INIT_RETRY_DELAY * 1000 + (int) (Math.random() * 1000));
                        } catch (InterruptedException e) {
                            throw new PcjRuntimeException("Interruption occurred while waiting for connection retry.");
                        }
                    } else {
                        throw new PcjRuntimeException(String.format("Connecting to node0 (%s:%d) failed!",
                                node0.getHostname(), node0.getPort()), ex);
                    }
                } catch (InterruptedException ex) {
                    throw new PcjRuntimeException(
                            String.format("Interruption occurred while connecting to node0 (%s:%d).",
                                    node0.getHostname(), node0.getPort()));
                }
            }
        }
        throw new IllegalStateException("Unreachable code.");

    }

    private static void helloPhase(int port, Set<Integer> threadIds) throws UncheckedIOException {
        try {
            HelloState state = nodeData.getHelloState();

            HelloMessage helloMessage = new HelloMessage(port, threadIds.stream().mapToInt(Integer::intValue).toArray());

            networker.send(nodeData.getNode0Socket(), helloMessage);

            /* waiting for HELLO_GO */
            state.await(Configuration.INIT_MAXTIME);
        } catch (InterruptedException ex) {
            throw new PcjRuntimeException("Interruption occurred while waiting for finish HELLO phase");
        } catch (TimeoutException e) {
            throw new PcjRuntimeException("Timeout occurred while waiting for finish HELLO phase");
        }
    }

    private static Map<Integer, PcjThread> preparePcjThreads(Class<? extends StartPoint> startPointClass, Set<Integer> threadIds, Semaphore notificationSemaphore) {
        InternalCommonGroup globalGroup = nodeData.getCommonGroupById(InternalCommonGroup.GLOBAL_GROUP_ID);
        Map<Integer, PcjThread> pcjThreads = new HashMap<>();

        for (int threadId : threadIds) {
            InternalGroup threadGlobalGroup = new InternalGroup(threadId, globalGroup);
            PcjThreadData pcjThreadData = new PcjThreadData(threadGlobalGroup);
            PcjThread.PcjThreadGroup pcjThreadGroup = PcjThread.createPcjThreadGroup(threadId, pcjThreadData);

            BlockingQueue<Runnable> blockingQueue;
            if (Configuration.ASYNC_WORKERS_QUEUE_SIZE > 0) {
                blockingQueue = new ArrayBlockingQueue<>(Configuration.ASYNC_WORKERS_QUEUE_SIZE);
            } else if (Configuration.ASYNC_WORKERS_QUEUE_SIZE == 0) {
                blockingQueue = new SynchronousQueue<>();
            } else {
                blockingQueue = new LinkedBlockingQueue<>();
            }

            ExecutorService asyncTasksWorkers = new WorkerPoolExecutor(
                    Configuration.ASYNC_WORKERS_COUNT,
                    pcjThreadGroup, "PcjThread-" + threadId + "-Task-",
                    blockingQueue,
                    new ThreadPoolExecutor.AbortPolicy());


            PcjThread pcjThread = new PcjThread(startPointClass, threadId, pcjThreadGroup, asyncTasksWorkers, notificationSemaphore);

            pcjThreads.put(threadId, pcjThread);
        }

        return pcjThreads;
    }

    private static void waitForPcjThreadsComplete(Collection<PcjThread> pcjThreadsSet, Semaphore notificationSemaphore) throws InterruptedException {
        Set<PcjThread> pcjThreads = new HashSet<>(pcjThreadsSet);

        int notProcessedFinishedThreads = 0;
        while (!pcjThreads.isEmpty()) {
            if (notProcessedFinishedThreads <= 0) {
                notificationSemaphore.acquire();
                notProcessedFinishedThreads += notificationSemaphore.drainPermits() + 1;
            }
            for (Iterator<PcjThread> it = pcjThreads.iterator(); it.hasNext(); ) {
                PcjThread pcjThread = it.next();
                if (!pcjThread.isAlive()) {
                    Throwable t = pcjThread.getThrowable();
                    if (t != null) {
                        LOGGER.log(Level.SEVERE, "Exception occurred in thread: " + pcjThread.getName(), t);

                        AliveState aliveState = nodeData.getAliveState();
                        aliveState.nodeAborted();
                        return;
                    }
                    it.remove();
                    --notProcessedFinishedThreads;
                }
            }
        }
    }

    private static void byePhase() {
        try {
            ByeState byeState = nodeData.getByeState();
            byeState.nodeProcessed();
            /* waiting for BYE_COMPLETED */
            byeState.await();
        } catch (InterruptedException ex) {
            throw new PcjRuntimeException("Interruption occurred while waiting for MESSAGE_BYE_COMPLETED phase");
        }
    }

    public static Networker getNetworker() {
        return networker;
    }

    public static NodeData getNodeData() {
        return nodeData;
    }

    public static SocketChannel getLoopbackSocketChannel() {
        return LoopbackSocketChannel.getInstance();
    }

    protected static InternalGroup joinGroup(int globalThreadId, String groupName) {
        PcjThreadData currentThreadData = PcjThread.getCurrentThreadData();
        InternalGroup group = currentThreadData.getInternalGroupByName(groupName);
        if (group != null) {
            return group;
        }

        InternalCommonGroup commonGroup = nodeData.getInternalCommonGroupByName(groupName);
        if (commonGroup == null) {
            GroupQueryStates groupQueryStates = nodeData.getGroupQueryStates();
            GroupQueryStates.State state = groupQueryStates.create();

            GroupQueryMessage message = new GroupQueryMessage(state.getRequestNum(), nodeData.getCurrentNodePhysicalId(), groupName);

            networker.send(nodeData.getNode0Socket(), message);

            PcjFuture<InternalCommonGroup> future = state.getFuture();
            commonGroup = future.get();
        }

        GroupJoinStates groupJoinStates = nodeData.getGroupJoinStates();
        GroupJoinStates.Notification notification = groupJoinStates.createNotification(globalThreadId);

        GroupJoinRequestMessage message = new GroupJoinRequestMessage(
                notification.getRequestNum(), groupName, commonGroup.getGroupId(), nodeData.getCurrentNodePhysicalId(), globalThreadId);

        SocketChannel groupMasterSocketChannel = nodeData.getSocketChannelByPhysicalId(commonGroup.getCommunicationTree().getMasterNode());

        networker.send(groupMasterSocketChannel, message);

        PcjFuture<InternalGroup> future = notification.getFuture();
        return future.get();
    }
}
