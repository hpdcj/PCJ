/*
 * This file is the part of the PCJ Library
 */
package org.pcj;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.pcj.internal.DeployPCJ;
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

    public static int getNodeId() {
        return InternalPCJ.getNodeData().getPhysicalId();
    }

    public static int getNodeCount() {
        return InternalPCJ.getNodeData().getTotalNodeCount();
    }

    public static int myId() {
        return getGlobalGroup().myId();
    }

    public static int threadCount() {
        return getGlobalGroup().threadCount();
    }

    public static Group getGlobalGroup() {
        return PcjThread.getThreadGlobalGroup();
    }

    public static void createShared(Shared variable) {
        PcjThread.getThreadStorage().createShared(variable);
    }

    public static void createShared(Class<? extends Enum<? extends Shared>> sharedEnum) {
        if (sharedEnum.isEnum() == false || Shared.class.isAssignableFrom(sharedEnum) == false) {
            throw new IllegalArgumentException("Argument is not shared enum");
        }

        InternalStorage storage = PcjThread.getThreadStorage();
        Arrays.stream(sharedEnum.getEnumConstants())
                .map(e -> (Shared) e)
                .forEach(storage::createShared);
    }

    public static PcjFuture<Void> asyncBarrier() {
        return getGlobalGroup().asyncBarrier();
    }

    public static void barrier() {
        PCJ.asyncBarrier().get();
    }

    public static PcjFuture<Void> asyncBarrier(int threadId) {
        return getGlobalGroup().asyncBarrier(threadId);
    }

    public static void barrier(int threadId) {
        PCJ.asyncBarrier(threadId).get();
    }

    public static void monitor(Shared variable) {
        PcjThread.getThreadStorage().monitor(variable);
    }

    public static int waitFor(Shared variable) {
        return waitFor(variable, 1);
    }

    public static int waitFor(Shared variable, int count) {
        return PcjThread.getThreadStorage().waitFor(variable, count);
    }

    public static int waitFor(Shared variable, int count,
            long timeout, TimeUnit unit) throws TimeoutException {
        return PcjThread.getThreadStorage().waitFor(variable, count, timeout, unit);
    }

    public static <T> T getLocal(Shared variable, int... indices) {
        return PcjThread.getThreadStorage().get(variable, indices);
    }

    public static <T> void putLocal(Shared variable, T newValue, int... indices) throws ClassCastException {
        PcjThread.getThreadStorage().put(variable, newValue, indices);
    }

    public static <T> PcjFuture<T> asyncGet(int threadId, Shared variable, int... indices) {
        return getGlobalGroup().asyncGet(threadId, variable, indices);
    }

    public static <T> T get(int threadId, Shared variable, int... indices) throws PcjRuntimeException {
        return PCJ.<T>asyncGet(threadId, variable, indices).get();
    }

    public static <T> PcjFuture<Void> asyncPut(int threadId, Shared variable, T newValue, int... indices) {
        return getGlobalGroup().asyncPut(threadId, variable, newValue, indices);
    }

    public static <T> void put(int threadId, Shared variable, T newValue, int... indices) {
        PCJ.<T>asyncPut(threadId, variable, newValue, indices).get();
    }

    public static <T> PcjFuture<Void> asyncBroadcast(Shared variable, T newValue) {
        return getGlobalGroup().asyncBroadcast(variable, newValue);
    }

    public static <T> void broadcast(Shared variable, T newValue) {
        PCJ.<T>asyncBroadcast(variable, newValue).get();
    }

    public static Group join(String name) {
        int myThreadId = getGlobalGroup().myId();
        return (Group) InternalPCJ.join(myThreadId, name);
    }

//    public static <T, R> PcjFuture<R> asyncAt(int threadId, Function<R, T> lambda) throws PcjRuntimeException {
//        throw new UnsupportedOperationException("Not implemented yet.");
//    }
}
