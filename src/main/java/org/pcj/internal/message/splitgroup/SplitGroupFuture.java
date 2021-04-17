/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.splitgroup;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.pcj.Group;
import org.pcj.PcjFuture;
import org.pcj.PcjRuntimeException;
import org.pcj.internal.InternalFuture;

/*
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class SplitGroupFuture extends InternalFuture<Group> implements PcjFuture<Group> {

    private Group group;

    SplitGroupFuture() {
    }

    protected void signalDone(Group group) {
        this.group = group;
        super.signal();
    }

    @Override
    public boolean isDone() {
        return super.isSignaled();
    }

    @Override
    public Group get() throws PcjRuntimeException {
        try {
            super.await();
        } catch (InterruptedException ex) {
            throw new PcjRuntimeException(ex);
        }
        return group;
    }

    @Override
    public Group get(long timeout, TimeUnit unit) throws TimeoutException, PcjRuntimeException {
        try {
            super.await(timeout, unit);
        } catch (InterruptedException ex) {
            throw new PcjRuntimeException(ex);
        }
        return group;
    }
}
