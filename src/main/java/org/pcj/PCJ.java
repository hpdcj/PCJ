/*
 * Copyright (c) 2011-2021, PCJ Library, Marek Nowicki
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
     * @return {@link ExecutionBuilder} for chain configuration and starting application
     */
    public static ExecutionBuilder executionBuilder(Class<? extends StartPoint> startPoint) {
        return new ExecutionBuilder(startPoint);
    }

    /**
     * Gets unique node identifier.
     * <p>
     * Node identifiers are consecutive numbers that starts from 0.
     *
     * @return node identifier
     */
    public static int getNodeId() {
        return InternalPCJ.getNodeData().getCurrentNodePhysicalId();
    }

    /**
     * Gets total number of nodes.
     *
     * @return total number of nodes
     */
    public static int getNodeCount() {
        return InternalPCJ.getNodeData().getTotalNodeCount();
    }

    /**
     * Gets all PCJ properties.
     * <p>
     * Properties can be set using {@link ExecutionBuilder#addProperty(String, String)}
     * or {@link ExecutionBuilder#addProperties(Properties)} methods.
     *
     * @return {@link Properties} object with the PCJ properties
     */
    public static Properties getProperties() {
        return InternalPCJ.getConfiguration().getProperties();
    }

    /**
     * Gets the PCJ property indicated by the specified key.
     *
     * @param key the name of the PCJ property
     * @return the string value of the PCJ property, or null if there is no property with that key
     */
    public static String getProperty(String key) {
        return getProperties().getProperty(key);
    }

    /**
     * Gets the PCJ property indicated by the specified key.
     * <p>
     * The method returns the default value argument if the key is not found.
     *
     * @param key          the name of the PCJ property
     * @param defaultValue a default value
     * @return the string value of the PCJ property, or defaultValue if there is no property with that key
     */
    public static String getProperty(String key, String defaultValue) {
        return getProperties().getProperty(key, defaultValue);
    }

    /**
     * Gets identifier of current PCJ Thread in the global group.
     * <p>
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
     * Static methods of this class and methods of the global group do the same things.
     * Static methods are wrappers for methods in global group.
     *
     * @return global group
     */
    public static Group getGlobalGroup() {
        return PcjThread.getCurrentThreadData().getGlobalGroup();
    }

    /**
     * Register storage as enum' constants.
     * <p>
     * This method, when necessary, creates a object pointed by enum' annotation.
     * If the object is already created for the PCJ Thread, method returns already associated object.
     *
     * @param storageEnumClass Enum class that represents storage shareable variables
     * @return Object associated with Storage (type of value from enum annotation)
     * @throws PcjRuntimeException thrown when there is problem with registering provided class.
     */
    public static Object registerStorage(Class<? extends Enum<?>> storageEnumClass)
            throws PcjRuntimeException {
        return PcjThread.getCurrentThreadData().getStorages().registerStorage(storageEnumClass);
    }

    /**
     * Register storage as enum' constants.
     * <p>
     * This method associates provided object as storage of shareable variable.
     * If the object is already created for the PCJ Thread, method returns already associated object.
     * <p>
     * This method uses provided object as storage of shareable variables.
     * The object has to be the same type as the type pointed by enum' annotation.
     * <p>
     * If the {@code storageObject} is {@code null}, it works like
     * {@link #registerStorage(java.lang.Class)} method.
     *
     * @param storageEnumClass Enum class that represents storage shareable
     *                         variables
     * @param storageObject    object that stores the shareable variables
     * @return Object associated with Storage (type of value from enum annotation)
     * @throws PcjRuntimeException thrown when there is problem with registering provided class.
     */
    public static Object registerStorage(Class<? extends Enum<?>> storageEnumClass, Object storageObject)
            throws PcjRuntimeException {
        return PcjThread.getCurrentThreadData().getStorages().registerStorage(storageEnumClass, storageObject);
    }

    /**
     * Get object associated with registered storage.
     *
     * @param storageEnumClass Enum class that represents storage shareable variables
     * @return Object associated with Storage (type of value from enum annotation)
     */
    public static Object getStorageObject(Class<? extends Enum<?>> storageEnumClass) {
        return PcjThread.getCurrentThreadData().getStorages().getStorage(storageEnumClass);
    }

    /**
     * Starts asynchronous barrier. After starting barrier the {@link PcjFuture} is returned.
     * <p>
     * PCJ Thread can continue to work and use {@link PcjFuture#isDone()} method to check if every thread arrive at the barrier.
     * <p>
     * {@link PcjFuture} returns null when completed.
     *
     * @return {@link PcjFuture}  to check barrier state
     */
    public static PcjFuture<Void> asyncBarrier() {
        return getGlobalGroup().asyncBarrier();
    }

    /**
     * Synchronous barrier.
     * <p>
     * Wrapper for {@link #asyncBarrier()}.
     * <p>
     * It is the equivalent to call:
     * <blockquote>{@code PCJ.asyncBarrier().get();}</blockquote>
     */
    public static void barrier() {
        PCJ.asyncBarrier().get();
    }

    /**
     * Starts asynchronous barrier with one peer PCJ Thread.
     * <p>
     * Given {@code threadId} should be different from current PCJ Thread id, otherwise the exception will be thrown.
     * <p>
     * PCJ Thread can continue to work and use {@link PcjFuture#isDone()} method to check if peer thread arrive at the barrier.
     * <p>
     * {@link PcjFuture} returns null when completed.
     *
     * @param threadId global PCJ Thread id
     * @return {@link PcjFuture} to check barrier state
     */
    public static PcjFuture<Void> asyncBarrier(int threadId) {
        return getGlobalGroup().asyncBarrier(threadId);
    }

    /**
     * Synchronous barrier with one peer PCJ Thread.
     * <p>
     * Wrapper for {@link #asyncBarrier(int)}.
     * <p>
     * It is the equivalent to call:
     * <blockquote>{@code PCJ.asyncBarrier(threadId).get();}</blockquote>
     *
     * @param threadId global PCJ Thread id
     */
    public static void barrier(int threadId) {
        PCJ.asyncBarrier(threadId).get();
    }

    /**
     * Clear modification count of the shareable variable.
     *
     * @param variable shareable variable
     * @return modification count before clearing
     */
    public static int monitor(Enum<?> variable) {
        return PcjThread.getCurrentThreadData().getStorages().monitor(variable);
    }

    /**
     * Checks and optionally waits for one modification of the shareable variable.
     * Decrease number of modification count by one.
     *
     * @param variable shareable variable
     * @return remaining modification count
     */
    public static int waitFor(Enum<?> variable) {
        return waitFor(variable, 1);
    }

    /**
     * Checks and optionally waits for many ({@code count}) modifications of the shareable variable.
     * <p>
     * Decrease number of modification count.
     *
     * @param variable shareable variable
     * @param count    number of modifications
     * @return remaining modification count
     */
    public static int waitFor(Enum<?> variable, int count) {
        return PcjThread.getCurrentThreadData().getStorages().waitFor(variable, count);
    }

    /**
     * Checks and optionally waits for many ({@code count}) modifications of the shareable variable.
     * <p>
     * Decrease number of modification count.
     *
     * @param variable shareable variable
     * @param count    number of modifications
     * @param timeout  timeout
     * @param unit     unit of time
     * @return remaining modification count
     * @throws TimeoutException when not so much modifications occurs till timeout
     */
    public static int waitFor(Enum<?> variable, int count,
                              long timeout, TimeUnit unit) throws TimeoutException {
        return PcjThread.getCurrentThreadData().getStorages().waitFor(variable, count, timeout, unit);
    }

    /**
     * Gets reference to shareable variable of current PCJ Thread.
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
     * Puts value to shareable variable of current PCJ Thread.
     * <p>
     * Upon successful completion increases modification count of the shareable variable by one.
     *
     * @param <T>      type of variable
     * @param newValue value (reference)
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @throws PcjRuntimeException contains wrapped exception (eg. ArrayOutOfBoundException).
     */
    public static <T> void putLocal(T newValue, Enum<?> variable, int... indices) throws PcjRuntimeException {
        try {
            PcjThread.getCurrentThreadData().getStorages().put(newValue, variable, indices);
        } catch (Exception ex) {
            throw new PcjRuntimeException(ex);
        }
    }

    /**
     * Accumulates value to shareable variable of current PCJ Thread.
     * <p>
     * This function increases modification count for shareable variable.
     *
     * @param <T>      type of variable
     * @param function reduce function
     * @param newValue value (reference)
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @throws PcjRuntimeException contains wrapped exception (eg. ArrayOutOfBoundException).
     */
    public static <T> void accumulateLocal(ReduceOperation<T> function, T newValue, Enum<?> variable, int... indices) throws PcjRuntimeException {
        try {
            PcjThread.getCurrentThreadData().getStorages().accumulate(function, newValue, variable, indices);
        } catch (Exception ex) {
            throw new PcjRuntimeException(ex);
        }
    }

    /**
     * Asynchronous get operation.
     * <p>
     * Gets value of shareable variable from PCJ Thread from the global group.
     *
     * @param <T>      type of value
     * @param threadId global PCJ Thread id
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @return {@link PcjFuture} that will contain shareable variable value
     */
    public static <T> PcjFuture<T> asyncGet(int threadId, Enum<?> variable, int... indices) {
        return getGlobalGroup().asyncGet(threadId, variable, indices);
    }

    /**
     * Synchronous get operation.
     * <p>
     * Wrapper for {@link #asyncGet(int, Enum, int...)}.
     * <p>
     * It is the equivalent to call:
     * <blockquote>{@code PCJ.<T>asyncGet(threadId, variable, indices).get();}</blockquote>
     *
     * @param <T>      type of value
     * @param threadId global PCJ Thread id
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @return value of variable
     * @throws PcjRuntimeException contains wrapped exception (eg. ArrayOutOfBoundException).
     */
    public static <T> T get(int threadId, Enum<?> variable, int... indices) throws PcjRuntimeException {
        return PCJ.<T>asyncGet(threadId, variable, indices).get();
    }

    /**
     * Asynchronous collect operation.
     * <p>
     * Gets value of shareable variable from all PCJ Threads from the global group.
     *
     * @param <T>      type of value
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @return {@link org.pcj.PcjFuture} that will contain shareable variable values in form of array
     */
    public static <T> PcjFuture<T> asyncCollect(Enum<?> variable, int... indices) throws PcjRuntimeException {
        return getGlobalGroup().asyncCollect(variable, indices);
    }

    /**
     * Synchronous collect operation.
     * <p>
     * Wrapper for {@link #asyncCollect(Enum, int...)}.
     * <p>
     * It is the equivalent to call:
     * <blockquote>{@code PCJ.<T>asyncCollect(variable, indices).get();}</blockquote>
     *
     * @param <T>      type of value
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @return shareable variable values in form of array
     */
    public static <T> T collect(Enum<?> variable, int... indices) throws PcjRuntimeException {
        return PCJ.<T>asyncCollect(variable, indices).get();
    }

    /**
     * Asynchronous reduce operation.
     * <p>
     * Reduces value of shareable variable from all PCJ Threads from the global group.
     *
     * @param <T>      type of value
     * @param function reduce function
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @return {@link org.pcj.PcjFuture} that will contain reduced shareable variable value
     */
    public static <T> PcjFuture<T> asyncReduce(ReduceOperation<T> function, Enum<?> variable, int... indices) {
        return getGlobalGroup().asyncReduce(function, variable, indices);
    }

    /**
     * Synchronous reduce operation.
     * <p>
     * Wrapper for {@link #asyncReduce(ReduceOperation, Enum, int...)}.
     * <p>
     * It is the equivalent to call:
     * <blockquote>{@code PCJ.<T>asyncReduce(function, variable, indices).get();}</blockquote>
     *
     * @param <T>      type of value
     * @param function reduce function
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @return {@link org.pcj.PcjFuture} that will contain reduced shareable variable value
     */
    public static <T> T reduce(ReduceOperation<T> function, Enum<?> variable, int... indices) {
        return PCJ.asyncReduce(function, variable, indices).get();
    }

    /**
     * Asynchronous put operation.
     * <p>
     * Puts value into shareable variable to PCJ Thread from the global group.
     * Upon successful completion increases modification count of the shareable variable by one.
     *
     * @param <T>      type of value
     * @param newValue new variable value
     * @param threadId global PCJ Thread id
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @return {@link org.pcj.PcjFuture}&lt;{@link java.lang.Void}&gt; for checking the operation state
     */
    public static <T> PcjFuture<Void> asyncPut(T newValue, int threadId, Enum<?> variable, int... indices) {
        return getGlobalGroup().asyncPut(newValue, threadId, variable, indices);
    }

    /**
     * Synchronous put operation.
     * <p>
     * Wrapper for {@link #asyncPut(Object, int, Enum, int...)}.
     * <p>
     * It is the equivalent to call:
     * <blockquote>{@code PCJ.<T>asyncPut(threadId, variable, newValue, indices).get();}</blockquote>
     *
     * @param <T>      type of value
     * @param newValue new variable value
     * @param threadId global PCJ Thread id
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @throws PcjRuntimeException contains wrapped exception (eg. ArrayOutOfBoundException).
     */
    public static <T> void put(T newValue, int threadId, Enum<?> variable, int... indices) throws PcjRuntimeException {
        PCJ.asyncPut(newValue, threadId, variable, indices).get();
    }

    /**
     * Asynchronous accumulate operation.
     * <p>
     * Accumulates value into shareable variable to PCJ thread from the global group.
     * Upon successful completion increases modification count of the shareable variable by one.
     *
     * @param <T>      type of value
     * @param function reduce function
     * @param newValue new variable value
     * @param threadId global PCJ Thread id
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @return {@link org.pcj.PcjFuture}&lt;{@link java.lang.Void}&gt; for checking the operation state
     */
    public static <T> PcjFuture<Void> asyncAccumulate(ReduceOperation<T> function, T newValue, int threadId, Enum<?> variable, int... indices) {
        return getGlobalGroup().asyncAccumulate(function, newValue, threadId, variable, indices);
    }

    /**
     * Synchronous accumulate operation.
     * <p>
     * Wrapper for {@link #asyncAccumulate(ReduceOperation, Object, int, Enum, int...)}.
     * <p>
     * It is the equivalent to call:
     * <blockquote>{@code PCJ.<T>asyncAccumulate(function, newValue, threadId, variable, indices).get();}</blockquote>
     *
     * @param <T>      type of value
     * @param function reduce function
     * @param newValue new variable value
     * @param threadId global PCJ Thread id
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @throws PcjRuntimeException contains wrapped exception (eg. ArrayOutOfBoundException).
     */
    public static <T> void accumulate(ReduceOperation<T> function, T newValue, int threadId, Enum<?> variable, int... indices) throws PcjRuntimeException {
        PCJ.asyncAccumulate(function, newValue, threadId, variable, indices).get();
    }

    /**
     * Asynchronous broadcast operation.
     * <p>
     * Broadcasts value into shareable variable of all PCJ Threads from the global group.
     * Upon successful completion increases modification count of the shareable variable by one.
     *
     * @param <T>      type of value
     * @param newValue new variable value
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @return {@link org.pcj.PcjFuture}&lt;{@link java.lang.Void}&gt; for checking the operation state
     */
    public static <T> PcjFuture<Void> asyncBroadcast(T newValue, Enum<?> variable, int... indices) {
        return getGlobalGroup().asyncBroadcast(newValue, variable, indices);
    }

    /**
     * Synchronous broadcast operation.
     * <p>
     * Wrapper for {@link #asyncBroadcast(Object, Enum, int...)}.
     * <p>
     * It is the equivalent to call:
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


    private static <T> PcjFuture<Void> asyncScatter(T newValueArray, Enum<?> variable, int... indices) {
        return getGlobalGroup().asyncScatter(newValueArray, variable, indices);
    }

    public static <T> void scatter(T newValueArray, Enum<?> variable, int... indices) {
        PCJ.asyncScatter(newValueArray, variable, indices).get();
    }

    /**
     * Asynchronous execution operation.
     * <p>
     * Executes associated function on specified PCJ Thread from global group and returns value.
     *
     * @param <T>       type of returned value
     * @param threadId  global PCJ Thread id
     * @param asyncTask function to be executed
     * @return {@link PcjFuture} that will contain value returned by the function
     */
    public static <T> PcjFuture<T> asyncAt(int threadId, AsyncTask<T> asyncTask) throws PcjRuntimeException {
        return getGlobalGroup().asyncAt(threadId, asyncTask);
    }

    /**
     * Synchronous execution operation.
     * <p>
     * Executes associated function on specified thread from global group and returns value.
     * <p>
     * Wrapper for {@link #asyncAt(int, AsyncTask)}.
     * <p>
     * It is the equivalent to call:
     * <blockquote>{@code PCJ.<T>asyncAt(threadId, asyncTask).get();}</blockquote>
     *
     * @param <T>       type of returned value
     * @param threadId  global PCJ Thread id
     * @param asyncTask function to be executed
     * @return {@link org.pcj.PcjFuture} that will contain value returned by the function
     */
    public static <T> T at(int threadId, AsyncTask<T> asyncTask) throws PcjRuntimeException {
        return PCJ.asyncAt(threadId, asyncTask).get();
    }

    /**
     * Asynchronous execution operation.
     * <p>
     * Executes associated function on specified PCJ Thread from global group without returning value.
     *
     * @param threadId  global PCJ Thread id
     * @param asyncTask function to be executed
     * @return {@link org.pcj.PcjFuture} that indicates finish execution of execution
     */
    public static PcjFuture<Void> asyncAt(int threadId, AsyncTask.VoidTask asyncTask) throws PcjRuntimeException {
        return getGlobalGroup().asyncAt(threadId, asyncTask);
    }

    /**
     * Synchronous execution operation.
     * <p>
     * Executes associated function on specified thread from global group without returning value.
     * <p>
     * Wrapper for {@link #asyncAt(int, AsyncTask.VoidTask)}.
     * <p>
     * It is the equivalent to call:
     * <blockquote>{@code PCJ.<T>asyncAt(threadId, asyncTask).get();}</blockquote>
     *
     * @param threadId  global PCJ Thread id
     * @param asyncTask function to be executed
     */
    public static void at(int threadId, AsyncTask.VoidTask asyncTask) throws PcjRuntimeException {
        PCJ.asyncAt(threadId, asyncTask).get();
    }

    /**
     * Asynchronous collective split group operation. This method has to be invoked by all threads in a group.
     * <p>
     * Splits global group into subgroups based on the split and ordering parameters.
     * <p>
     * Split parameter can be {@code null} which means the thread would not be included in any of new group.
     * Threads with the same split parameter value are in the same new group.
     * <p>
     * Ordering determines the PCJ Thread id in new group.
     * The smaller number of ordering means smaller PCJ Thread id in subgroup.
     * When multiple PCJ Threads gives the same ordering value,
     * the original group PCJ Thread id will be used to break a tie.
     *
     * @param split    control of subgroup assignment
     *                 or {@code null} if the thread would not be included in any of new group
     * @param ordering control of PCJ Thread id assignment
     * @return {@link org.pcj.PcjFuture} that will contains {@link org.pcj.Group} of subgroup
     * or {@code null} if the thread would not be included in any of new group
     */
    public static PcjFuture<Group> asyncSplitGroup(Integer split, int ordering) {
        return getGlobalGroup().asyncSplitGroup(split, ordering);
    }

    /**
     * Synchronous collective split group operation. This method has to be invoked by all threads in a group.
     * <p>
     * Splits global group into subgroups based on the split and ordering parameters.
     * <p>
     * Wrapper for {@link #asyncSplitGroup(Integer, int)}.
     * <p>
     * It is the equivalent to call:
     * <blockquote>{@code PCJ.asyncSplitGroup(split, ordering).get();}</blockquote>
     *
     * @param split    control of subgroup assignment
     *                 or {@code null} if the thread would not be included in any of new group
     * @param ordering control of PCJ Thread id assignment
     * @return {@link org.pcj.Group} of subgroup or {@code null} if the thread would not be included in any of new group
     */
    public static Group splitGroup(Integer split, int ordering) {
        return PCJ.asyncSplitGroup(split, ordering).get();
    }

    /**
     * This function will be removed.
     * <p>
     * This operation is no longer supported.
     * Throws {@link UnsupportedOperationException}
     * <p>
     * Use {@link #splitGroup(Integer, int)} instead.
     *
     * @deprecated use {@link #splitGroup(Integer, int)} instead
     */
    @Deprecated
    public static Group joinGroup(String name) {
        throw new UnsupportedOperationException("Use splitGroup(Integer, int");
    }

    /**
     * This function will be removed.
     * <p>
     * This operation is no longer supported.
     * Throws {@link UnsupportedOperationException}
     * <p>
     * Use {@link #splitGroup(Integer, int)} instead.
     *
     * @deprecated use {@link #splitGroup(Integer, int)} instead
     */
    @Deprecated
    public static Group join(String name) {
        throw new UnsupportedOperationException("Use splitGroup(Integer, int");
    }

    /**
     * This function will be removed.
     * <p>
     * Starts PCJ calculations on local node using specified {@link StartPoint}.
     * {@link NodesDescription} contains list of all hostnames used in calculations.
     * Hostnames can be specified many times, so more than one PCJ Thread will be run on node.
     *
     * @param startPoint       start point class
     * @param nodesDescription description of used nodes (and threads)
     * @deprecated use {@link #executionBuilder(Class)} instead
     */
    @Deprecated
    public static void start(Class<? extends StartPoint> startPoint,
                             NodesDescription nodesDescription) {
        PCJ.executionBuilder(startPoint).addNodes(nodesDescription.getNodes()).start();
    }

    /**
     * This function will be removed.
     * <p>
     * Deploys and starts PCJ calculations on nodes using specified {@link StartPoint} class.
     * {@link NodesDescription} contains list of all hostnames used in calculations.
     * Hostnames can be specified many times, so more than one PCJ Thread will be run on node.
     * Empty hostnames means current JVM.
     * <p>
     * Line with hostname can have part with port number (after colon ':'), eg.
     * ["localhost:8000", "localhost:8001", "localhost", "host2:8001", "host2"].
     * Default port is 8091 and can be modified using {@systemProperty pcj.port} system property value.
     *
     * @param startPoint       start point class
     * @param nodesDescription description of used nodes (and threads)
     * @deprecated use {@link #executionBuilder(Class)} instead
     */
    @Deprecated
    public static void deploy(Class<? extends StartPoint> startPoint,
                              NodesDescription nodesDescription) {
        PCJ.executionBuilder(startPoint).addNodes(nodesDescription.getNodes()).deploy();
    }
}
