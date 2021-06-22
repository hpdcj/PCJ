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
     * This function will be removed.
     * <p>
     * Gets group name.
     * <p>
     * Global group name is empty string {@code ""}.
     *
     * @return name of the group
     * @deprecated for removal
     */
    @Deprecated
    default String getName() {
        return null;
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
     * Asynchronous gather operation.
     * <p>
     * Gets value of shareable variable from all PCJ Threads from the group.
     *
     * @param <T>      type of value
     * @param variable variable name
     * @param indices  (optional) indices for array variable
     * @return {@link org.pcj.PcjFuture} that will contain shareable variable values in form of array
     */
    <T> PcjFuture<Map<Integer, T>> asyncGather(Enum<?> variable, int... indices);

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
     * <p>
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
     * Asynchronous scatter operation.
     * <p>
     * Scatter value array into shareable variable of all PCJ Threads from the group.
     * The array has to be the length of the group size.
     * <p>
     * The method maps the value array index to the thread id
     * and puts a proper element of the array into adequate thread storage.
     * <p>
     * Upon successful completion increases modification count of the shareable variable by one.
     *
     * @param <T>           has to be array type
     * @param newValueArray array with new values
     * @param variable      variable name
     * @param indices       (optional) indices for array variable
     * @return {@link org.pcj.PcjFuture}&lt;{@link java.lang.Void}&gt;
     */
    <T> PcjFuture<Void> asyncScatter(Map<Integer, T> newValueArray, Enum<?> variable, int... indices);

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
     * @deprecated for removal
     */
    @Deprecated
    default String getGroupName() {
        return getName();
    }

    /**
     * Asynchronous collective split group operation. This method has to be invoked by all threads in a group.
     * <p>
     * Splits this group into subgroups based on the split and ordering parameters.
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
    PcjFuture<Group> asyncSplitGroup(Integer split, int ordering);
}
