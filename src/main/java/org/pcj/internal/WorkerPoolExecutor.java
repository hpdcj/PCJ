/*
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is modified version of {@link ThreadPoolExecutor}.
 * <p>
 * This class adds another strategy of queuing. It holds corePoolSize threads and, if necessary, make new threads
 * to maximumPoolSize limit. If limit is reached, it adds tasks to the work queue.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class WorkerPoolExecutor implements ExecutorService {
    private final ThreadPoolExecutor workers;

    public WorkerPoolExecutor(int corePoolSize, int maximumPoolSize, int keepAliveTime,
                              ThreadGroup threadGroup, String threadNamePrefix,
                              BlockingQueue<Runnable> workQueue,
                              RejectedExecutionHandler rejectedExecutionHandler) {
        WorkerBlockingQueue workerBlockingQueue = new WorkerBlockingQueue(workQueue, rejectedExecutionHandler);
        workers = new ThreadPoolExecutor(
                corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS,
                workerBlockingQueue,
                new WorkerThreadFactory(threadGroup, threadNamePrefix),
                workerBlockingQueue);
    }

    @Override
    public void shutdown() {
        workers.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return workers.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return workers.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return workers.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return workers.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return workers.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return workers.submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return workers.submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return workers.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return workers.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return workers.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return workers.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        workers.execute(command);
    }

    private static class WorkerThreadFactory implements ThreadFactory {
        private final ThreadGroup threadGroup;
        private final String threadNamePrefix;
        private final AtomicInteger counter = new AtomicInteger(0);

        private WorkerThreadFactory(ThreadGroup threadGroup, String threadNamePrefix) {
            this.threadGroup = threadGroup;
            this.threadNamePrefix = threadNamePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(threadGroup, r, threadNamePrefix + counter.getAndIncrement());
        }
    }

    /**
     * Use this class as {@link BlockingQueue} and {@link RejectedExecutionHandler} handler of {@link ThreadPoolExecutor}
     * to use threads as much as possible, and if the maximum thread count is reached, the tasks are put into queue.
     */
    private class WorkerBlockingQueue extends AbstractQueue<Runnable> implements BlockingQueue<Runnable>, RejectedExecutionHandler {

        private final BlockingQueue<Runnable> blockingQueue;
        private final RejectedExecutionHandler rejectedHandler;

        public WorkerBlockingQueue(BlockingQueue<Runnable> blockingQueue, RejectedExecutionHandler rejectedHandler) {
            this.blockingQueue = blockingQueue;
            this.rejectedHandler = rejectedHandler;
        }

        @Override
        public Iterator<Runnable> iterator() {
            return blockingQueue.iterator();
        }

        @Override
        public int size() {
            return blockingQueue.size();
        }

        @Override
        public boolean offer(Runnable runnable) {
            if (workers.getActiveCount() < workers.getCorePoolSize()) {
                return blockingQueue.offer(runnable);
            }
            return false;
        }

        @Override
        public void put(Runnable runnable) throws InterruptedException {
            blockingQueue.put(runnable);
        }

        @Override
        public boolean offer(Runnable runnable, long timeout, TimeUnit unit) throws InterruptedException {
            if (workers.getActiveCount() < workers.getCorePoolSize()) {
                return blockingQueue.offer(runnable, timeout, unit);
            }
            return false;
        }

        @Override
        public Runnable take() throws InterruptedException {
            return blockingQueue.take();
        }

        @Override
        public Runnable poll(long timeout, TimeUnit unit) throws InterruptedException {
            return blockingQueue.poll(timeout, unit);
        }

        @Override
        public int remainingCapacity() {
            return blockingQueue.remainingCapacity();
        }

        @Override
        public int drainTo(Collection<? super Runnable> c) {
            return blockingQueue.drainTo(c);
        }

        @Override
        public int drainTo(Collection<? super Runnable> c, int maxElements) {
            return blockingQueue.drainTo(c, maxElements);
        }

        @Override
        public Runnable poll() {
            return blockingQueue.poll();
        }

        @Override
        public Runnable peek() {
            return blockingQueue.peek();
        }

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            if (!e.isShutdown()) {
                try {
                    blockingQueue.add(r);
                } catch (Exception ex) {
                    rejectedHandler.rejectedExecution(r, e);
                }
            }
        }
    }
}
