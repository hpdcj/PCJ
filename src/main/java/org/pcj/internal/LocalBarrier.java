/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj.internal;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author faramir
 */
public class LocalBarrier {

    private final int round;
    private final Object lock;
    private final Bitmask localBarrierBitmask;
    private final Bitmask localBarrierMaskBitmask;
    private volatile boolean done;

    public LocalBarrier(int round, Bitmask localBitmask) {
        this.round = round;
        this.lock = new Object();
        this.localBarrierBitmask = new Bitmask(localBitmask.getSize());
        this.localBarrierMaskBitmask = new Bitmask(localBitmask);

        this.done = false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LocalBarrier == false) {
            return false;
        }
        return ((LocalBarrier) obj).round == this.round;
    }

    @Override
    public int hashCode() {
        return round;
    }

    public int getRound() {
        return round;
    }

    public void set(int index) {
        localBarrierBitmask.set(index);
    }

    public boolean isSet() {
        return localBarrierBitmask.isSet(localBarrierMaskBitmask);
    }

    public void signalAll() {
        done = true;
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    public void await() throws InterruptedException {
        while (done == false) {
            synchronized (lock) {
                lock.wait();
            }
        }
    }

    public void await(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        while (done == false) {
            synchronized (lock) {
                unit.timedWait(lock, timeout);
            }
            if (done == false) {
                throw new TimeoutException("Not yet done.");
            }
        }
    }

    public boolean isDone() {
        return done;
    }
}
