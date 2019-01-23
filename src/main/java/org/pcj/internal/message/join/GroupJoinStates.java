/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.join;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.pcj.PcjFuture;
import org.pcj.internal.InternalGroup;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class GroupJoinStates {
    private final AtomicInteger counter;

    private final ConcurrentMap<Integer, State> stateMap;

    public GroupJoinStates() {
        counter = new AtomicInteger(0);
        stateMap = new ConcurrentHashMap<>();
    }

    public State create() {
        int requestNum = counter.incrementAndGet();

        GroupJoinFuture future = new GroupJoinFuture();
        State state = new State(requestNum, future);

        stateMap.put(requestNum, state);

        return state;
    }

    public State remove(int requestNum) {
        return stateMap.remove(requestNum);
    }

    public static class State {
        private final int requestNum;
        private final GroupJoinFuture future;

        public State(int requestNum, GroupJoinFuture future) {
            this.requestNum = requestNum;
            this.future = future;
        }

        public int getRequestNum() {
            return requestNum;
        }

        public PcjFuture<InternalGroup> getFuture() {
            return future;
        }

        public void signal(InternalGroup internalGroup) {
            future.signalDone(internalGroup);
        }
    }

}
