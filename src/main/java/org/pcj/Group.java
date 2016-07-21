/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj;

/**
 *
 * @author faramir
 */
public interface Group {

    PcjFuture<Void> asyncBarrier();

    /**
     * Fully asynchronous get operation - receives variable value from other
     * thread. If threadId is current thread, data is cloned.
     *
     * @param threadId other node group thread id
     * @param variable name of variable
     * @param indices  indices of variable array (not needed)
     *
     * @return FutureObject that will contain received object
     */
    <T> PcjFuture<T> asyncGet(int threadId, Enum<? extends Shared> variable, int... indices);

    <T> PcjFuture<Void> asyncPut(int threadId, Enum<? extends Shared> variable, T newValue, int... indices);

    int myId();
    
}
