/* 
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.futures;

import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.pcj.PcjFuture;
import org.pcj.PcjRuntimeException;
import org.pcj.internal.Bitmask;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.MessageValueBroadcastResponse;

/**
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class BroadcastState extends InternalFuture<Void> implements PcjFuture<Void> {

    private final int groupId;
    private final int requestNum;
    private final int requesterThreadId;
    private final Queue<Exception> exceptions;
    private final Bitmask physicalBarrierBitmask;
    private final Bitmask physicalBarrierMaskBitmask;
    private Exception exception;

    public BroadcastState(int groupId, int requestNum, int requesterThreadId, Bitmask physicalBitmask) {
        this.groupId = groupId;
        this.requestNum = requestNum;
        this.requesterThreadId = requesterThreadId;

 physicalBarrierBitmask = new Bitmask(physicalBitmask.getSize());
        physicalBarrierMaskBitmask = new Bitmask(physicalBitmask);

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
        physicalBarrierBitmask.set(physicalId);
    }

    private boolean isPhysicalSet() {
        return physicalBarrierBitmask.isSet(physicalBarrierMaskBitmask);
    }
    
    public synchronized boolean processPhysical(int physicalId) {
        this.setPhysical(physicalId);
        
        if (isPhysicalSet()) {
            NodeData nodeData = InternalPCJ.getNodeData();
            InternalCommonGroup commonGroup = nodeData.getGroupById(groupId);

            int globalThreadId = commonGroup.getGlobalThreadId(requesterThreadId);
            int requesterPhysicalId = nodeData.getPhysicalId(globalThreadId);
            SocketChannel socket = InternalPCJ.getNodeData().getSocketChannelByPhysicalId().get(requesterPhysicalId);

            Message message = new MessageValueBroadcastResponse(groupId, requestNum, requesterThreadId, exceptions);

            InternalPCJ.getNetworker().send(socket, message);

            return true;
        }

        return false;
    }
}
