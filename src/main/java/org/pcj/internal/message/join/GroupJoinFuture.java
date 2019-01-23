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
import org.pcj.internal.InternalGroup;
import org.pcj.internal.futures.InternalFuture;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
class GroupJoinFuture extends InternalFuture<InternalGroup> implements PcjFuture<InternalGroup> {
    private InternalGroup internalGroup;
    private PcjRuntimeException exception;

    GroupJoinFuture() {
    }

    @Override
    public boolean isDone() {
        return super.isSignaled();
    }

    protected void signalDone(InternalGroup internalGroup) {
        this.internalGroup = internalGroup;
        super.signal();
    }

    @Override
    public InternalGroup get() throws PcjRuntimeException {
        try {
            super.await();
        } catch (InterruptedException ex) {
            throw new PcjRuntimeException(ex);
        }
        if (exception != null) {
            throw exception;
        }
        return internalGroup;
    }

    @Override
    public InternalGroup get(long timeout, TimeUnit unit) throws TimeoutException, PcjRuntimeException {
        try {
            super.await(timeout, unit);
        } catch (InterruptedException ex) {
            throw new PcjRuntimeException(ex);
        }
        if (exception != null) {
            throw exception;
        }
        return internalGroup;
    }
}
