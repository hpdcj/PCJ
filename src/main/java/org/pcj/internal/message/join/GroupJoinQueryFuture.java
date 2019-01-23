package org.pcj.internal.message.join;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.pcj.PcjFuture;
import org.pcj.PcjRuntimeException;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.futures.InternalFuture;

class GroupJoinQueryFuture extends InternalFuture<InternalCommonGroup> implements PcjFuture<InternalCommonGroup> {
    private InternalCommonGroup internalCommonGroup;
    private PcjRuntimeException exception;

    GroupJoinQueryFuture() {
    }

    @Override
    public boolean isDone() {
        return super.isSignaled();
    }

    protected void signalDone(InternalCommonGroup internalCommonGroup) {
        this.internalCommonGroup = internalCommonGroup;
        super.signalDone();
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
