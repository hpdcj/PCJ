/*
 * Copyright (c) 2011-2021, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
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

    private final PcjThreadGroup pcjThreadGroup;
    private final Class<? extends StartPoint> startPointClass;
    private final AsyncWorkers asyncWorkers;
    private final Semaphore notificationSemaphore;
    private Throwable throwable;

    PcjThread(Class<? extends StartPoint> startPointClass,
              int threadId,
              PcjThreadGroup pcjThreadGroup,
              ExecutorService asyncTasksWorkers,
              Semaphore notificationSemaphore) {
        super(pcjThreadGroup, "PcjThread-" + threadId);

        this.startPointClass = startPointClass;
        this.pcjThreadGroup = pcjThreadGroup;
        this.asyncWorkers = new AsyncWorkers(asyncTasksWorkers);
        this.notificationSemaphore = notificationSemaphore;
    }

    static class PcjThreadGroup extends ThreadGroup {

        private final PcjThreadData threadData;

        private PcjThreadGroup(String name, PcjThreadData threadData) {
            super(name);
            this.threadData = threadData;
        }

        public PcjThreadData getThreadData() {
            return threadData;
        }

        public void join(long millis) throws InterruptedException {
            synchronized (this) {
                int activeCount = this.activeCount();
                Thread[] activeThreads = new Thread[activeCount];
                this.enumerate(activeThreads);

                final long startTime = System.nanoTime();
                long delay = millis;

                for (Thread pcjThread : activeThreads) {
                    while (pcjThread.isAlive() && delay > 0) {
                        pcjThread.join(delay);
                        delay = millis - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                    }
                }
            }
        }
    }

    public static class AsyncWorkers {
        private final ExecutorService asyncTasksWorkers;

        private AsyncWorkers(ExecutorService asyncTasksWorkers) {
            this.asyncTasksWorkers = asyncTasksWorkers;
        }

        public void execute(Runnable runnable) {
            asyncTasksWorkers.execute(runnable);
        }

        void shutdown() {
            try {
                asyncTasksWorkers.shutdown();
                if (!asyncTasksWorkers.awaitTermination(8L, TimeUnit.SECONDS)) {
                    asyncTasksWorkers.shutdownNow();
                }
            } catch (InterruptedException ex) {
                throw new PcjRuntimeException("Interrupted exception while shutting down thread pool");
            }
        }
    }

    static PcjThreadGroup createPcjThreadGroup(int threadId, PcjThreadData pcjThreadData) {
        return new PcjThreadGroup("PcjThreadGroup-" + threadId, pcjThreadData);
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
        } finally {
            notificationSemaphore.release();
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
            Class<? extends Enum<?>>[] storages = registerStorage.value();
            if (storages.length == 0) {
                @SuppressWarnings("unchecked")
                Class<? extends Enum<?>>[] tmpStorages = Arrays.stream(startPointClass.getDeclaredClasses())
                                                                 .filter(Class::isEnum)
                                                                 .filter(clazz -> clazz.isAnnotationPresent(Storage.class))
                                                                 .toArray(Class[]::new);
                storages = tmpStorages;
            }

            for (Class<? extends Enum<?>> sharedEnumClass : storages) {
                Storage storageAnnotation = sharedEnumClass.getAnnotation(Storage.class);
                if (storageAnnotation == null) {
                    throw new PcjRuntimeException("Enum is not annotated by @Storage annotation: " + sharedEnumClass.getName());
                }

                Class<?> storageClass;
                if (storageAnnotation.value() != Storage.Default.class) {
                    storageClass = storageAnnotation.value();
                } else {
                    storageClass = sharedEnumClass.getEnclosingClass();
                }

                Object object = PCJ.registerStorage(sharedEnumClass);
                if (storageClass.equals(startPointClass)) {
                    startPoint = (StartPoint) object;
                }
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

    Throwable getThrowable() {
        return throwable;
    }

    public PcjThreadData getThreadData() {
        return pcjThreadGroup.getThreadData();
    }

    public PcjThreadGroup getPcjThreadGroup() {
        return pcjThreadGroup;
    }

    public void shutdown() {
        this.interrupt();
        pcjThreadGroup.interrupt();
        asyncWorkers.shutdown();
    }

    public AsyncWorkers getAsyncWorkers() {
        return asyncWorkers;
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
