/*
 * Copyright (c) 2014-2016, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * This file is the part of the PCJ Library.
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
     * @param storages         (optional) Shared Enum classes to register shared variables
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
     * @param storages         (optional) Shared Enum classes to register shared variables
     */
    @SafeVarargs
    public static void deploy(Class<? extends StartPoint> startPoint,
            NodesDescription nodesDescription,
            Class<? extends Enum<? extends Shared>>... storages) {
        DeployPCJ.deploy(startPoint, nodesDescription, Arrays.asList(storages));
    }

    /**
     * Gets unique node identifier. Node identifiers are consecutive numbers that starts from 0.
     *
     * @return node identifier
     */
    public static int getNodeId() {
        return InternalPCJ.getNodeData().getPhysicalId();
    }

    /**
     * Gets total number of nodes.
     *
     * @return total number of nodes.
     */
    public static int getNodeCount() {
        return InternalPCJ.getNodeData().getTotalNodeCount();
    }

    /**
     * Gets identifier of current PCJ Thread in the global group. Identifiers are consecutive
     * numbers that
     * start with 0.
     *
     * @return current PCJ Thread identifier
     */
    public static int myId() {
        return getGlobalGroup().myId();
    }

    /**
     * Gets total number of PCJ Thread in the global group.
     *
     * @return total number of PCJ Thread in the global group
     */
    public static int threadCount() {
        return getGlobalGroup().threadCount();
    }

    /**
     * Gets global group.
     *
     * Static methods of this class and methods of the global group do the same things -- static
     * methods are wrapper for methods in global group.
     *
     * @return global group
     */
    public static Group getGlobalGroup() {
        return PcjThread.getThreadGlobalGroup();
    }

    /**
     * Register shared variable for current PCJ Thread.
     *
     * @param variable variable to register
     */
    public static void registerShared(Shared variable) {
        PcjThread.getThreadStorage().registerShared(variable);
    }

    /**
     * Register all enum constants representing shared variables for current PCJ Thread.
     *
     * @param sharedEnum Shared Enum class
     */
    public static void registerShared(Class<? extends Enum<? extends Shared>> sharedEnum) {
        if (sharedEnum.isEnum() == false || Shared.class.isAssignableFrom(sharedEnum) == false) {
            throw new IllegalArgumentException("Argument is not shared enum class");
        }

        InternalStorage storage = PcjThread.getThreadStorage();
        Arrays.stream(sharedEnum.getEnumConstants())
                .map(e -> (Shared) e)
                .forEach(storage::registerShared);
    }

    /**
     * Starts asynchronos barrier. After starting barrier the PcjFuture is returned.
     *
     * PCJ Thread can continue to work and can check returned PcjFuture if every thread done this
     * barrier using {@link PcjFuture#isDone()} method. PcjFuture returns null when completed.
     *
     * @return PcjFuture to check barrier state
     */
    public static PcjFuture<Void> asyncBarrier() {
        return getGlobalGroup().asyncBarrier();
    }

    /**
     * Synchronous barrier.
     *
     * Wrapper for {@link asyncBarrier()}. It is the equivalent to call:
     *
     * {@code PCJ.asyncBarrier().get();}
     */
    public static void barrier() {
        PCJ.asyncBarrier().get();
    }

    /**
     * Starts asynchronous barrier with one peer PCJ Thread. Given threadId should be different from
     * current PCJ Thread id, otherwise the exception is thrown.
     *
     * PCJ Thread can continue to work and can check returned PcjFuture if every thread done this
     * barrier using {@link PcjFuture#isDone()} method. PcjFuture returns null when completed.
     *
     * @param threadId global PCJ Thread
     *
     * @return PcjFuture to check barrier state
     */
    public static PcjFuture<Void> asyncBarrier(int threadId) {
        return getGlobalGroup().asyncBarrier(threadId);
    }

    /**
     * Synchronous barrier with one peer PCJ Thread.
     *
     * Wrapper for (@link asyncBarrier(int)}. It is the equivalent to call:
     *
     * {@code PCJ.asyncBarrier(threadId).get();}
     *
     * @param threadId
     */
    public static void barrier(int threadId) {
        PCJ.asyncBarrier(threadId).get();
    }

    /**
     * Clear modification count of the shared variable.
     *
     * @param variable shared variable
     *
     * @return modification count before clearing
     */
    public static void monitor(Shared variable) {
        PcjThread.getThreadStorage().monitor(variable);
    }

    /**
     * Checks and optionally waits for one modification. Decrease number of modification count.
     *
     * @param variable shared variable
     *
     * @return remaining modification count
     */
    public static int waitFor(Shared variable) {
        return waitFor(variable, 1);
    }

    /**
     * Checks and optionally waits for many ({@code count}) modifications. Decrease number of
     * modification count.
     *
     * @param variable shared variable
     * @param count    number of modifications
     *
     * @return remaining modification count
     */
    public static int waitFor(Shared variable, int count) {
        return PcjThread.getThreadStorage().waitFor(variable, count);
    }

    /**
     * Checks and optionally waits for many ({@code count}) modifications. Decrease number of
     * modification count.
     *
     * @param variable shared variable
     * @param count    number of modifications
     * @param timeout  timeout
     * @param unit     unit of time
     *
     * @return remaining modification count
     *
     * @throws TimeoutException when not so much modifications occurs till timeout
     */
    public static int waitFor(Shared variable, int count,
            long timeout, TimeUnit unit) throws TimeoutException {
        return PcjThread.getThreadStorage().waitFor(variable, count, timeout, unit);
    }

    /**
     * Gets reference to shared variable of current PCJ Thread.
     *
     * @param <T>      type of variable
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     *
     * @return value (reference)
     */
    public static <T> T getLocal(Shared variable, int... indices) {
        return PcjThread.getThreadStorage().get(variable, indices);
    }

    /**
     * Puts value to shared variable of current PCJ Thread.
     *
     * @param <T>      type of variable
     * @param variable variable name
     * @param newValue value (reference)
     * @param indices  (optional) indices for array variable
     *
     * @throws ClassCastException when unable to put because of wrong type
     */
    public static <T> void putLocal(Shared variable, T newValue, int... indices) throws ClassCastException {
        PcjThread.getThreadStorage().put(variable, newValue, indices);
    }

    /**
     * Asynchronous get operation. Gets value of shared variable from PCJ Thread from the global
     * group.
     *
     * @param <T>      type of value
     * @param threadId peer PCJ Thread
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     *
     * @return PcjFuture that will contain shared variable value
     */
    public static <T> PcjFuture<T> asyncGet(int threadId, Shared variable, int... indices) {
        return getGlobalGroup().asyncGet(threadId, variable, indices);
    }

    /**
     * Synchronous get operation.
     *
     * Wrapper for (@link asyncGet(int,Shared,int...)}. It is the equivalent to call:
     *
     * {@code PCJ.<T>asyncGet(threadId, variable, indices).get();}
     *
     * @param <T>      type of value
     * @param threadId peer PCJ Thread
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     *
     * @return value of variable
     *
     * @throws PcjRuntimeException possible wrapped exception (eg. ArrayOutOfBoundException).
     */
    public static <T> T get(int threadId, Shared variable, int... indices) throws PcjRuntimeException {
        return PCJ.<T>asyncGet(threadId, variable, indices).get();
    }

    /**
     * Asynchronous put operation. Puts value into shared variable to PCJ Thread from the global
     * group.
     *
     * @param <T>      type of value
     * @param threadId peer PCJ Thread
     * @param variable variable name
     * @param newValue new variable value
     * @param indices  (optional) indices for array variable
     *
     * @return PcjFuture to check completion of put operation
     */
    public static <T> PcjFuture<Void> asyncPut(int threadId, Shared variable, T newValue, int... indices) {
        return getGlobalGroup().asyncPut(threadId, variable, newValue, indices);
    }

    /**
     * Synchronous put operation.
     *
     * Wrapper for (@link asyncPut(int,Shared,T,int...)}. It is the equivalent to call:
     *
     * {@code PCJ.<T>asyncPut(threadId, variable, newValue, indices).get();}
     *
     * @param <T>      type of value
     * @param threadId peer PCJ Thread
     * @param variable variable name
     * @param newValue new variable value
     * @param indices  (optional) indices for array variable
     *
     * @throws PcjRuntimeException possible wrapped exception (eg. ArrayOutOfBoundException).
     */
    public static <T> void put(int threadId, Shared variable, T newValue, int... indices) throws PcjRuntimeException {
        PCJ.<T>asyncPut(threadId, variable, newValue, indices).get();
    }

    /**
     * Asynchronous broadcast operation. Broadcasts value into shared variable of all PCJ Threads
     * from the global group.
     *
     * @param <T>      type of value
     * @param variable variable name
     * @param newValue new variable value
     *
     * @return PcjFuture to check completion of put operation
     */
    public static <T> PcjFuture<Void> asyncBroadcast(Shared variable, T newValue) {
        return getGlobalGroup().asyncBroadcast(variable, newValue);
    }

    /**
     * Synchronous broadcast operation.
     *
     * Wrapper for (@link asyncBroadcast(Shared,T)}. It is the equivalent to call:
     *
     * {@code PCJ.<T>asyncBroadcast(variable, newValue).get();}
     *
     * @param <T>
     * @param variable
     * @param newValue
     */
    public static <T> void broadcast(Shared variable, T newValue) {
        PCJ.<T>asyncBroadcast(variable, newValue).get();
    }

    /**
     * Joins current PCJ Thread to the group.
     *
     * If current PCJ Thread is already in the group, returns the group.
     *
     * @param name name of the group
     *
     * @return joined group
     */
    public static Group join(String name) {
        int myThreadId = getGlobalGroup().myId();
        return (Group) InternalPCJ.join(myThreadId, name);
    }

//    public static <T, R> PcjFuture<R> asyncAt(int threadId, Function<R, T> lambda) throws PcjRuntimeException {
//        throw new UnsupportedOperationException("Not implemented yet.");
//    }
}
