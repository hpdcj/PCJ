/* 
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.futures;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Class for synchronization.
 * <p>
 * It tries to eliminate "spurious wakeup" by synchronization
 * rounds.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class WaitObject {

    private int round = 0;
    private final ReentrantLock lock = new ReentrantLock(false);
    private final Condition condition = lock.newCondition();

    private void nextRound() {
        ++round;
    }

    /**
     * Begin of the synchronized (this) block
     */
    public void lock() {
        lock.lock();
    }

    /**
     * End of the synchronized (this) block
     */
    public void unlock() {
        lock.unlock();
    }

    /**
     * Similar to Object.wait()
     *
     * @see Object#wait()
     */
    public void await() throws InterruptedException {
        lock.lock();
        try {
            final int r = this.round;

            for (;;) {
                condition.await();

                if (r != this.round) {
                    return;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Similar to nofity()
     *
     * @see Object#notify()
     */
    public void signal() {
        lock.lock();
        try {
            condition.signal();
            nextRound();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Similar to notifyAll()
     *
     * @see Object#notifyAll()
     */
    public void signalAll() {
        lock.lock();
        try {
            condition.signalAll();
            nextRound();
        } finally {
            lock.unlock();
        }
    }
}
