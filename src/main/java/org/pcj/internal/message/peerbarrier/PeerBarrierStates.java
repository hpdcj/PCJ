/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.peerbarrier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.pcj.PcjFuture;

/*
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class PeerBarrierStates {

    private final ConcurrentMap<Integer, State> stateMap;

    public PeerBarrierStates() {
        stateMap = new ConcurrentHashMap<>();
    }

    public State getOrCreate(int threadId) {
        return stateMap.computeIfAbsent(threadId, key -> new State());
    }

    public static class State {

        private final AtomicInteger mineBarrierCounter;
        private final AtomicInteger peerBarrierCounter;
        private final Map<Integer, PeerBarrierFuture> futures;

        private State() {
            mineBarrierCounter = new AtomicInteger(0);
            peerBarrierCounter = new AtomicInteger(0);
            futures = new ConcurrentHashMap<>();
        }

        public PcjFuture<Void> doMineBarrier() {
            return barrier(mineBarrierCounter.incrementAndGet());
        }

        public PcjFuture<Void> doPeerBarrier() {
            return barrier(peerBarrierCounter.incrementAndGet());
        }

        private PcjFuture<Void> barrier(int count) {
            PeerBarrierFuture newFuture = new PeerBarrierFuture();
            PeerBarrierFuture oldFuture = futures.putIfAbsent(count, newFuture);
            if (oldFuture != null) {
                oldFuture.signalDone();
                futures.remove(count);
                return oldFuture;
            }
            return newFuture;
        }
    }
}
