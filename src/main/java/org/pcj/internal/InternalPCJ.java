/* 
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal;

import org.pcj.NodesDescription;
import org.pcj.StartPoint;
import org.pcj.internal.futures.GroupJoinQuery;
import org.pcj.internal.futures.WaitObject;
import org.pcj.internal.message.MessageBye;
import org.pcj.internal.message.MessageGroupJoinQuery;
import org.pcj.internal.message.MessageGroupJoinRequest;
import org.pcj.internal.message.MessageHello;
import org.pcj.internal.network.LoopbackSocketChannel;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Internal class for external PCJ class.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public abstract class InternalPCJ {

    private static final Logger LOGGER = Logger.getLogger(InternalPCJ.class.getName());
    private static final String PCJ_VERSION;
    private static final String PCJ_BUILD_DATE;
    private static Networker networker;
    private static NodeData nodeData;

    static {
        Package p = InternalPCJ.class.getPackage();
        PCJ_VERSION = p.getImplementationVersion() == null ? "UNKNOWN" : p.getImplementationVersion();
        PCJ_BUILD_DATE = p.getImplementationTitle() == null ? "UNKNOWN" : p.getImplementationTitle();
    }

    /* Suppress default constructor for noninstantiability.
     * Have to be protected to allow inheritance */
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

        boolean isCurrentJvmNode0 = node0.isLocalAddress()
                && node0.getPort() == currentJvm.getPort();

        if (isCurrentJvmNode0) {
            LOGGER.log(Level.INFO, "PCJ version {0} built on {1}.",
                    new Object[]{PCJ_VERSION, PCJ_BUILD_DATE});
        }

        networker = new Networker(currentJvm.getThreadIds().length + 1);
        networker.startup();
        try {
            /* Getting all interfaces */
            Queue<InetAddress> inetAddresses = getHostAllNetworkInferfaces();

            /* Binding on all interfaces */
            bindOnAllNetworkInterfaces(inetAddresses, currentJvm.getPort());

            ScheduledFuture<?> exitTimer = scheduleExitTimer(Thread.currentThread());

            /* connecting to node0 */
            SocketChannel node0Socket = connectToNode0(isCurrentJvmNode0, node0);

            /* Prepare initial data */
            nodeData = new NodeData(node0Socket, isCurrentJvmNode0);
            nodeData.getSocketChannelByPhysicalId().put(0, node0Socket);

            if (isCurrentJvmNode0) {
                nodeData.getNode0Data().setAllNodesThreadCount(allNodesThreadCount);
            }

            /* send HELLO message */
            helloPhase(currentJvm.getPort(), currentJvm.getThreadIds());

            exitTimer.cancel(true);

            long nanoTime = Long.MIN_VALUE;
            /* Starting execution */
            if (isCurrentJvmNode0) {
                nanoTime = System.nanoTime();
                LOGGER.log(Level.INFO, "Starting {0} with {1,number,#}"
                                + " {1,choice,1#thread|1<threads}"
                                + " (on {2,number,#} {2,choice,1#node|1<nodes})...",
                        new Object[]{startPointClass.getName(),
                                nodeData.getGroupById(InternalCommonGroup.GLOBAL_GROUP_ID).threadCount(),
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
                                nodeData.getGroupById(InternalCommonGroup.GLOBAL_GROUP_ID).threadCount(),
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

    private static ScheduledFuture<?> scheduleExitTimer(Thread thread) {
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

    private static Set<PcjThread> preparePcjThreads(Class<? extends StartPoint> startPointClass, int[] threadIds) {
        InternalCommonGroup globalGroup = nodeData.getGroupById(InternalCommonGroup.GLOBAL_GROUP_ID);
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
                    .collect(Collectors.toCollection(ArrayDeque::new));
        } catch (SocketException ex) {
            LOGGER.log(Level.SEVERE, "Unable to get network interfaces", ex);
            throw new UncheckedIOException(ex);
        }
    }

    private static void bindOnAllNetworkInterfaces(Queue<InetAddress> inetAddresses, int bindingPort) throws UncheckedIOException {
        for (int attempt = 0; attempt <= Configuration.RETRY_COUNT; ++attempt) {
            for (Iterator<InetAddress> it = inetAddresses.iterator(); it.hasNext(); ) {
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

    private static SocketChannel connectToNode0(boolean isCurrentJvmNode0, NodeInfo node0) throws RuntimeException {
        if (isCurrentJvmNode0 == true) {
            return LoopbackSocketChannel.getInstance();
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
                    SocketChannel node0Socket = networker.connectTo(inetAddressNode0, node0.getPort());

                    if (LOGGER.isLoggable(Level.FINER)) {
                        LOGGER.log(Level.FINER, "Connected to node0 ({0}:{1,number,#}): {2}",
                                new Object[]{node0.getHostname(), node0.getPort(), Objects.toString(node0Socket)});
                    }

                    return node0Socket;
                } catch (IOException ex) {
                    if (attempt < Configuration.RETRY_COUNT) {
                        LOGGER.log(Level.WARNING,
                                "({0,number,#} attempt of {1,number,#}) Connecting to node0 ({2}:{3,number,#}) failed: {4}. Retrying.",
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
            networker.send(nodeData.getNode0Socket(), messageHello);

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

    protected static InternalGroup join(int globalThreadId, String groupName) {
        PcjThreadData currentThreadData = PcjThread.getCurrentThreadData();
        InternalGroup group = currentThreadData.getGroupByName(groupName);
        if (group != null) {
            return group;
        }
        int requestNum = nodeData.getGroupJoinCounter().incrementAndGet();

        InternalCommonGroup commonGroup = nodeData.getGroupByName(groupName);

        GroupJoinQuery groupJoinQuery = nodeData.getGroupJoinQuery(requestNum);
        WaitObject waitObject = groupJoinQuery.getWaitObject();

        if (commonGroup == null) {
            MessageGroupJoinQuery message
                    = new MessageGroupJoinQuery(requestNum, nodeData.getPhysicalId(), groupName);

            waitObject.lock();
            try {
                networker.send(nodeData.getNode0Socket(), message);

                waitObject.await();

                commonGroup = nodeData.createGroup(
                        groupJoinQuery.getGroupMasterId(), groupJoinQuery.getGroupId(), groupName);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            } finally {
                waitObject.unlock();
            }
        }

        MessageGroupJoinRequest message
                = new MessageGroupJoinRequest(requestNum, groupName, commonGroup.getGroupId(),
                nodeData.getPhysicalId(), globalThreadId);

        SocketChannel masterSocketChannel = nodeData.getSocketChannelByPhysicalId().get(commonGroup.getGroupMasterNode());

        waitObject.lock();
        try {
            networker.send(masterSocketChannel, message);

            waitObject.await();

            InternalGroup threadGroup = new InternalGroup(groupJoinQuery.getGroupThreadId(), commonGroup);

            currentThreadData.addGroup(threadGroup);

            return threadGroup;
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        } finally {
            waitObject.unlock();
        }
    }
}
