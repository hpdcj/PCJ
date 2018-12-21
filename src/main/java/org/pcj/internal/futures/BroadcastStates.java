/*
 * Copyright (c) 2011-2018, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.futures;

import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.pcj.PcjRuntimeException;

public class BroadcastStates {
    private final AtomicInteger counter;
    private final ConcurrentMap<List<Integer>, State> stateMap;

    public BroadcastStates() {
        counter = new AtomicInteger(0);
        stateMap = new ConcurrentHashMap<>();
    }

    public State create(int threadId, int childrenCount) {
        int requestNum = counter.incrementAndGet();

        BroadcastFuture future = new BroadcastFuture();
        State state = new State(requestNum, threadId, childrenCount, future);

        stateMap.put(Arrays.asList(requestNum, threadId), state);

        return state;
    }

    public State getOrCreate(int requestNum, int threadId, int childrenCount) {
        return stateMap.computeIfAbsent(Arrays.asList(requestNum, threadId), key -> new State(requestNum, threadId, childrenCount));
    }

    public State remove(int requestNum, int threadId) {
        return stateMap.remove(Arrays.asList(requestNum, threadId));
    }

    public State remove(State state) {
        return stateMap.remove(Arrays.asList(state.requestNum, state.threadId));
    }

    public static class State {

        private final int requestNum;
        private final int threadId;
        private final AtomicInteger notificationCount;
        private final BroadcastFuture future;
        private final Queue<Exception> exceptions;

        private State(int requestNum, int threadId, int childrenCount, BroadcastFuture future) {
            this.requestNum = requestNum;
            this.threadId = threadId;

            this.future = future;

            // notification from children and from itself
            notificationCount = new AtomicInteger(childrenCount + 1);
            exceptions = new ConcurrentLinkedDeque<>();
        }

        private State(int requestNum, int threadId, int childrenCount) {
            this(requestNum, threadId, childrenCount, null);
        }

        public int getRequestNum() {
            return requestNum;
        }

        public BroadcastFuture getFuture() {
            return future;
        }

        public void addException(Exception ex) {
            exceptions.add(ex);
        }

        public Queue<Exception> getExceptions() {
            return exceptions;
        }

        public boolean checkProcessed() {
            return notificationCount.decrementAndGet() == 0;
        }

        public void signalDone() {
            future.signalDone();
        }

        public void signalException(PcjRuntimeException ex) {
            future.signalException(ex);
        }
    }
}
