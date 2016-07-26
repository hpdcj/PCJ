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

    int myId();

    PcjFuture<Void> asyncBarrier();

    <T> PcjFuture<T> asyncGet(int threadId, Shared variable, int... indices);

    <T> PcjFuture<Void> asyncPut(int threadId, Shared variable, T newValue, int... indices);

    <T> PcjFuture<Void> asyncBroadcast(Shared variable, T newValue);
}
