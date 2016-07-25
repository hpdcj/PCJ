/*
 * This file is the part of the PCJ Library
 */
package org.pcj;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.pcj.internal.DeployPCJ;
import org.pcj.internal.InternalGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.InternalStorage;
import org.pcj.internal.PcjThread;

/**
 * Main PCJ class with static methods.
 *
 * Static methods provide way to use library.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class PCJ extends InternalPCJ {

    /* Suppress default constructor for noninstantiability */
    private PCJ() {
        throw new AssertionError();
    }

    /**
     * Starts PCJ calculations on local node using specified StartPoint.
     * NodesDescription contains list of all hostnames used in calculations. Hostnames can be
     * specified many times, so more than one instance of PCJ will be run on node (called threads).
     *
     * @param startPoint       start point class
     * @param nodesDescription description of used nodes (and threads)
     */
    @SafeVarargs
    public static void start(Class<? extends StartPoint> startPoint,
            NodesDescription nodesDescription,
            Class<? extends Enum<? extends Shared>>... storages) {
        InternalPCJ.start(startPoint, nodesDescription, Arrays.asList(storages));
    }

    /**
     * Deploys and starts PCJ calculations on nodes using specified StartPoint
     * class.
     * NodesDescription contains list of all hostnames used in calculations.
     * Hostnames can be specified many times, so more than one instance
     * of PCJ will be run on node (called threads). Empty hostnames means current JVM.
     *
     * Hostnames can take port (after colon ':'), eg. ["localhost:8000", "localhost:8001",
     * "localhost", "host2:8001", "host2"]. Default port is 8091 and can be modified using
     * <tt>pcj.port</tt> system property value (-Dpcj.port=8091).
     *
     * @param startPoint       start point class
     * @param nodesDescription description of used nodes (and threads)
     */
    @SafeVarargs
    public static void deploy(Class<? extends StartPoint> startPoint,
            NodesDescription nodesDescription,
            Class<? extends Enum<? extends Shared>>... storages) {
        DeployPCJ.deploy(startPoint, nodesDescription, Arrays.asList(storages));
    }

    public static int myId() {
        return ((InternalGroup) PcjThread.getThreadGlobalGroup()).myId();
    }

    public static int threadCount() {
        return ((InternalGroup) PcjThread.getThreadGlobalGroup()).threadCount();
    }

    public static int getNodeId() {
        return InternalPCJ.getNodeData().getPhysicalId();
    }

    public static int getNodeCount() {
        return InternalPCJ.getNodeData().getTotalNodeCount();
    }

    public static void createShared(Enum<? extends Shared> variable) {
        PcjThread.getThreadStorage().createShared(variable);
    }

    public static void createShared(Class<? extends Enum<? extends Shared>> sharedEnum) {
        if (sharedEnum.isEnum() == false) {
            throw new IllegalArgumentException("Argument is not enum");
        }
//        if (element instanceof Shared == false){
        if (Shared.class.isAssignableFrom(sharedEnum) == false) {
            throw new IllegalArgumentException("Argument is not shared");
        }
        InternalStorage storage = PcjThread.getThreadStorage();
        Arrays.stream(sharedEnum.getEnumConstants()).forEach(storage::createShared);
    }

    public static PcjFuture<Void> asyncBarrier() {
        return ((InternalGroup) PcjThread.getThreadGlobalGroup()).asyncBarrier();
    }

    public static void barrier() {
        PCJ.asyncBarrier().get();
    }

    public static void monitor(Enum<? extends Shared> variable) {
        PcjThread.getThreadStorage().monitor(variable);
    }

    public static int waitFor(Enum<? extends Shared> variable) {
        return waitFor(variable, 1);
    }

    public static int waitFor(Enum<? extends Shared> variable, int count) {
        return PcjThread.getThreadStorage().waitFor(variable, count);
    }

    public static int waitFor(Enum<? extends Shared> variable, int count,
            long timeout, TimeUnit unit) throws TimeoutException {
        return PcjThread.getThreadStorage().waitFor(variable, count, timeout, unit);
    }

    public static <T> T getLocal(Enum<? extends Shared> variable, int... indices) {
        return PcjThread.getThreadStorage().get(variable, indices);
    }

    public static <T> void putLocal(Enum<? extends Shared> variable, T newValue, int... indices) throws ClassCastException {
        PcjThread.getThreadStorage().put(variable, newValue, indices);
    }

    public static <T> PcjFuture<T> asyncGet(int threadId, Enum<? extends Shared> variable, int... indices) {
        return ((InternalGroup) PcjThread.getThreadGlobalGroup()).asyncGet(threadId, variable, indices);
    }

    public static <T> T get(int threadId, Enum<? extends Shared> variable, int... indices) throws PcjRuntimeException {
        return PCJ.<T>asyncGet(threadId, variable, indices).get();
    }

    public static <T> PcjFuture<Void> asyncPut(int threadId, Enum<? extends Shared> variable, T newValue, int... indices) {
        return ((InternalGroup) PcjThread.getThreadGlobalGroup()).asyncPut(threadId, variable, newValue, indices);
    }

    public static <T> void put(int threadId, Enum<? extends Shared> variable, T newValue, int... indices) {
        PCJ.<T>asyncPut(threadId, variable, newValue, indices).get();
    }

//    /**
//     * Broadcast the value to all threads and inserts it into InternalStorage
//     *
//     * @param variable name of variable
//     * @param newValue new value of variable
//     *
//     * @throws ClassCastException when the value cannot be cast to the type of variable in InternalStorage
//     */
//    public static PcjFuture<Void> broadcast(String variable, Object newValue) throws ClassCastException, PcjRuntimeException {
//        if (PcjThread.threadStorage().isAssignable(variable, newValue) == false) {
//            throw new ClassCastException("Cannot cast " + newValue.getClass().getCanonicalName()
//                    + " to the type of variable '" + variable + "'");
//        }
//        return ((InternalGroup) PcjThread.getThreadGlobalGroup()).broadcast(variable, newValue);
//    }
//    
//    public static PcjFuture<R> asyncAt(int threadId, Function<R,T> lambda) throws PcjRuntimeException {
//    }
//
//    /**
//     * Returns the global group
//     *
//     * @return the global group
//     */
//    public static InternalGroup getGlobalGroup() {
//        return ((InternalGroup) PcjThread.getThreadGlobalGroup());
//    }
//
//    /**
//     * Returns group by name
//     *
//     * @param name name of the group
//     *
//     * @return group by name
//     */
//    public static InternalGroup getGroup(String name) {
//        return (InternalGroup) PcjThread.threadGroup(name);
//    }
//
//    /**
//     * Joins the current thread to the group
//     *
//     * @param name name of the group
//     *
//     * @return group to which thread joined
//     */
//    public static InternalGroup join(String name) {
//        int myThreadId = ((InternalGroup) PcjThread.getThreadGlobalGroup()).myId();
//        return (InternalGroup) InternalPCJ.join(myThreadId, name);
//    }
}
