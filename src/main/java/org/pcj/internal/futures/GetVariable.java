/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj.internal.futures;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.pcj.PcjFuture;
import org.pcj.PcjRuntimeException;

/**
 *
 * @author faramir
 */
public class GetVariable<T> extends InternalFuture<T> implements PcjFuture<T> {

    private T variableValue;
    private Exception exception;

    @SuppressWarnings("unchecked")
    public void setVariableValue(Object variableValue) {
        this.variableValue = (T) variableValue;
        super.signalAll();
    }

    public void setException(Exception exception) {
        this.exception = exception;
        super.signalAll();
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
