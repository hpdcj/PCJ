/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj.internal.futures;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import org.pcj.PcjFuture;
import org.pcj.PcjRuntimeException;

/**
 *
 * @author faramir
 */
public class PeerBarrierState {

    private final AtomicLong myBarrierCounter;
    private final AtomicLong peerBarrierCounter;
    private final Map<Long, PeerBarrierStateFuture> futures;

    public PeerBarrierState() {
        myBarrierCounter = new AtomicLong(Long.MIN_VALUE);
        peerBarrierCounter = new AtomicLong(Long.MIN_VALUE);
        futures = new ConcurrentHashMap<>();
    }

    public PcjFuture<Void> mineBarrier() {
        return barrier(myBarrierCounter.incrementAndGet());
    }

    public PcjFuture<Void> peerBarrier() {
        return barrier(peerBarrierCounter.incrementAndGet());
    }

    private PcjFuture<Void> barrier(Long count) {
        PeerBarrierStateFuture newValue = new PeerBarrierStateFuture();
        PeerBarrierStateFuture oldValue = futures.putIfAbsent(count, newValue);
        if (oldValue != null) {
            oldValue.signalDone();
            futures.remove(count);
            return oldValue;
        }
        return newValue;
    }

    private static class PeerBarrierStateFuture extends InternalFuture<Void> implements PcjFuture<Void> {

        @Override
        public void signalDone() {
            super.signalDone();
        }

        @Override
        public boolean isDone() {
            return super.isSignaled();
        }

        @Override
        public Void get() throws PcjRuntimeException {
            try {
                super.await();
            } catch (InterruptedException ex) {
                throw new PcjRuntimeException(ex);
            }
            return null;
        }

        @Override
        public Void get(long timeout, TimeUnit unit) throws TimeoutException, PcjRuntimeException {
            try {
                super.await(timeout, unit);
            } catch (InterruptedException ex) {
                throw new PcjRuntimeException(ex);
            }
            return null;
        }

    }
}
