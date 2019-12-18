/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj;

/**
 * Class that represents group of PCJ Threads.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public interface Group {

    /**
     * Gets identifier of current PCJ Thread in the group.
     * <p>
     * Identifiers are consecutive numbers that start with 0.
     *
     * @return current PCJ Thread identifier
     */
    int myId();

    /**
     * Gets total number of PCJ Thread in the group.
     *
     * @return total number of PCJ Thread in the group
     */
    int threadCount();

    /**
     * Gets group name.
     * <p>
     * Global group name is empty string {@code ""}.
     *
     * @return name of the group
     */
    String getName();

    /**
     * Starts asynchronous barrier. After starting barrier the {@link PcjFuture} is returned.
     * <p>
     * PCJ Thread can continue to work and use {@link PcjFuture#isDone()} method to check if every thread arrive at the barrier.
     * <p>
     * {@link PcjFuture} returns null when completed.
     *
     * @return {@link PcjFuture}  to check barrier state
     */
    PcjFuture<Void> asyncBarrier();

    /**
     * Starts asynchronous barrier with one peer PCJ Thread.
     * <p>
     * Given {@code threadId} should be different from current PCJ Thread id, otherwise the exception will be thrown.
     * <p>
     * PCJ Thread can continue to work and use {@link PcjFuture#isDone()} method to check if peer thread arrive at the barrier.
     * <p>
     * {@link PcjFuture} returns null when completed.
     *
     * @param threadId current group PCJ Thread id
     * @return {@link PcjFuture} to check barrier state
     */
    PcjFuture<Void> asyncBarrier(int threadId);

    /**
     * Asynchronous get operation.
     * <p>
     * Gets value of shareable variable from PCJ Thread from the group.
     *
     * @param <T>      type of value
     * @param threadId current group PCJ Thread id
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @return {@link PcjFuture} that will contain shareable variable value
     */
    <T> PcjFuture<T> asyncGet(int threadId, Enum<?> variable, int... indices);

    /**
     * Asynchronous collect operation.
     * <p>
     * Gets value of shareable variable from all PCJ Threads from the group.
     *
     * @param <T>      type of value
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @return {@link org.pcj.PcjFuture} that will contain shareable variable values in form of array
     */
    <T> PcjFuture<T> asyncCollect(Enum<?> variable, int... indices);

    /**
     * Asynchronous reduce operation.
     * <p>
     * Reduces value of shareable variable from all PCJ Threads from the group.
     *
     * @param <T>      type of value
     * @param function reduce function
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @return {@link org.pcj.PcjFuture} that will contain reduced shareable variable value
     */
    <T> PcjFuture<T> asyncReduce(ReduceOperation<T> function, Enum<?> variable, int... indices);

    /**
     * Asynchronous put operation.
     * <p>
     * Puts value into shareable variable to PCJ Thread from the group.
     * Upon successful completion increases modification count of the shareable variable by one.
     *
     * @param <T>      type of value
     * @param newValue new variable value
     * @param threadId current group PCJ Thread id
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @return {@link org.pcj.PcjFuture} for checking the operation state
     */
    <T> PcjFuture<Void> asyncPut(T newValue, int threadId, Enum<?> variable, int... indices);

    /**
     * Asynchronous accumulate operation.
     * <p>
     * Accumulates value into shareable variable to PCJ thread from the group.
     * Upon successful completion increases modification count of the shareable variable by one.
     *
     * @param <T>      type of value
     * @param function reduce function
     * @param newValue new variable value
     * @param threadId current group PCJ Thread id
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @return {@link org.pcj.PcjFuture}
     */
    <T> PcjFuture<Void> asyncAccumulate(ReduceOperation<T> function, T newValue, int threadId, Enum<?> variable, int... indices);

    /**
     * Asynchronous broadcast operation.
     * <p>
     * Broadcasts value into shareable variable of all PCJ Threads from the group.
     * Upon successful completion increases modification count of the shareable variable by one.
     *
     * @param <T>      type of value
     * @param newValue new variable value
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @return {@link org.pcj.PcjFuture}&lt;{@link java.lang.Void}&gt;
     */
    <T> PcjFuture<Void> asyncBroadcast(T newValue, Enum<?> variable, int... indices);

    /**
     * Asynchronous execution operation.
     * <p>
     * Executes associated function on specified PCJ Thread from group and returns value.
     *
     * @param <T>       type of returned value
     * @param threadId  current group PCJ Thread id
     * @param asyncTask function to be executed
     * @return {@link PcjFuture} that will contain value returned by the function
     */
    <T> PcjFuture<T> asyncAt(int threadId, AsyncTask<T> asyncTask);

    /**
     * This function will be removed.
     *
     * @return name of the group
     * @deprecated use {@link #getName()} instead
     */
    @Deprecated
    default String getGroupName() {
        return getName();
    }
}
