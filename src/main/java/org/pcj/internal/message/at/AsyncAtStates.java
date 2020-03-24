/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.at;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.pcj.PcjFuture;
import org.pcj.PcjRuntimeException;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class AsyncAtStates {

    private final AtomicInteger counter;
    private final ConcurrentMap<Integer, State<?>> stateMap;

    public AsyncAtStates() {
        counter = new AtomicInteger(0);
        stateMap = new ConcurrentHashMap<>();
    }

    public <T> State<T> create() {
        int requestNum = counter.incrementAndGet();

        AsyncAtFuture<T> future = new AsyncAtFuture<>();
        State<T> state = new State<>(requestNum, future);

        stateMap.put(requestNum, state);

        return state;
    }

    public State<?> remove(int requestNum) {
        return stateMap.remove(requestNum);
    }

    public static class State<T> {

        private final int requestNum;
        private final AsyncAtFuture<T> future;

        private State(int requestNum, AsyncAtFuture<T> future) {
            this.requestNum = requestNum;

            this.future = future;
        }

        public int getRequestNum() {
            return requestNum;
        }

        public PcjFuture<T> getFuture() {
            return future;
        }

        public void signal(Object variableValue, Exception exception) {
            if (exception == null) {
                future.signalDone(variableValue);
            } else {
                PcjRuntimeException ex = new PcjRuntimeException("Asynchronous execution failed", exception);
                future.signalException(ex);
            }
        }
    }
}
