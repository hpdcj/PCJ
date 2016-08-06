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

    /**
     * Blocks till PcjFuture completed.
     *
     * @return returned value (or null if T is Void)
     *
     * @throws PcjRuntimeException possible exception
     */
    @Override
    public T get() throws PcjRuntimeException;

    /**
     * Blocks till PcjFuture completed or throws TimeoutException when timeout occurs.
     *
     * @param timeout time
     * @param unit    time unit
     *
     * @return returned value (or {@code null} if {@code T} is {@code Void})
     *
     * @throws TimeoutException    timeout if PcjFuture not complete before timeout
     * @throws PcjRuntimeException possible exception
     */
    @Override
    public T get(long timeout, TimeUnit unit) throws TimeoutException, PcjRuntimeException;

    /**
     * Checks if PcjFuture completed.
     *
     * @return
     */
    @Override
    public boolean isDone();

    /**
     * Checks if cancelled.
     *
     * Default implementation: always return {@code false}.
     *
     * @return false
     */
    @Override
    default public boolean isCancelled() {
        return false;
    }

    /**
     * Cancel.
     * Default implementation: never cancel.
     *
     * @param mayInterruptIfRunning not used.
     *
     * @return false
     */
    @Override
    default public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

}
