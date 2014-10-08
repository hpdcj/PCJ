/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal;

import java.util.Map;
import org.pcj.internal.storage.InternalStorage;

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
    //<editor-fold defaultstate="collapsed" desc="public void run()">

    final private InternalStartPoint startPoint;
    final private PcjThreadGroup threadGroup;
    final private Thread deserializer;

    @Override
    public void run() {
        try {
            deserializer.start();
            startPoint.main();
        } catch (Throwable ex) {
            ex.printStackTrace(System.err);
            System.exit(5);
        } finally {
            // TODO: sprawdzić, czy ten interrupt jest konieczny
            deserializer.interrupt();
        }
    }

    PcjThread(int id, InternalStartPoint startPoint, PcjThreadLocalData data) {
        super(new PcjThreadGroup("Node:" + id, data), "NodeThread:" + id);

        this.deserializer = new Thread(data.getDeserializer(), "Deserializer:" + id);
        this.deserializer.setDaemon(true);

        this.threadGroup = (PcjThreadGroup) this.getThreadGroup();

        this.startPoint = startPoint;
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="static thread* methods">

    private static PcjThreadGroup threadPcjThreadGroup() {
        ThreadGroup group = Thread.currentThread().getThreadGroup();
        if (group instanceof PcjThreadGroup) {
            return (PcjThreadGroup) group;
        }
        while (group.getParent() != null) {
            group = group.getParent();
            if (group instanceof PcjThreadGroup) {
                return (PcjThreadGroup) group;
            }
        }
        return null;
    }

    public static InternalStorage threadStorage() {
        PcjThreadGroup tg = threadPcjThreadGroup();
        if (tg == null) {
            return null;
        }
        return tg.data.getStorage();
    }

    public static InternalGroup threadGroup(String name) {
        PcjThreadGroup tg = threadPcjThreadGroup();
        if (tg == null) {
            return null;
        }
        return tg.data.getGroupsByName().get(name);
    }

    public static InternalGroup threadGlobalGroup() {
        PcjThreadGroup tg = threadPcjThreadGroup();
        if (tg == null) {
            return null;
        }
        return tg.data.getGlobalGroup();
    }
    //</editor-fold>

    private static class PcjThreadGroup extends ThreadGroup {

        private final PcjThreadLocalData data;

        public PcjThreadGroup(String name, PcjThreadLocalData data) {
            super(name);
            this.data = data;
        }
    }

    public InternalStorage getStorage() {
        return threadGroup.data.getStorage();
    }

    public Map<String, InternalGroup> getGroups() {
        return threadGroup.data.getGroupsByName();
    }

    public InternalGroup getGroup(String name) {
        return threadGroup.data.getGroupsByName().get(name);
    }

    public InternalGroup getGlobalGroup() {
        return threadGroup.data.getGlobalGroup();
    }
}
