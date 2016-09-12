/* 
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.pcj.PCJ;
import org.pcj.StartPoint;
import org.pcj.Storage;
import org.pcj.RegisterStorage;

/**
 * This class represents PCJ thread.
 *
 * Thread contains reference to its own local data.
 *
 * When thread is run deserializer also runs.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class PcjThread extends Thread {

    private final Class<? extends StartPoint> startPointClass;
    private final PcjThreadGroup pcjThreadGroup;
    private final int threadId;
    private Throwable throwable;

    PcjThread(int threadId, Class<? extends StartPoint> startPoint, PcjThreadData threadData) {
        super(new PcjThreadGroup("PcjThreadGroup-" + threadId, threadData), "PcjThread-" + threadId);

        this.threadId = threadId;
        this.pcjThreadGroup = (PcjThreadGroup) this.getThreadGroup();

        this.startPointClass = startPoint;
    }

    private static class PcjThreadGroup extends ThreadGroup {

        private final PcjThreadData threadData;

        public PcjThreadGroup(String name, PcjThreadData threadData) {
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

            /* be sure that each thread initialized startPoint and storages */
            PCJ.barrier();

            /* start calculations */
            startPoint.main();

            /* be sure that each thread finishes before continuing */
            PCJ.barrier();
        } catch (Throwable t) {
            this.throwable = t;
        }
    }

    private StartPoint getStartPointObject() throws SecurityException, RuntimeException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException, NoSuchMethodException {
        StartPoint startPoint = initializeStorages();
        if (startPoint == null) {
            startPoint = initializeStartPointClass();
        }
        return startPoint;
    }

    private StartPoint initializeStorages() throws IllegalAccessException, NoSuchFieldException, InstantiationException, RuntimeException {
        StartPoint startPoint = null;
            
        RegisterStorage[] registerStorages = startPointClass.getAnnotationsByType(RegisterStorage.class);

        for (RegisterStorage registerStorage : registerStorages) {
            Class<? extends Enum<?>> sharedEnumClass = registerStorage.value();
            Storage storageAnnotation = sharedEnumClass.getAnnotation(Storage.class);
            if (storageAnnotation == null) {
                throw new RuntimeException("Enum is not annotated by @Storage annotation: " + sharedEnumClass.getName());
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

    public int getThreadId() {
        return threadId;
    }

    public Throwable getThrowable() {
        return throwable;
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

    public PcjThreadData getThreadData() {
        return pcjThreadGroup.getThreadData();
    }
}
