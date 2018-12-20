/* 
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.futures;

import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.pcj.PcjFuture;
import org.pcj.PcjRuntimeException;
import org.pcj.internal.Bitmask;

/**
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class BroadcastState extends InternalFuture<Void> implements PcjFuture<Void> {

    private final Queue<Exception> exceptions;
    private final Set<Integer> childrenSet;
    private Exception exception;

    public BroadcastState(List<Integer> childrenNodes) {
        this.childrenSet = ConcurrentHashMap.newKeySet(childrenNodes.size());
        childrenNodes.forEach(childrenSet::add);

        this.exceptions = new ConcurrentLinkedDeque<>();
    }

    public void addException(Exception ex) {
        exceptions.add(ex);
    }

    public void signalException(Exception exception) {
        this.exception = exception;
        super.signalDone();
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
        if (exception != null) {
            throw new PcjRuntimeException(exception);
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
        if (exception != null) {
            throw new PcjRuntimeException(exception);
        }
        return null;
    }

    private void setPhysical(int physicalId) {
        childrenSet.remove(physicalId);
    }

    private boolean isPhysicalSet() {
        return childrenSet.isEmpty();
    }

    public boolean processPhysical(int physicalId) {
        this.setPhysical(physicalId);
        return isPhysicalSet();
    }

    public boolean isExceptionOccurs() {
        return !exceptions.isEmpty();
    }

    public Queue<Exception> getExceptions() {
        return exceptions;
    }
}
