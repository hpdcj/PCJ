/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal;

import org.pcj.PcjRuntimeException;
import org.pcj.StartPoint;
import org.pcj.internal.message.alive.AliveState;
import org.pcj.internal.message.bye.ByeState;
import org.pcj.internal.message.hello.HelloMessage;
import org.pcj.internal.message.hello.HelloState;
import org.pcj.internal.network.LoopbackSocketChannel;
import org.pcj.internal.network.MessageProc;
import java.io.UncheckedIOException;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Internal class for external PCJ class.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public abstract class InternalPCJ {

    private static final Logger LOGGER = Logger.getLogger(InternalPCJ.class.getName());
    private static final String PCJ_VERSION;

    private static Configuration configuration;
    private static Networker networker;
    private static MessageProc messageProc;
    private static NodeData nodeData;

    static {
        Package p = InternalPCJ.class.getPackage();
        PCJ_VERSION = p.getImplementationVersion() == null ? "UNKNOWN" : p.getImplementationVersion();
    }

    /**
     * Suppress default constructor for noninstantiability.
     */
    private InternalPCJ() {
        throw new AssertionError();
    }

    public static Configuration getConfiguration() {
        return configuration;
    }

    static void setConfiguration(Configuration configuration) {
        InternalPCJ.configuration = configuration;
    }

    public static Networker getNetworker() {
        return networker;
    }

    public static MessageProc getMessageProc() {
        return messageProc;
    }

    public static NodeData getNodeData() {
        return nodeData;
    }

    public static SocketChannel getLoopbackSocketChannel() {
        return LoopbackSocketChannel.getInstance();
    }

    static void start(Class<? extends StartPoint> startPointClass,
                      NodeInfo node0,
                      NodeInfo currentJvm,
                      int allNodesThreadCount) {
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

        /* Prepare initial data */
        nodeData = new NodeData();
        nodeData.setHelloState(new HelloState(allNodesThreadCount));
        if (isCurrentJvmNode0) {
            nodeData.setNode0Data(new NodeData.Node0Data());
        }

        messageProc = new MessageProc();
        networker = new Networker(currentJvm.getPort());
        try {
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
                } else {
                    throw new InterruptedException("Interrupted.");
                }
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, "PCJ main thread interrupted. Interrupting all PCJ threads");
                pcjThreads.values().stream().map(PcjThread::getPcjThreadGroup).forEach(ThreadGroup::interrupt);

                try {
                    LOGGER.log(Level.INFO, "Waiting up to 8 seconds before forcibly stop PCJ threads.");
                    joinPcjThreadGroup(pcjThreads.values(), 8_000);
                } catch (InterruptedException e) {
                    LOGGER.log(Level.SEVERE, "Exception while waiting for joining PCJ Threads", e);
                }
                if (pcjThreads.values().stream().map(PcjThread::getPcjThreadGroup)
                        .mapToInt(PcjThread.PcjThreadGroup::activeCount).count() > 0) {
                    LOGGER.log(Level.INFO, "Forcibly stopping PCJ threads.");
                    pcjThreads.values().stream().map(PcjThread::getPcjThreadGroup).forEach(ThreadGroup::stop);
                }
            } finally {
                aliveState.stop();

                /* Finishing asyncTaskWorkers */
                if (pcjThreads != null) {
                    pcjThreads.values()
                            .stream()
                            .map(PcjThread::getAsyncWorkers)
                            .forEach(PcjThread.AsyncWorkers::shutdown);
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
            messageProc.shutdown();
            networker.shutdown();
        }
    }

    private static void joinPcjThreadGroup(Collection<PcjThread> pcjThreadsSet, long millis) throws InterruptedException {
        Set<PcjThread> pcjThreads = new HashSet<>(pcjThreadsSet);

        final long startTime = System.nanoTime();
        long delay = millis;

        for (PcjThread pcjThread : pcjThreads) {
            PcjThread.PcjThreadGroup pcjThreadGroup = pcjThread.getPcjThreadGroup();
            while (delay > 0) {
                pcjThreadGroup.join(delay);
                delay = millis - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            }
        }
    }

    private static SocketChannel connectToNode0(NodeInfo node0, boolean isCurrentJvmNode0) throws PcjRuntimeException {
        if (isCurrentJvmNode0) {
            return LoopbackSocketChannel.getInstance();
        }

        try {
            LOGGER.fine("Waiting 300-500ms before attempting to connect to node0 to ensure binding completion.");
            Thread.sleep(300 + (int) (Math.random() * 200));
        } catch (InterruptedException ex) {
            throw new PcjRuntimeException("Interruption occurred while waiting before attempting to connect to node0.");
        }

        return networker.tryToConnectTo(node0.getHostname(), node0.getPort());
    }

    private static void helloPhase(int port, Set<Integer> threadIds) throws UncheckedIOException {
        try {
            HelloState state = nodeData.getHelloState();

            HelloMessage helloMessage = new HelloMessage(port, threadIds.stream().mapToInt(Integer::intValue).toArray());

            networker.send(nodeData.getNode0Socket(), helloMessage);

            /* waiting for HELLO_GO */
            state.await(InternalPCJ.getConfiguration().INIT_MAXTIME);
        } catch (InterruptedException ex) {
            throw new PcjRuntimeException("Interruption occurred while waiting for finish HELLO phase");
        } catch (TimeoutException e) {
            throw new PcjRuntimeException("Timeout occurred while waiting for finish HELLO phase");
        }
    }

    private static Map<Integer, PcjThread> preparePcjThreads(Class<? extends StartPoint> startPointClass,
                                                             Set<Integer> threadIds,
                                                             Semaphore notificationSemaphore) {
        InternalCommonGroup globalGroup = nodeData.getCommonGroupById(InternalCommonGroup.GLOBAL_GROUP_ID);
        Map<Integer, PcjThread> pcjThreads = new HashMap<>();

        for (int threadId : threadIds) {
            InternalGroup threadGlobalGroup = new InternalGroup(threadId, globalGroup);
            PcjThreadData pcjThreadData = new PcjThreadData(threadGlobalGroup);
            PcjThread.PcjThreadGroup pcjThreadGroup = PcjThread.createPcjThreadGroup(threadId, pcjThreadData);

            BlockingQueue<Runnable> blockingQueue;
            if (InternalPCJ.getConfiguration().ASYNC_WORKERS_QUEUE_SIZE > 0) {
                blockingQueue = new ArrayBlockingQueue<>(InternalPCJ.getConfiguration().ASYNC_WORKERS_QUEUE_SIZE);
            } else if (InternalPCJ.getConfiguration().ASYNC_WORKERS_QUEUE_SIZE == 0) {
                blockingQueue = new SynchronousQueue<>();
            } else {
                blockingQueue = new LinkedBlockingQueue<>();
            }

            ExecutorService asyncTasksWorkers = new WorkerPoolExecutor(
                    InternalPCJ.getConfiguration().ASYNC_WORKERS_COUNT,
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
        while (!pcjThreads.isEmpty() && !Thread.currentThread().isInterrupted()) {
            if (notProcessedFinishedThreads <= 0) {
                notificationSemaphore.acquire();
                notProcessedFinishedThreads += notificationSemaphore.drainPermits() + 1;
            }
            for (Iterator<PcjThread> it = pcjThreads.iterator(); it.hasNext(); ) {
                PcjThread pcjThread = it.next();
                if (!pcjThread.isAlive()) {
                    Throwable t = pcjThread.getThrowable();
                    if (t != null) {
                        LOGGER.log(Level.SEVERE, "Exception occurred in thread: " + pcjThread.getName()
                                        + " (node: " + nodeData.getCurrentNodePhysicalId() + ")",
                                t);

                        AliveState aliveState = nodeData.getAliveState();
                        aliveState.nodeAborted();
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
}
