/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.gather;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.pcj.PcjFuture;
import org.pcj.PcjRuntimeException;
import org.pcj.internal.InternalFuture;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class GatherFuture<T> extends InternalFuture<Map<Integer, T>> implements PcjFuture<Map<Integer, T>> {

    private Map<Integer, T> valuesMap;
    private PcjRuntimeException exception;

    GatherFuture() {
    }

    @Override
    public boolean isDone() {
        return super.isSignaled();
    }

    @SuppressWarnings("unchecked")
    protected void signalDone(Map<Integer, T> valuesArray) {
        this.valuesMap = valuesArray;
        super.signal();
    }

    protected void signalException(PcjRuntimeException exception) {
        this.exception = exception;
        super.signal();
    }

    @Override
    public Map<Integer, T> get() throws PcjRuntimeException {
        try {
            super.await();
        } catch (InterruptedException ex) {
            throw new PcjRuntimeException(ex);
        }
        if (exception != null) {
            throw exception;
        }
        return valuesMap;
    }

    @Override
    public Map<Integer, T> get(long timeout, TimeUnit unit) throws TimeoutException, PcjRuntimeException {
        try {
            super.await(timeout, unit);
        } catch (InterruptedException ex) {
            throw new PcjRuntimeException(ex);
        }
        if (exception != null) {
            throw exception;
        }
        return valuesMap;
    }
}
