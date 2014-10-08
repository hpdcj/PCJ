/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Internal (with common ClassLoader) FutureObject.
 * 
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class InternalFutureObject<T> implements Future<T>, ResponseAttachment {

    final private Object waitObj = new Object();
    volatile private boolean done;
    private T response;

    protected InternalFutureObject() {
        done = false;
        response = null;
    }

    /**
     * Fill this future response object with response
     *
     * @param response value of response
     */
    @Override
    @SuppressWarnings("unchecked")
    public void setObject(Object response) {
        synchronized (waitObj) {
            if (done != false) {
                throw new IllegalStateException("Response value already set.");
            }
            this.response = (T) response;
            done = true;
            waitObj.notify();
        }
    }

    /**
     * Checks whether response is set.
     *
     * @return true if response is set, otherwise false.
     */
    @Override
    public boolean isDone() {
        return done;
    }

    /**
     * Causes the current thread to wait until the response is set
     */
    private void waitFor() {
        if (done) {
            return;
        }

        synchronized (waitObj) {
            while (!done) {
                try {
                    waitObj.wait();
                } catch (InterruptedException ex) {
                    ex.printStackTrace(System.err);
                }
            }
        }
    }

    /**
     * Causes the current thread to wait until the response is set
     */
    private void waitFor(long nanos) throws TimeoutException {
        if (done) {
            return;
        }

        synchronized (waitObj) {
            long waitTo = System.nanoTime() + nanos;
            while (!done) {
                nanos = waitTo - System.nanoTime();
                if (nanos <= 0) {
                    throw new TimeoutException();
                }
                try {
                    waitObj.wait(nanos / 1000000, (int) (nanos % 1000000));
                } catch (InterruptedException ex) {
                    ex.printStackTrace(System.err);
                }
            }
        }
    }

    @Override
    public T get() {
        waitFor();
        return response;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws TimeoutException {
        waitFor(unit.toNanos(timeout));
        return response;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    /**
     * Waits if necessary and returns value.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getObject() {
        waitFor();
        return (T) response;
    }
}
