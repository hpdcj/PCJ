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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
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
import org.pcj.PcjFuture;
import org.pcj.StartPoint;
import org.pcj.internal.futures.WaitObject;
import org.pcj.internal.message.MessageBye;
import org.pcj.internal.message.hello.HelloState;
import org.pcj.internal.message.hello.MessageHello;
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

        boolean isCurrentJvmNode0 = node0.isLocalAddress() && node0.getPort() == currentJvm.getPort();

        if (isCurrentJvmNode0) {
            LOGGER.log(Level.INFO, "PCJ version {0}", PCJ_VERSION);
        }

        networker = prepareNetworker(currentJvm);
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

            ScheduledFuture<?> helloTimeoutTimer = scheduleInterruptTimer(Thread.currentThread(), Configuration.INIT_MAXTIME, TimeUnit.SECONDS);

            /* connecting to node0 */
            SocketChannel node0Socket = connectToNode0(node0, isCurrentJvmNode0);
            nodeData.setNode0Socket(node0Socket);

            /* send HELLO message */
            helloPhase(currentJvm.getPort(), currentJvm.getThreadIds());

            helloTimeoutTimer.cancel(true);

            nodeData.setHelloState(null);

            long nanoTime = Long.MIN_VALUE;
            /* Starting execution */
            if (isCurrentJvmNode0) {
                nanoTime = System.nanoTime();
                LOGGER.log(Level.INFO, "Starting {0}"
                                               + " with {1,number,#} {1,choice,1#thread|1<threads}"
                                               + " (on {2,number,#} {2,choice,1#node|1<nodes})...",
                        new Object[]{startPointClass.getName(),
                                nodeData.getCommonGroupById(InternalCommonGroup.GLOBAL_GROUP_ID).threadCount(),
                                nodeData.getTotalNodeCount(),});
            }


            /* Preparing PcjThreads*/
            Set<PcjThread> pcjThreads = preparePcjThreads(startPointClass, currentJvm.getThreadIds());
            pcjThreads.forEach(pcjThread -> nodeData.putPcjThread(pcjThread));

            /* Starting PcjThreads*/
            pcjThreads.forEach(pcjThread -> pcjThread.start());

            /* Waiting for all threads complete */
            waitForPcjThreadsComplete(pcjThreads);

            if (isCurrentJvmNode0) {
                long timer = (System.nanoTime() - nanoTime) / 1_000_000_000L;
                long h = timer / (60 * 60);
                long m = (timer / 60) % 60;
                long s = (timer % 60);

                LOGGER.log(Level.INFO, "Completed {0}"
                                               + " with {1,number,#} {1,choice,1#thread|1<threads}"
                                               + " (on {2,number,#} {2,choice,1#node|1<nodes})"
                                               + " after {3,number,#}h {4,number,#}m {5,number,#}s.",
                        new Object[]{
                                startPointClass.getName(),
                                nodeData.getCommonGroupById(InternalCommonGroup.GLOBAL_GROUP_ID).threadCount(),
                                nodeData.getTotalNodeCount(),
                                h, m, s
                        });
            }

            /* finishing */
            byePhase();
        } finally {
            networker.shutdown();
        }
    }

    private static Networker prepareNetworker(NodeInfo currentJvm) {
        int minWorkerCount = currentJvm.getThreadIds().size() + 1;
        int maxWorkerCount = currentJvm.getThreadIds().size() + 1;
        if (Configuration.WORKERS_MIN_COUNT >= 0) {
            minWorkerCount = Configuration.WORKERS_MIN_COUNT;
        }
        if (Configuration.WORKERS_MAX_COUNT > 0) {
            maxWorkerCount = Configuration.WORKERS_MAX_COUNT;
        }
        return new Networker(minWorkerCount, Math.max(minWorkerCount, maxWorkerCount));
    }

    private static ScheduledFuture<?> scheduleInterruptTimer(Thread thread, long delay, TimeUnit unit) {
        ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(1);
        timer.setRemoveOnCancelPolicy(true);
        timer.setExecuteExistingDelayedTasksAfterShutdownPolicy(true);

        ScheduledFuture<?> exitTimer = timer.schedule(() -> thread.interrupt(), delay, unit);

        timer.shutdown();

        return exitTimer;
    }

    private static void waitForPcjThreadsComplete(Set<PcjThread> pcjThreads) {
        while (pcjThreads.isEmpty() == false) {
            try {
                for (Iterator<PcjThread> it = pcjThreads.iterator(); it.hasNext(); ) {
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

    private static Set<PcjThread> preparePcjThreads(Class<? extends StartPoint> startPointClass, Set<Integer> threadIds) {
        InternalCommonGroup globalGroup = nodeData.getCommonGroupById(InternalCommonGroup.GLOBAL_GROUP_ID);
        Set<PcjThread> pcjThreads = new HashSet<>();

        for (int threadId : threadIds) {
            InternalGroup threadGlobalGroup = new InternalGroup(threadId, globalGroup);
            PcjThreadData pcjThreadData = new PcjThreadData(threadGlobalGroup);
            PcjThread pcjThread = new PcjThread(threadId, startPointClass, pcjThreadData);

            pcjThreads.add(pcjThread);
        }

        return pcjThreads;
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
                                "({0,number,#} attempt of {1,number,#}) Binding on port {2,number,#} failed: {3}. Retrying.",
                                new Object[]{
                                        attempt + 1,
                                        Configuration.INIT_RETRY_COUNT + 1,
                                        bindingPort,
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
                    throw new RuntimeException("Interruption occurs while waiting for binding retry.", ex);
                }
            }
        }
        throw new IllegalStateException("Unreachable code.");
    }

    private static SocketChannel connectToNode0(NodeInfo node0, boolean isCurrentJvmNode0) throws RuntimeException {
        if (isCurrentJvmNode0 == true) {
            return LoopbackSocketChannel.getInstance();
        } else {
            try {
                LOGGER.fine("Waiting 300-500ms before attempting to connect to node0 to ensure binding completion.");
                Thread.sleep(300 + (int) (Math.random() * 200));
            } catch (InterruptedException ex) {
                throw new RuntimeException("Interruption occurs while waiting before attempting to connect to node0.", ex);
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

    private static void helloPhase(int port, Set<Integer> threadIds) throws UncheckedIOException {
        try {
            HelloState state = nodeData.getHelloState();

            MessageHello messageHello = new MessageHello(port, threadIds.stream().mapToInt(Integer::intValue).toArray());

            networker.send(nodeData.getNode0Socket(), messageHello);

            /* waiting for HELLO_GO */
            state.await();
        } catch (InterruptedException ex) {
            throw new RuntimeException("Interruption occurs while waiting for finish HELLO phase", ex);
        }
    }

    private static void byePhase() throws UncheckedIOException {
        /* Sending BYE message to node0 */
        MessageBye messageBye = new MessageBye(nodeData.getCurrentNodePhysicalId());
        WaitObject finishedObject = nodeData.getGlobalWaitObject();
        finishedObject.lock();
        try {
            networker.send(nodeData.getNode0Socket(), messageBye);

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

        SocketChannel groupMasterSocketChannel = nodeData.getSocketChannelByPhysicalId().get(commonGroup.getCommunicationTree().getMasterNode());

        networker.send(groupMasterSocketChannel, message);

        PcjFuture<InternalGroup> future = notification.getFuture();
        return future.get();
    }
}
