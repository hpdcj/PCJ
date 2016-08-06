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
import org.pcj.PcjFuture;
import org.pcj.PcjRuntimeException;

/**
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class GetVariable<T> extends InternalFuture<T> implements PcjFuture<T> {

    private T variableValue;
    private Exception exception;

    @SuppressWarnings("unchecked")
    public void signalDone(Object variableValue) {
        this.variableValue = (T) variableValue;
        super.signalDone();
    }

    public void signalException(Exception exception) {
        this.exception = exception;
        super.signalDone();
    }

    @Override
    public boolean isDone() {
        return super.isSignaled();
    }

    @Override
    public T get() throws PcjRuntimeException {
        try {
            super.await();
        } catch (InterruptedException ex) {
            throw new PcjRuntimeException(ex);
        }
        if (exception != null) {
            throw new PcjRuntimeException(exception);
        }
        return variableValue;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws TimeoutException, PcjRuntimeException {
        try {
            super.await(timeout, unit);
        } catch (InterruptedException ex) {
            throw new PcjRuntimeException(ex);
        }
        if (exception != null) {
            throw new PcjRuntimeException(exception);
        }
        return variableValue;
    }
}
