/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.join;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.pcj.PcjFuture;
import org.pcj.PcjRuntimeException;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.futures.InternalFuture;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
class GroupQueryFuture extends InternalFuture<InternalCommonGroup> implements PcjFuture<InternalCommonGroup> {
    private InternalCommonGroup internalCommonGroup;
    private PcjRuntimeException exception;

    GroupQueryFuture() {
    }

    @Override
    public boolean isDone() {
        return super.isSignaled();
    }

    protected void signalDone(InternalCommonGroup internalCommonGroup) {
        this.internalCommonGroup = internalCommonGroup;
        super.signal();
    }

    @Override
    public InternalCommonGroup get() throws PcjRuntimeException {
        try {
            super.await();
        } catch (InterruptedException ex) {
            throw new PcjRuntimeException(ex);
        }
        if (exception != null) {
            throw exception;
        }
        return internalCommonGroup;
    }

    @Override
    public InternalCommonGroup get(long timeout, TimeUnit unit) throws TimeoutException, PcjRuntimeException {
        try {
            super.await(timeout, unit);
        } catch (InterruptedException ex) {
            throw new PcjRuntimeException(ex);
        }
        if (exception != null) {
            throw exception;
        }
        return internalCommonGroup;
    }
}
