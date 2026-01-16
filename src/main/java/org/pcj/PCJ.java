/*
 * Copyright (c) 2011-2021, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collector;
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
     * Creates an {@link ExecutionBuilder} for configuring and starting a PCJ application.
     * <p>
     * This method does not start execution immediately.
     * To start the application, invoke {@link ExecutionBuilder#deploy()}
     * or {@link ExecutionBuilder#start()} on the returned builder.
     *
     * <pre>
     * PCJ.executionBuilder(Hello.class)
     *    .addNodes(new File("nodes.txt"))
     *    .deploy();
     * </pre>
     *
     * @param startPoint class implementing {@link StartPoint}
     * @return execution builder for configuring and starting the application
     */
    public static ExecutionBuilder executionBuilder(Class<? extends StartPoint> startPoint) {
        return new ExecutionBuilder(startPoint);
    }

    /**
     * Returns the unique identifier of the current node.
     * <p>
     * Node identifiers are consecutive integers starting from {@code 0}.
     *
     * @return node identifier
     */
    public static int getNodeId() {
        return InternalPCJ.getNodeData().getCurrentNodePhysicalId();
    }

    /**
     * Returns the total number of nodes participating in the computation.
     *
     * @return total number of nodes
     */
    public static int getNodeCount() {
        return InternalPCJ.getNodeData().getTotalNodeCount();
    }

    /**
     * Returns all PCJ properties.
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
     * Returns the value of the PCJ property identified by the specified key.
     *
     * @param key the name of the PCJ property
     * @return the string value of the PCJ property, or null if there is no property with that key
     */
    public static String getProperty(String key) {
        return getProperties().getProperty(key);
    }

    /**
     * Returns the value of the PCJ property identified by the specified key.
     * <p>
     * If the property is not defined, the provided default value is returned.
     *
     * @param key          the name of the PCJ property
     * @param defaultValue a default value
     * @return the string value of the PCJ property, or defaultValue if there is no property with that key
     */
    public static String getProperty(String key, String defaultValue) {
        return getProperties().getProperty(key, defaultValue);
    }

    /**
     * Returns the identifier of the current PCJ Thread in the global group.
     * <p>
     * Thread identifiers are consecutive integers starting from {@code 0}.
     *
     * @return current PCJ Thread identifier
     */
    public static int myId() {
        return getGlobalGroup().myId();
    }

    /**
     * Returns the total number of PCJ Threads in the global group.
     *
     * @return number of PCJ Threads
     */
    public static int threadCount() {
        return getGlobalGroup().threadCount();
    }

    /**
     * Returns the global PCJ group.
     * <p>
     * Static methods of this class are thin wrappers around methods provided by the global group.
     *
     * @return global PCJ group
     */
    public static Group getGlobalGroup() {
        return PcjThread.getCurrentThreadData().getGlobalGroup();
    }

    /**
     * Registers a storage defined by an enum class.
     * <p>
     * This method, when necessary, creates a object pointed by enum' annotation.
     * If the object is already created for the PCJ Thread, method returns already associated object.
     *
     * @param storageEnumClass Enum class that represents storage shareable variables
     * @return Object associated with Storage (the type of value from enum annotation)
     * @throws PcjRuntimeException thrown when there is problem with registering provided class.
     */
    public static Object registerStorage(Class<? extends Enum<?>> storageEnumClass)
            throws PcjRuntimeException {
        return PcjThread.getCurrentThreadData().getStorages().registerStorage(storageEnumClass);
    }

    /**
     * Registers a storage defined by an enum class.
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
     * @return Object associated with Storage (the type of value from enum annotation)
     * @throws PcjRuntimeException thrown when there is problem with registering provided class.
     */
    public static Object registerStorage(Class<? extends Enum<?>> storageEnumClass, Object storageObject)
            throws PcjRuntimeException {
        return PcjThread.getCurrentThreadData().getStorages().registerStorage(storageEnumClass, storageObject);
    }

    /**
     * Returns object associated with registered storage.
     *
     * @param storageEnumClass Enum class that represents storage shareable variables
     * @return Object associated with Storage (the type of value from enum annotation)
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
     * This is a synchronous wrapper for {@link #asyncBarrier()}.
     * <p>
     * It is the equivalent to call:
     * <blockquote>{@code PCJ.asyncBarrier().get();}</blockquote>
     */
    public static void barrier() {
        PCJ.asyncBarrier().get();
    }

    /**
     * Starts asynchronous barrier with a single peer PCJ Thread.
     * <p>
     * The provided {@code threadId} must be different from the current PCJ Thread id, otherwise the exception will be thrown.
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
     * This is a synchronous wrapper for {@link #asyncBarrier(int)}.
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
     * Clears the modification counter of the specified shareable variable.
     *
     * @param variable shareable variable
     * @return modification count before clearing
     */
    public static int monitor(Enum<?> variable) {
        return PcjThread.getCurrentThreadData().getStorages().monitor(variable);
    }

    /**
     * Checks and optionally waits for at least one modification of the specified shareable variable.
     * <p>
     * Upon return, the modification counter is decreased by one.
     *
     * @param variable shareable variable
     * @return remaining modification count
     */
    public static int waitFor(Enum<?> variable) {
        return waitFor(variable, 1);
    }

    /**
     * Checks and optionally waits for at least {@code count} modifications of the specified shareable variable.
     * <p>
     * Upon return, the modification counter is decreased by one.
     *
     * @param variable shareable variable
     * @param count    number of modifications
     * @return remaining modification count
     */
    public static int waitFor(Enum<?> variable, int count) {
        return PcjThread.getCurrentThreadData().getStorages().waitFor(variable, count);
    }

    /**
     * Checks and optionally waits for at least {@code count} modifications of the specified shareable variable,
     * subject to a timeout.
     * <p>
     * Upon return, the modification counter is decreased by one.
     *
     * @param variable shareable variable
     * @param count    number of modifications
     * @param timeout  timeout
     * @param unit     unit of time
     * @return remaining modification count
     * @throws TimeoutException if the timeout expires before enough modifications occur
     */
    public static int waitFor(Enum<?> variable, int count,
                              long timeout, TimeUnit unit) throws TimeoutException {
        return PcjThread.getCurrentThreadData().getStorages().waitFor(variable, count, timeout, unit);
    }

    /**
     * This function will be removed: use {@link #localGet(Enum, int...)} instead.
     * <p>
     * Gets reference to shareable variable of current PCJ Thread.
     *
     * @param <R>      the type of variable
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @return value (reference)
     * @deprecated use {@link #localGet(Enum, int...)} instead
     */
    @Deprecated
    public static <R> R getLocal(Enum<?> variable, int... indices) {
        return localGet(variable, indices);
    }

    /**
     * Gets a reference to the specified shareable variable of the current PCJ Thread.
     *
     * @param <R>      the type of variable
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @return value (reference)
     */
    public static <R> R localGet(Enum<?> variable, int... indices) {
        return PcjThread.getCurrentThreadData().getStorages().get(variable, indices);
    }

    /**
     * This function will be removed: use {@link #localPut(Object, Enum, int...)} instead.
     * <p>
     * Puts value to shareable variable of current PCJ Thread.
     * <p>
     * Upon successful completion increases modification count of the shareable variable by one.
     *
     * @param <T>      the type of variable
     * @param newValue value (reference)
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @throws PcjRuntimeException contains wrapped exception (e.g. ArrayOutOfBoundException).
     * @deprecated use {@link #localPut(Object, Enum, int...)} instead
     */
    @Deprecated
    public static <T> void putLocal(T newValue, Enum<?> variable, int... indices) throws PcjRuntimeException {
        localPut(newValue, variable, indices);
    }


    /**
     * Puts the value to the specified shareable variable of the current PCJ Thread.
     * <p>
     * Upon successful completion increases modification count of the shareable variable by one.
     *
     * @param <T>      the type of variable
     * @param newValue value (reference)
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @throws PcjRuntimeException contains wrapped exception (e.g. ArrayOutOfBoundException).
     */
    public static <T> void localPut(T newValue, Enum<?> variable, int... indices) throws PcjRuntimeException {
        try {
            PcjThread.getCurrentThreadData().getStorages().put(newValue, variable, indices);
        } catch (Exception ex) {
            throw new PcjRuntimeException(ex);
        }
    }


    /**
     * This function will be removed: use {@link #localAccumulate(ReduceOperation, Object, Enum, int...)} instead.
     * <p>
     * Accumulates value to shareable variable of current PCJ Thread.
     * <p>
     * This function increases modification count for shareable variable.
     *
     * @param <T>      the type of variable
     * @param function reduce function
     * @param newValue value (reference)
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @throws PcjRuntimeException contains wrapped exception (e.g. ArrayOutOfBoundException).
     * @deprecated use {@link #localAccumulate(ReduceOperation, Object, Enum, int...)} instead
     */
    @Deprecated
    public static <T> void accumulateLocal(ReduceOperation<T> function, T newValue, Enum<?> variable, int... indices) throws PcjRuntimeException {
        localAccumulate(function, newValue, variable, indices);
    }

    /**
     * Accumulates the value into the specified shareable variable of the current PCJ Thread.
     * <p>
     * Upon successful completion increases modification count of the shareable variable by one.
     *
     * @param <T>      the type of variable
     * @param function reduce function
     * @param newValue value (reference)
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @throws PcjRuntimeException contains wrapped exception (e.g. ArrayOutOfBoundException).
     */
    public static <T> void localAccumulate(ReduceOperation<T> function, T newValue, Enum<?> variable, int... indices) throws PcjRuntimeException {
        try {
            PcjThread.getCurrentThreadData().getStorages().accumulate(function, newValue, variable, indices);
        } catch (Exception ex) {
            throw new PcjRuntimeException(ex);
        }
    }

    /**
     * Asynchronous get operation.
     * <p>
     * Gets a value of the specified shareable variable from PCJ Thread from the global group.
     *
     * @param <T>      the type of value
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
     * @param <T>      the type of value
     * @param threadId global PCJ Thread id
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @return value of variable
     * @throws PcjRuntimeException contains wrapped exception (e.g. ArrayOutOfBoundException).
     */
    public static <T> T get(int threadId, Enum<?> variable, int... indices) throws PcjRuntimeException {
        return PCJ.<T>asyncGet(threadId, variable, indices).get();
    }

    /**
     * Asynchronous gather operation.
     * <p>
     * Gets values of the specified shareable variable from all PCJ Threads in the global group.
     * <p>
     * The resulting map is indexed by PCJ Thread identifiers.
     *
     * @param <T>      the type of value
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @return {@link org.pcj.PcjFuture} that will contain shareable variable values in form of map
     */
    public static <T> PcjFuture<Map<Integer, T>> asyncGather(Enum<?> variable, int... indices) {
        return getGlobalGroup().asyncGather(variable, indices);
    }

    /**
     * Synchronous gather operation.
     * <p>
     * Wrapper for {@link #asyncGather(Enum, int...)}.
     * <p>
     * It is the equivalent to call:
     * <blockquote>{@code PCJ.<T>asyncGather(variable, indices).get();}</blockquote>
     *
     * @param <R>      the type of the result
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @return shareable variable values in form of map
     */
    public static <R> Map<Integer, R> gather(Enum<?> variable, int... indices) throws PcjRuntimeException {
        return PCJ.<R>asyncGather(variable, indices).get();
    }

    /**
     * Asynchronous reduce operation.
     * <p>
     * Reduces values of the specified shareable variable from all PCJ Threads in the global group.
     *
     * @param <R>      the type of the result
     * @param function reduce function
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @return {@link org.pcj.PcjFuture} that will contain reduced shareable variable value
     */
    public static <R> PcjFuture<R> asyncReduce(ReduceOperation<R> function, Enum<?> variable, int... indices) {
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
     * @param <R>      the type of the result
     * @param function reduce function
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @return reduced shareable variable value
     * @throws PcjRuntimeException contains wrapped exception (e.g. ArrayOutOfBoundException).
     */
    public static <R> R reduce(ReduceOperation<R> function, Enum<?> variable, int... indices) throws PcjRuntimeException {
        return PCJ.asyncReduce(function, variable, indices).get();
    }

    /**
     * Asynchronous collect operation.
     * <p>
     * Performs mutable reduction on values of the specified shareable variable from all PCJ Threads in the global group.
     *
     * @param <T>               the type of shareable variable value
     * @param <R>               the type of the result
     * @param collectorSupplier supplier of collection function
     * @param variable          variable name
     * @param indices           (optional) indices for array variable
     * @return {@link org.pcj.PcjFuture} that will contain collected shareable variable value
     */
    public static <T, R> PcjFuture<R> asyncCollect(SerializableSupplier<Collector<T, ?, R>> collectorSupplier, Enum<?> variable, int... indices) {
        return getGlobalGroup().asyncCollect(collectorSupplier, variable, indices);
    }

    /**
     * Synchronous collect operation.
     * <p>
     * Wrapper for {@link #asyncCollect(SerializableSupplier, Enum, int...)}.
     * <p>
     * It is the equivalent to call:
     * <blockquote>{@code PCJ.<T,R>asyncCollect(collectorSupplier, variable, indices).get();}</blockquote>
     *
     * @param <T>               the type of shareable variable value
     * @param <R>               the type of the result
     * @param collectorSupplier supplier of collection function
     * @param variable          variable name
     * @param indices           (optional) indices for array variable
     * @return collected shareable variable value
     */
    public static <T, R> R collect(SerializableSupplier<Collector<T, ?, R>> collectorSupplier, Enum<?> variable, int... indices) throws PcjRuntimeException {
        return PCJ.asyncCollect(collectorSupplier, variable, indices).get();
    }

    /**
     * Asynchronous put operation.
     * <p>
     * Puts the value to the specified shareable variable of a target PCJ Thread from the global group.
     * Upon successful completion increases modification count of the shareable variable by one.
     *
     * @param <T>      the type of value
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
     * <blockquote>{@code PCJ.<T>asyncPut(newValue, threadId, variable, indices).get();}</blockquote>
     *
     * @param <T>      the type of value
     * @param newValue new variable value
     * @param threadId global PCJ Thread id
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @throws PcjRuntimeException contains wrapped exception (e.g. ArrayOutOfBoundException).
     */
    public static <T> void put(T newValue, int threadId, Enum<?> variable, int... indices) throws PcjRuntimeException {
        PCJ.asyncPut(newValue, threadId, variable, indices).get();
    }

    /**
     * Asynchronous accumulate operation.
     * <p>
     * Accumulates value into the specified shareable variable of a target PCJ thread from the global group.
     * Upon successful completion increases modification count of the shareable variable by one.
     *
     * @param <T>      the type of value
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
     * @param <T>      the type of value
     * @param function reduce function
     * @param newValue new variable value
     * @param threadId global PCJ Thread id
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @throws PcjRuntimeException contains wrapped exception (e.g. ArrayOutOfBoundException).
     */
    public static <T> void accumulate(ReduceOperation<T> function, T newValue, int threadId, Enum<?> variable, int... indices) throws PcjRuntimeException {
        PCJ.asyncAccumulate(function, newValue, threadId, variable, indices).get();
    }

    /**
     * Asynchronous broadcast operation.
     * <p>
     * Broadcasts value into the specified shareable variable of all PCJ Threads from the global group.
     * Upon successful completion increases modification count of the shareable variable by one.
     *
     * @param <T>      the type of value
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
     * <blockquote>{@code PCJ.<T>asyncBroadcast(newValue, variable, indices).get();}</blockquote>
     *
     * @param <T>      the type of value
     * @param newValue new variable value
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @throws PcjRuntimeException contains wrapped exception (e.g. ArrayOutOfBoundException).
     */
    public static <T> void broadcast(T newValue, Enum<?> variable, int... indices) throws PcjRuntimeException {
        PCJ.asyncBroadcast(newValue, variable, indices).get();
    }

    /**
     * Asynchronous scatter operation.
     * <p>
     * Scatter value map into the specified shareable variable of all PCJ Threads from the group.
     * <p>
     * Values are stored only on PCJ Threads whose thread identifiers are present as keys in the value map.
     * Upon successful completion increases modification count of the shareable variable by one only on these threads.
     *
     * @param <T>         the type of the scattered variable
     * @param newValueMap map with new values
     * @param variable    variable name
     * @param indices     (optional) indices for array variable
     * @return {@link org.pcj.PcjFuture}&lt;{@link java.lang.Void}&gt;
     */
    public static <T> PcjFuture<Void> asyncScatter(Map<Integer, T> newValueMap, Enum<?> variable, int... indices) {
        return getGlobalGroup().asyncScatter(newValueMap, variable, indices);
    }

    /**
     * Synchronous scatter operation.
     * <p>
     * Wrapper for {@link #asyncScatter(Map, Enum, int...)}.
     * <p>
     * It is the equivalent to call:
     * <blockquote>{@code PCJ.<T>asyncScatter(newValueMap, variable, indices).get();}</blockquote>
     *
     * @param <T>         the type of the scattered variable
     * @param newValueMap map with new values
     * @param variable    variable name
     * @param indices     (optional) indices for array variable
     * @throws PcjRuntimeException contains wrapped exception (e.g. ArrayOutOfBoundException).
     */
    public static <T> void scatter(Map<Integer, T> newValueMap, Enum<?> variable, int... indices) throws PcjRuntimeException {
        PCJ.asyncScatter(newValueMap, variable, indices).get();
    }

    /**
     * Asynchronous execution operation.
     * <p>
     * Executes the provided function on the specified PCJ Thread in the global group and returns a value.
     *
     * @param <R>       the type of returned value
     * @param threadId  global PCJ Thread id
     * @param asyncTask function to be executed
     * @return {@link PcjFuture} that will contain value returned by the function
     */
    public static <R> PcjFuture<R> asyncAt(int threadId, AsyncTask<R> asyncTask) throws PcjRuntimeException {
        return getGlobalGroup().asyncAt(threadId, asyncTask);
    }

    /**
     * Synchronous execution operation.
     * <p>
     * Wrapper for {@link #asyncAt(int, AsyncTask)}.
     * <p>
     * It is the equivalent to call:
     * <blockquote>{@code PCJ.<R>asyncAt(threadId, asyncTask).get();}</blockquote>
     *
     * @param <R>       the type of returned value
     * @param threadId  global PCJ Thread id
     * @param asyncTask function to be executed
     * @return {@link org.pcj.PcjFuture} that will contain value returned by the function
     */
    public static <R> R at(int threadId, AsyncTask<R> asyncTask) throws PcjRuntimeException {
        return PCJ.asyncAt(threadId, asyncTask).get();
    }

    /**
     * Asynchronous execution operation.
     * <p>
     * Executes the provided function on the specified PCJ Thread in the global group without returning a value.
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
     * Asynchronous collective group-splitting operation.
     * This method must be invoked by all PCJ Threads in the group.
     * <p>
     * Splits the global group into subgroups based on the {@code split} and {@code ordering} parameters.
     * <p>
     * Threads with the same {@code split} value are placed in the same subgroup.
     * The {@code split} parameter may be {@code null}, which means that
     * the thread will not be included in any new subgroup.
     * <p>
     * The {@code ordering} parameter determines the PCJ Thread identifier within the new subgroup.
     * A smaller {@code ordering} value results in a smaller PCJ Thread identifier in the subgroup.
     * If multiple PCJ Threads provide the same {@code ordering} value,
     * the original PCJ Thread identifier in the parent group is used to break ties.
     *
     * @param split    control of subgroup assignment
     *                 or {@code null} if the thread would not be included in any of new group
     * @param ordering control of PCJ Thread id assignment
     * @return {@link org.pcj.PcjFuture} that will contain {@link org.pcj.Group} of subgroup
     * or {@code null} if the thread would not be included in any of new group
     */
    public static PcjFuture<Group> asyncSplitGroup(Integer split, int ordering) {
        return getGlobalGroup().asyncSplitGroup(split, ordering);
    }

    /**
     * Synchronous collective split group operation. This method has to be invoked by all threads in a group.
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
     * This function will be removed: use {@link #splitGroup(Integer, int)} instead.
     * <p>
     * This operation is no longer supported.
     * Throws {@link UnsupportedOperationException}
     * <p>
     * Use {@link #splitGroup(Integer, int)} instead.
     *
     * @throws UnsupportedOperationException always throws exception
     * @deprecated use {@link #splitGroup(Integer, int)} instead
     */
    @Deprecated
    public static Group joinGroup(String name) {
        throw new UnsupportedOperationException("Use splitGroup(Integer, int");
    }

    /**
     * This function will be removed: use {@link #splitGroup(Integer, int)} instead.
     * <p>
     * This operation is no longer supported.
     * Throws {@link UnsupportedOperationException}
     * <p>
     * Use {@link #splitGroup(Integer, int)} instead.
     *
     * @throws UnsupportedOperationException always throws exception
     * @deprecated use {@link #splitGroup(Integer, int)} instead
     */
    @Deprecated
    public static Group join(String name) {
        throw new UnsupportedOperationException("Use splitGroup(Integer, int");
    }

    /**
     * This function will be removed: use {@link #executionBuilder(Class)} instead.
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
     * This function will be removed: use {@link #executionBuilder(Class)} instead.
     * <p>
     * Deploys and starts PCJ calculations on nodes using specified {@link StartPoint} class.
     * {@link NodesDescription} contains list of all hostnames used in calculations.
     * Hostnames can be specified many times, so more than one PCJ Thread will be run on node.
     * Empty hostnames means current JVM.
     * <p>
     * Line with hostname can have part with port number (after colon ':'), e.g.
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
