/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.pcj.internal.InternalGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.PcjThread;

/**
 * Main PCJ class with static methods.
 * <p>
 * Static methods provide way to use library.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public final class PCJ {

    /* Suppress default constructor for noninstantiability */
    private PCJ() {
        throw new AssertionError();
    }

    /**
     * Creates execution builder for starting the application using startPoint class.
     * <p>
     * It does not start application.
     * It is necessary to execute {@link ExecutionBuilder#deploy()} or {@link ExecutionBuilder#start()} method to start
     * application.
     * <p>
     * The example of usage:
     * <pre>
     * PCJ.executionBuilder(Hello.class)
     *    .addNodes(new File("nodes.txt"))
     *    .deploy();
     * </pre>
     *
     * @param startPoint start point class
     * @return ExecutionBuilder for chain configuration and starting application
     */
    public static ExecutionBuilder executionBuilder(Class<? extends StartPoint> startPoint) {
        return new ExecutionBuilder(startPoint);
    }

    /**
     * Gets unique node identifier. Node identifiers are consecutive numbers that starts from 0.
     *
     * @return node identifier
     */
    public static int getNodeId() {
        return InternalPCJ.getNodeData().getCurrentNodePhysicalId();
    }

    /**
     * Gets total number of nodes.
     *
     * @return total number of nodes.
     */
    public static int getNodeCount() {
        return InternalPCJ.getNodeData().getTotalNodeCount();
    }


    public static Properties getProperties() {
        return InternalPCJ.getConfiguration().getProperties();
    }

    public static String getProperty(String key) {
        return getProperties().getProperty(key);
    }

    /**
     * Gets identifier of current PCJ Thread in the global group.
     * Identifiers are consecutive numbers that start with 0.
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
     * <p>
     * Static methods of this class and methods of the global group do the same things
     * -- static methods are wrapper for methods in global group.
     *
     * @return global group
     */
    public static Group getGlobalGroup() {
        return PcjThread.getCurrentThreadData().getGlobalGroup();
    }

    /**
     * Register storage as enum' constants.
     * <p>
     * This method creates object pointing by enum' annotation when necessary,
     * that is when the class pointed by annotation was not previously used to store shared variables.
     *
     * @param storageEnumClass Enum class that represents storage shared
     *                         variables
     * @return Object associated with Storage (type of value from enum
     * annotation)
     * @throws PcjRuntimeException thrown when there is problem with registering
     *                             provided class.
     */
    public static Object registerStorage(Class<? extends Enum<?>> storageEnumClass)
            throws PcjRuntimeException {
        return PcjThread.getCurrentThreadData().getStorages().registerStorage(storageEnumClass);
    }

    /**
     * Register storage as enum' constants.
     * <p>
     * This method uses provided object as storage of shared variables.
     * The object has to be the same type as the type pointed by enum' annotation.
     * <p>
     * If the {@code storageObject} is {@code null}, it works like {@link #registerStorage(java.lang.Class)} method.
     *
     * @param storageEnumClass Enum class that represents storage shared
     *                         variables
     * @param storageObject    object that stores the shared variables
     * @return Object associated with Storage (type of value from enum
     * annotation)
     * @throws PcjRuntimeException thrown when there is problem with registering
     *                             provided class.
     */
    public static Object registerStorage(Class<? extends Enum<?>> storageEnumClass, Object storageObject)
            throws PcjRuntimeException {
        return PcjThread.getCurrentThreadData().getStorages().registerStorage(storageEnumClass, storageObject);
    }

    /**
     * Get object associated with registered storage.
     *
     * @param sharedEnumClass Enum class that represents storage shared
     *                        variables
     * @return Object associated with Storage (type of value from enum
     * annotation)
     */
    public static Object getStorageObject(Class<? extends Enum<?>> sharedEnumClass) {
        return PcjThread.getCurrentThreadData().getStorages().getStorage(sharedEnumClass);
    }

    /**
     * Starts asynchronos barrier. After starting barrier the PcjFuture is returned.
     * <p>
     * PCJ Thread can continue to work and can check returned PcjFuture to find if every thread arrive at this barrier
     * using {@link PcjFuture#isDone()} method.
     * PcjFuture returns null when completed.
     *
     * @return PcjFuture to check barrier state
     */
    public static PcjFuture<Void> asyncBarrier() {
        return getGlobalGroup().asyncBarrier();
    }

    /**
     * Synchronous barrier.
     * <p>
     * Wrapper for {@link PCJ#asyncBarrier()}. It is the equivalent to call:
     *
     * <blockquote>{@code PCJ.asyncBarrier().get();}</blockquote>
     */
    public static void barrier() {
        PCJ.asyncBarrier().get();
    }

    /**
     * Starts asynchronous barrier with one peer PCJ Thread.
     * Given threadId should be different from current PCJ Thread id, otherwise the exception is thrown.
     * <p>
     * PCJ Thread can continue to work and can check returned PcjFuture to find if every thread arrive at this barrier
     * using {@link PcjFuture#isDone()} method.
     * PcjFuture returns null when completed.
     *
     * @param threadId global PCJ Thread id
     * @return PcjFuture to check barrier state
     */
    public static PcjFuture<Void> asyncBarrier(int threadId) {
        return getGlobalGroup().asyncBarrier(threadId);
    }

    /**
     * Synchronous barrier with one peer PCJ Thread.
     * <p>
     * Wrapper for {@link #asyncBarrier(int)}.
     * It is the equivalent to call:
     *
     * <blockquote>{@code PCJ.asyncBarrier(threadId).get();}</blockquote>
     *
     * @param threadId global PCJ Thread id
     */
    public static void barrier(int threadId) {
        PCJ.asyncBarrier(threadId).get();
    }

    /**
     * Clear modification count of the shared variable.
     *
     * @param variable shared variable
     * @return modification count before clearing
     */
    public static int monitor(Enum<?> variable) {
        return PcjThread.getCurrentThreadData().getStorages().monitor(variable);
    }

    /**
     * Checks and optionally waits for one modification.
     * Decrease number of modification count.
     * <p>
     * It is the equivalent to call:
     *
     * <blockquote>{@code PCJ.waitFor(variable, 1);}</blockquote>
     *
     * @param variable shared variable
     * @return remaining modification count
     */
    public static int waitFor(Enum<?> variable) {
        return waitFor(variable, 1);
    }

    /**
     * Checks and optionally waits for many ({@code count}) modifications.
     * Decreases number of modification count.
     *
     * @param variable shared variable
     * @param count    number of modifications
     * @return remaining modification count
     */
    public static int waitFor(Enum<?> variable, int count) {
        return PcjThread.getCurrentThreadData().getStorages().waitFor(variable, count);
    }

    /**
     * Checks and optionally waits for many ({@code count}) modifications.
     * Decreases number of modification count.
     *
     * @param variable shared variable
     * @param count    number of modifications
     * @param timeout  timeout
     * @param unit     unit of time
     * @return remaining modification count
     * @throws TimeoutException when not so much modifications occurs till
     *                          timeout
     */
    public static int waitFor(Enum<?> variable, int count,
                              long timeout, TimeUnit unit) throws TimeoutException {
        return PcjThread.getCurrentThreadData().getStorages().waitFor(variable, count, timeout, unit);
    }

    /**
     * Gets reference to shared variable of current PCJ Thread.
     *
     * @param <T>      type of variable
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @return value (reference)
     */
    public static <T> T getLocal(Enum<?> variable, int... indices) {
        return PcjThread.getCurrentThreadData().getStorages().get(variable, indices);
    }

    /**
     * Puts value to shared variable of current PCJ Thread.
     *
     * @param <T>      type of variable
     * @param newValue value (reference)
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @throws PcjRuntimeException contains wrapped exception (eg.
     *                             ArrayOutOfBoundException).
     */
    public static <T> void putLocal(T newValue, Enum<?> variable, int... indices) throws PcjRuntimeException {
        try {
            PcjThread.getCurrentThreadData().getStorages().put(newValue, variable, indices);
        } catch (Exception ex) {
            throw new PcjRuntimeException(ex);
        }
    }

    /**
     * Accumulates value to shared variable of current PCJ Thread.
     *
     * @param <T>      type of variable
     * @param function reduce function
     * @param newValue value (reference)
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @throws PcjRuntimeException contains wrapped exception (eg.
     *                             ArrayOutOfBoundException).
     */
    public static <T> void accumulateLocal(ReduceOperation<T> function, T newValue, Enum<?> variable, int... indices) throws PcjRuntimeException {
        try {
            PcjThread.getCurrentThreadData().getStorages().accumulate(function, newValue, variable, indices);
        } catch (Exception ex) {
            throw new PcjRuntimeException(ex);
        }
    }

    /**
     * Asynchronous get operation. Gets value of shared variable from PCJ Thread from the global group.
     *
     * @param <T>      type of value
     * @param threadId peer PCJ Thread
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @return {@link org.pcj.PcjFuture} that will contain shared variable value
     */
    public static <T> PcjFuture<T> asyncGet(int threadId, Enum<?> variable, int... indices) {
        return getGlobalGroup().asyncGet(threadId, variable, indices);
    }

    /**
     * Synchronous get operation.
     * <p>
     * Wrapper for {@code asyncGet(int, java.lang.Enum<?>, int...)}.
     * It is the equivalent to call:
     *
     * <blockquote>{@code PCJ.<T>asyncGet(threadId, variable, indices).get();}</blockquote>
     *
     * @param <T>      type of value
     * @param threadId peer PCJ Thread
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @return value of variable
     * @throws PcjRuntimeException contains wrapped exception (eg.
     *                             ArrayOutOfBoundException).
     */
    public static <T> T get(int threadId, Enum<?> variable, int... indices) throws PcjRuntimeException {
        return PCJ.<T>asyncGet(threadId, variable, indices).get();
    }

    /**
     * Asynchronous collect operation. Gets value of shared variable from all PCJ Threads from the group.
     *
     * @param <T>      type of value
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @return {@link org.pcj.PcjFuture} that will contain shared variable value
     */
    public static <T> PcjFuture<T> asyncCollect(Enum<?> variable, int... indices) throws PcjRuntimeException {
        return getGlobalGroup().asyncCollect(variable, indices);
    }

    /**
     * Synchronous collect operation.
     * <p>
     * Wrapper for {@code asyncCollect(java.lang.Enum<?>, int...)}.
     * It is the equivalent to call:
     *
     * <blockquote>{@code PCJ.<T>asyncCollect(variable, indices).get();}</blockquote>
     *
     * @param <T>      type of value
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @return {@link org.pcj.PcjFuture} that will contain shared variable value
     */
    public static <T> T collect(Enum<?> variable, int... indices) throws PcjRuntimeException {
        return PCJ.<T>asyncCollect(variable, indices).get();
    }

    /**
     * Asynchronous reduce operation. Reduce value of shared variable from all PCJ Threads from the group.
     *
     * @param <T>      type of value
     * @param function reduce function
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @return {@link org.pcj.PcjFuture} that will contain shared variable value
     */
    public static <T> PcjFuture<T> asyncReduce(ReduceOperation<T> function, Enum<?> variable, int... indices) {
        return getGlobalGroup().asyncReduce(function, variable, indices);
    }

    /**
     * Synchronous reduce operation.
     * <p>
     * Wrapper for {@code asyncReduce(BinaryOperator<T>, java.lang.Enum<?>, int...)}.
     * It is the equivalent to call:
     *
     * <blockquote>{@code PCJ.<T>asyncReduce(function, variable, indices).get();}</blockquote>
     *
     * @param <T>      type of value
     * @param function reduce function
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @return {@link org.pcj.PcjFuture} that will contain shared variable value
     */
    public static <T> T reduce(ReduceOperation<T> function, Enum<?> variable, int... indices) {
        return PCJ.asyncReduce(function, variable, indices).get();
    }

    /**
     * Asynchronous put operation. Puts value into shared variable to PCJ Thread from the global group.
     *
     * @param <T>      type of value
     * @param newValue new variable value
     * @param threadId peer PCJ Thread
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @return {@link org.pcj.PcjFuture}&lt;{@link java.lang.Void}&gt;
     */
    public static <T> PcjFuture<Void> asyncPut(T newValue, int threadId, Enum<?> variable, int... indices) {
        return getGlobalGroup().asyncPut(newValue, threadId, variable, indices);
    }

    /**
     * Synchronous put operation.
     * <p>
     * Wrapper for {@code asyncPut(T, int, java.lang.Enum<?>, int...)}.
     * It is the equivalent to call:
     *
     * <blockquote>{@code PCJ.<T>asyncPut(newValue, threadId, variable, indices).get();}</blockquote>
     *
     * @param <T>      type of value
     * @param newValue new variable value
     * @param threadId peer PCJ Thread
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @throws PcjRuntimeException contains wrapped exception (eg.
     *                             ArrayOutOfBoundException).
     */
    public static <T> void put(T newValue, int threadId, Enum<?> variable, int... indices) throws PcjRuntimeException {
        PCJ.asyncPut(newValue, threadId, variable, indices).get();
    }

    /**
     * Asynchronous accumulate operation. Accumulate value into shared variable to PCJ thread from the global group.
     *
     * @param <T>      type of value
     * @param function reduce function
     * @param newValue new variable value
     * @param threadId peer PCJ Thread
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @return {@link org.pcj.PcjFuture}&lt;{@link java.lang.Void}&gt;
     */
    public static <T> PcjFuture<Void> asyncAccumulate(ReduceOperation<T> function, T newValue, int threadId, Enum<?> variable, int... indices) {
        return getGlobalGroup().asyncAccumulate(function, newValue, threadId, variable, indices);
    }

    /**
     * Synchronous accumulate operation.
     * <p>
     * Wrapper for {@code asyncAccumulate(ReduceOperation<T>, T, int, java.lang.Enum<?>, int...)}.
     * It is the equivalent to call:
     *
     * <blockquote>{@code PCJ.<T>asyncAccumulate(function, newValue, threadId, variable, indices).get();}</blockquote>
     *
     * @param <T>      type of value
     * @param function reduce function
     * @param newValue new variable value
     * @param threadId peer PCJ Thread
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @throws PcjRuntimeException contains wrapped exception (eg.
     *                             ArrayOutOfBoundException).
     */
    public static <T> void accumulate(ReduceOperation<T> function, T newValue, int threadId, Enum<?> variable, int... indices) throws PcjRuntimeException {
        PCJ.asyncAccumulate(function, newValue, threadId, variable, indices).get();
    }

    /**
     * Asynchronous broadcast operation. Broadcasts value into shared variable of all PCJ Threads from the global group.
     *
     * @param <T>      type of value
     * @param newValue new variable value
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @return {@link org.pcj.PcjFuture}&lt;{@link java.lang.Void}&gt;
     */
    public static <T> PcjFuture<Void> asyncBroadcast(T newValue, Enum<?> variable, int... indices) {
        return getGlobalGroup().asyncBroadcast(newValue, variable, indices);
    }

    /**
     * Synchronous broadcast operation.
     * <p>
     * Wrapper for {@code asyncBroadcast(T, Enum<?>)}.
     * It is the equivalent to call:
     *
     * <blockquote>{@code PCJ.<T>asyncBroadcast(variable, newValue).get();}</blockquote>
     *
     * @param <T>      type of value
     * @param newValue new variable value
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     */
    public static <T> void broadcast(T newValue, Enum<?> variable, int... indices) {
        PCJ.asyncBroadcast(newValue, variable, indices).get();
    }

    /**
     * Asynchronous execution operation. Executes associated function on specified thread from global group and returns value.
     *
     * @param <T>       type of returned value
     * @param threadId  peer PCJ Thread
     * @param asyncTask function to be executed
     * @return {@link org.pcj.PcjFuture} that will contain value returned by the
     * function
     */
    public static <T> PcjFuture<T> asyncAt(int threadId, AsyncTask<T> asyncTask) throws PcjRuntimeException {
        return getGlobalGroup().asyncAt(threadId, asyncTask);
    }

    /**
     * Asynchronous execution operation. Executes associated function on specified thread from global group and returns value.
     * <p>
     * Wrapper for {@code asyncAt(int, AsyncTask<T>)}.
     * It is the equivalent to call:
     *
     * <blockquote>{@code PCJ.<T>asyncAt(threadId, asyncTask).get();}</blockquote>
     *
     * @param <T>       type of returned value
     * @param threadId  peer PCJ Thread
     * @param asyncTask function to be executed
     * @return {@link org.pcj.PcjFuture} that will contain value returned by the
     * function
     */
    public static <T> T at(int threadId, AsyncTask<T> asyncTask) throws PcjRuntimeException {
        return PCJ.asyncAt(threadId, asyncTask).get();
    }

    /**
     * Asynchronous execution operation. Executes associated function on specified thread from global group without returing value.
     *
     * @param threadId  peer PCJ Thread
     * @param asyncTask function to be executed
     * @return {@link org.pcj.PcjFuture} that indicates finish execution of execution
     */
    public static PcjFuture<Void> asyncAt(int threadId, AsyncTask.VoidTask asyncTask) throws PcjRuntimeException {
        return getGlobalGroup().asyncAt(threadId, asyncTask);
    }

    /**
     * Asynchronous execution operation. Executes associated function on specified thread from global group without returing value.
     * <p>
     * Wrapper for {@code asyncAt(int, AsyncTask.VoidTask)}.
     * It is the equivalent to call:
     *
     * <blockquote>{@code PCJ.<T>asyncAt(threadId, asyncTask).get();}</blockquote>
     *
     * @param threadId  peer PCJ Thread
     * @param asyncTask function to be executed
     */
    public static void at(int threadId, AsyncTask.VoidTask asyncTask) throws PcjRuntimeException {
        PCJ.asyncAt(threadId, asyncTask).get();
    }

    /**
     * Joins current PCJ Thread to the group.
     * <p>
     * If current PCJ Thread is already in the group, returns the group.
     *
     * @param name name of the group
     * @return joined group
     */
    public static Group joinGroup(String name) {
        int myThreadId = getGlobalGroup().myId();
        return InternalGroup.joinGroup(myThreadId, name);
    }

    /**
     * This function will be removed.
     *
     * @param name name of the group
     * @return joined group
     * @deprecated use {@link #joinGroup(String)} instead
     */
    @Deprecated(forRemoval = true)
    public static Group join(String name) {
        return PCJ.joinGroup(name);
    }

    /**
     * This function will be removed.
     *
     * Starts PCJ calculations on local node using specified StartPoint.
     * NodesDescription contains list of all hostnames used in calculations.
     * Hostnames can be specified many times, so more than one instance of PCJ
     * will be run on node (called threads).
     *
     * @param startPoint start point class
     * @param nodesDescription description of used nodes (and threads)
     * @deprecated use {@link #executionBuilder(Class)} instead
     */
    @Deprecated(forRemoval = true)
    public static void start(Class<? extends StartPoint> startPoint,
                             NodesDescription nodesDescription) {
        PCJ.executionBuilder(startPoint).addNodes(nodesDescription.getNodes()).start();
    }

    /**
     * This function will be removed.
     *
     * Deploys and starts PCJ calculations on nodes using specified StartPoint
     * class. NodesDescription contains list of all hostnames used in
     * calculations. Hostnames can be specified many times, so more than one
     * instance of PCJ will be run on node (called threads). Empty hostnames
     * means current JVM.
     *
     * Hostnames can take port (after colon ':'), eg. ["localhost:8000",
     * "localhost:8001", "localhost", "host2:8001", "host2"]. Default port is
     * 8091 and can be modified using
     * <tt>pcj.port</tt> system property value (-Dpcj.port=8091).
     *
     * @param startPoint start point class
     * @param nodesDescription description of used nodes (and threads)
     * @deprecated use {@link #executionBuilder(Class)} instead
     */
    @Deprecated(forRemoval = true)
    public static void deploy(Class<? extends StartPoint> startPoint,
                              NodesDescription nodesDescription) {
        PCJ.executionBuilder(startPoint).addNodes(nodesDescription.getNodes()).deploy();
    }
}
