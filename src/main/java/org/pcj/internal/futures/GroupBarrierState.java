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
import org.pcj.internal.Bitmask;

/**
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class GroupBarrierState extends InternalFuture<Void> implements PcjFuture<Void> {

    private final int groupId;
    private final int barrierRound;
    private final Bitmask physicalBarrierBitmask;
    private final Bitmask physicalBarrierMaskBitmask;
    private final Bitmask localBarrierBitmask;
    private final Bitmask localBarrierMaskBitmask;

    public GroupBarrierState(int groupId, int barrierRound, Bitmask localBitmask, Bitmask physicalBitmask) {
        this.groupId = groupId;
        this.barrierRound = barrierRound;

        physicalBarrierBitmask = new Bitmask(physicalBitmask.getSize());
        physicalBarrierMaskBitmask = new Bitmask(physicalBitmask);

        this.localBarrierBitmask = new Bitmask(localBitmask.getSize());
        this.localBarrierMaskBitmask = new Bitmask(localBitmask);
    }

    @Override
    public void signalDone() {
        super.signalDone();
    }

    @Override
    public boolean isDone() {
        return super.isSignaled();
    }

    @Override
    public Void get() throws PcjRuntimeException {
        try {
            super.await();
        } catch (InterruptedException ex) {
            throw new PcjRuntimeException(ex);
        }
        return null;
    }

    @Override
    public Void get(long timeout, TimeUnit unit) throws TimeoutException, PcjRuntimeException {
        try {
            super.await(timeout, unit);
        } catch (InterruptedException ex) {
            throw new PcjRuntimeException(ex);
        }
        return null;
    }

    private void setLocal(int index) {
        localBarrierBitmask.set(index);
    }

    private boolean isLocalSet() {
        return localBarrierBitmask.isSet(localBarrierMaskBitmask);
    }

    private void setPhysical(int physicalId) {
        physicalBarrierBitmask.set(physicalId);
    }

    private boolean isPhysicalSet() {
        return physicalBarrierBitmask.isSet(physicalBarrierMaskBitmask);
    }

    public synchronized boolean processPhysical(int physicalId) {
        this.setPhysical(physicalId);
        return isPhysicalSet();
    }

    public synchronized boolean processLocal(int threadId) {
        this.setLocal(threadId);
        return isLocalSet();
    }
}
