/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author faramir
 */
public interface PcjFuture<T> extends Future<T> {

    @Override
    public T get() throws PcjRuntimeException;

    @Override
    public T get(long timeout, TimeUnit unit) throws TimeoutException, PcjRuntimeException;

    @Override
    public boolean isDone();

    @Override
    default public boolean isCancelled() {
        return false;
    }

    @Override
    default public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

}
