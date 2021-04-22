/*
 * Copyright (c) 2011-2021, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.get;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.pcj.PcjFuture;
import org.pcj.PcjRuntimeException;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class ValueGetStates {

    private final AtomicInteger counter;
    private final ConcurrentMap<Integer, State<?>> stateMap;

    public ValueGetStates() {
        counter = new AtomicInteger(0);
        stateMap = new ConcurrentHashMap<>();
    }

    public <T> State<T> create() {
        int requestNum = counter.incrementAndGet();

        ValueGetFuture<T> future = new ValueGetFuture<>();
        State<T> state = new State<>(requestNum, future);

        stateMap.put(requestNum, state);

        return state;
    }

    public State<?> remove(int requestNum) {
        return stateMap.remove(requestNum);
    }

    public static class State<T> {

        private final int requestNum;
        private final ValueGetFuture<T> future;

        private State(int requestNum, ValueGetFuture<T> future) {
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
                PcjRuntimeException ex = new PcjRuntimeException("Getting value failed", exception);
                future.signalException(ex);
            }
        }
    }
}
