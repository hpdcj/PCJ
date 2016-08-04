/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj.internal.futures;

import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.pcj.PcjFuture;
import org.pcj.PcjRuntimeException;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.MessageValueBroadcastInform;
import org.pcj.internal.message.MessageValueBroadcastResponse;

/**
 *
 * @author faramir
 */
public class BroadcastState extends InternalFuture<Void> implements PcjFuture<Void> {

    private final int groupId;
    private final int requestNum;
    private final int requesterThreadId;
    private final Set<Integer> childrenSet;
    private final Queue<Exception> exceptions;

    private Exception exception;

    public BroadcastState(int groupId, int requestNum, int requesterThreadId, List<Integer> childrenNodes) {
        this.groupId = groupId;
        this.requestNum = requestNum;
        this.requesterThreadId = requesterThreadId;

        this.childrenSet = new HashSet<>(childrenNodes.size() + 1, 1.0f);
        childrenSet.add(InternalPCJ.getNodeData().getPhysicalId());
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

    public synchronized void processPhysical(int physicalId) {
        childrenSet.remove(physicalId);

        if (childrenSet.isEmpty()) {
            NodeData nodeData = InternalPCJ.getNodeData();
            InternalCommonGroup commonGroup = nodeData.getGroupById(groupId);

            SocketChannel socket;
            Message message;

            if (nodeData.getPhysicalId() == commonGroup.getGroupMasterNode()) {
                int globalThreadId = commonGroup.getGlobalThreadId(requesterThreadId);
                int requesterPhysicalId = nodeData.getPhysicalId(globalThreadId);
                socket = InternalPCJ.getNodeData().getSocketChannelByPhysicalId().get(requesterPhysicalId);

                message = new MessageValueBroadcastResponse(groupId, requestNum, requesterThreadId, exceptions);
            } else {
                int parentId = commonGroup.getParentNode();
                socket = nodeData.getSocketChannelByPhysicalId().get(parentId);

                message = new MessageValueBroadcastInform(groupId, requestNum, requesterThreadId,
                        nodeData.getPhysicalId(), exceptions);
            }

            InternalPCJ.getNetworker().send(socket, message);
        }
    }
}
