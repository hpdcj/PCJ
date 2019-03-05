/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.pcj.PCJ;
import org.pcj.PcjRuntimeException;
import org.pcj.RegisterStorage;
import org.pcj.StartPoint;
import org.pcj.Storage;

/**
 * This class represents PCJ thread.
 * <p>
 * Thread contains reference to its own local data.
 * <p>
 * When thread is run deserializer also runs.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class PcjThread extends Thread {

    private final int threadId;
    private final PcjThreadGroup pcjThreadGroup;
    private final Class<? extends StartPoint> startPointClass;
    private final ExecutorService asyncTasksWorkers;
    private Throwable throwable;

    PcjThread(int threadId, Class<? extends StartPoint> startPointClass, PcjThreadData threadData) {
        super(new PcjThreadGroup("PcjThreadGroup-" + threadId, threadData), "PcjThread-" + threadId);

        this.threadId = threadId;
        this.pcjThreadGroup = (PcjThreadGroup) this.getThreadGroup();
        this.startPointClass = startPointClass;

        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable runnable) {
                return new Thread(pcjThreadGroup, runnable,
                        PcjThread.this.getName() + "-Task-" + counter.getAndIncrement());
            }
        };

        this.asyncTasksWorkers = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                threadFactory
        );

    }

    private static class PcjThreadGroup extends ThreadGroup {

        private final PcjThreadData threadData;

        private PcjThreadGroup(String name, PcjThreadData threadData) {
            super(name);
            this.threadData = threadData;
        }

        public PcjThreadData getThreadData() {
            return threadData;
        }
    }

    @Override
    public void run() {
        try {
            StartPoint startPoint = getStartPointObject();

            /* be sure that each thread' startPoint and storages are initialized */
            PCJ.barrier();

            /* start execution */
            startPoint.main();
        } catch (Throwable t) {
            this.throwable = t;
        }
    }

    private StartPoint getStartPointObject() throws RuntimeException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        StartPoint startPoint = initializeStorages();
        if (startPoint == null) {
            startPoint = initializeStartPointClass();
        }
        return startPoint;
    }

    private StartPoint initializeStorages() {
        StartPoint startPoint = null;

        RegisterStorage[] registerStorages = startPointClass.getAnnotationsByType(RegisterStorage.class);

        for (RegisterStorage registerStorage : registerStorages) {
            Class<? extends Enum<?>> sharedEnumClass = registerStorage.value();
            Storage storageAnnotation = sharedEnumClass.getAnnotation(Storage.class);
            if (storageAnnotation == null) {
                throw new PcjRuntimeException("Enum is not annotated by @Storage annotation: " + sharedEnumClass.getName());
            }

            Object object = PCJ.registerStorage(sharedEnumClass);
            if (storageAnnotation.value().equals(startPointClass)) {
                startPoint = (StartPoint) object;
            }
        }

        return startPoint;
    }

    private StartPoint initializeStartPointClass() throws NoSuchMethodException, InstantiationException, InvocationTargetException, IllegalAccessException, SecurityException, IllegalArgumentException {
        StartPoint startPoint;
        Constructor<? extends StartPoint> startPointClassConstructor = startPointClass.getConstructor();
        startPointClassConstructor.setAccessible(true);
        startPoint = startPointClassConstructor.newInstance();
        return startPoint;
    }

    int getThreadId() {
        return threadId;
    }

    Throwable getThrowable() {
        return throwable;
    }

    void shutdownThreadPool() {
        try {
            asyncTasksWorkers.shutdown();
            asyncTasksWorkers.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            throw new PcjRuntimeException("Interrupted exception while shutting down thread pool", e);
        }
    }

    public PcjThreadData getThreadData() {
        return pcjThreadGroup.getThreadData();
    }

    public void executeOnAsyncTasksWorkers(Runnable runnable) {
        asyncTasksWorkers.execute(runnable);
    }

    private static PcjThreadGroup getPcjThreadGroupForCurrentThread() {
        ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
        if (threadGroup instanceof PcjThreadGroup) {
            return (PcjThreadGroup) threadGroup;
        }
        while (threadGroup.getParent() != null) {
            threadGroup = threadGroup.getParent();
            if (threadGroup instanceof PcjThreadGroup) {
                return (PcjThreadGroup) threadGroup;
            }
        }
        return null;
    }

    public static PcjThreadData getCurrentThreadData() {
        PcjThreadGroup tg = getPcjThreadGroupForCurrentThread();
        if (tg == null) {
            throw new IllegalStateException("Current thread is not part of PcjThread.");
        }
        return tg.getThreadData();
    }
}
