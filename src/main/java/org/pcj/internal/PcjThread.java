/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal;

import org.pcj.StartPoint;
import org.pcj.Storage;

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

    final private Class<? extends StartPoint> startPointClass;
    final private PcjThreadGroup pcjThreadGroup;
    final private int threadId;
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

    public int getThreadId() {
        return threadId;
    }

    public PcjThreadData getThreadData() {
        return pcjThreadGroup.getThreadData();
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

    @Override
    public void run() {
        try {
            StartPoint startPoint = startPointClass.newInstance();

            startPoint.main();
        } catch (Throwable t) {
            this.throwable = t;
        }
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public static Storage getThreadStorage() {
        PcjThreadGroup tg = getPcjThreadGroupForCurrentThread();
        if (tg == null) {
            throw new IllegalStateException("Current thread is not part of PcjThread.");
        }
        return tg.getThreadData().getStorage();
    }

    public static InternalCommonGroup getThreadGlobalGroup() {
        PcjThreadGroup tg = getPcjThreadGroupForCurrentThread();
        if (tg == null) {
            throw new IllegalStateException("Current thread is not part of PcjThread.");
        }
        return tg.getThreadData().getGlobalGroup();
    }

//    public InternalStorage getThreadStorage() {
//        return threadGroup.data.getThreadStorage();
//    }
//
//    public Map<String, InternalCommonGroup> getGroups() {
//        return threadGroup.data.getGroupsByName();
//    }
//
//    public InternalCommonGroup getGroup(String name) {
//        return threadGroup.data.getGroupsByName().get(name);
//    }
}
