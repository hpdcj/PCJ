/* 
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.futures;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public abstract class InternalFuture<T> {

    private final Object lock;
    private boolean signaled;

    protected InternalFuture() {
        this.lock = new Object();
        this.signaled = false;
    }

    protected boolean isSignaled() {
        synchronized (lock) {
            return signaled;
        }
    }

    protected void signalDone() {
        synchronized (lock) {
            signaled = true;
            lock.notifyAll();
        }
    }

    protected void await() throws InterruptedException {
        synchronized (lock) {
            while (signaled == false) {
                lock.wait();
            }
        }
    }

    protected void await(long timeout, TimeUnit unit) throws TimeoutException, InterruptedException {
        long nanosTimeout = unit.toNanos(timeout);
        final long deadline = System.nanoTime() + nanosTimeout;

        synchronized (lock) {
            while (signaled == false) {
                if (nanosTimeout <= 0L) {
                    throw new TimeoutException("Not done yet.");
                }
                lock.wait(nanosTimeout / 1_000_000, (int) (nanosTimeout % 1_000_000));
                nanosTimeout = deadline - System.nanoTime();
            }
        }
    }
}
