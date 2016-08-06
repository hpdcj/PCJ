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
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.pcj.PcjFuture;
import org.pcj.PcjRuntimeException;
import org.pcj.internal.Bitmask;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.MessageGroupBarrierGo;
import org.pcj.internal.message.MessageGroupBarrierWaiting;

/**
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class GroupBarrierState extends InternalFuture<Void> implements PcjFuture<Void> {

    private final int groupId;
    private final int barrierRound;
    private final Set<Integer> childrenSet;
    private final Bitmask localBarrierBitmask;
    private final Bitmask localBarrierMaskBitmask;

    public GroupBarrierState(int groupId, int barrierRound, Bitmask localBitmask, List<Integer> childrenNodes) {
        this.groupId = groupId;
        this.barrierRound = barrierRound;
        this.childrenSet = ConcurrentHashMap.newKeySet(childrenNodes.size());
        childrenNodes.forEach(childrenSet::add);

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
        childrenSet.remove(physicalId);
    }

    private boolean isPhysicalSet() {
        return childrenSet.isEmpty();
    }

    public synchronized void processPhysical(int physicalId) {
        this.setPhysical(physicalId);

        process();
    }

    public synchronized void processLocal(int threadId) {
        this.setLocal(threadId);

        process();
    }

    private synchronized void process() {
        if (this.isLocalSet() && this.isPhysicalSet()) {
            Message message;
            SocketChannel socket;
            NodeData nodeData = InternalPCJ.getNodeData();
            InternalCommonGroup group = nodeData.getGroupById(groupId);

            int physicalId = nodeData.getPhysicalId();
            if (physicalId == group.getGroupMasterNode()) {
                message = new MessageGroupBarrierGo(groupId, barrierRound);

                socket = nodeData.getSocketChannelByPhysicalId().get(physicalId);
            } else {
                int parentId = group.getParentNode();
                socket = nodeData.getSocketChannelByPhysicalId().get(parentId);

                message = new MessageGroupBarrierWaiting(groupId, barrierRound, physicalId);
            }

            InternalPCJ.getNetworker().send(socket, message);
        }
    }
}
