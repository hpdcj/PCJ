/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class WorkerPoolExecutor extends ThreadPoolExecutor {

    public WorkerPoolExecutor(int poolSize,
                              ThreadGroup threadGroup, String threadNamePrefix,
                              BlockingQueue<Runnable> workQueue,
                              RejectedExecutionHandler rejectedExecutionHandler) {
        super(poolSize, poolSize, Long.MAX_VALUE, TimeUnit.NANOSECONDS,
                workQueue,
                new WorkerThreadFactory(threadGroup, threadNamePrefix),
                rejectedExecutionHandler);
        prestartAllCoreThreads();
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
}
