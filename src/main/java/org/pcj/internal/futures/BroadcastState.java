/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
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
    private final Set<Exception> exceptions;

    public BroadcastState(int groupId, int requestNum, int requesterThreadId, List<Integer> childrenNodes) {
        this.groupId = groupId;
        this.requestNum = requestNum;
        this.requesterThreadId = requesterThreadId;
        
        this.childrenSet = ConcurrentHashMap.newKeySet(childrenNodes.size() + 1);
        childrenSet.add(InternalPCJ.getNodeData().getPhysicalId());
        childrenNodes.forEach(childrenSet::add);

        this.exceptions = ConcurrentHashMap.newKeySet();
    }

    @Override
    public void signalAll() {
        super.signalAll();
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
    
    public synchronized void processPhysical(int physicalId) {
                childrenSet.remove(physicalId);
                
            if (childrenSet.isEmpty()) {
                NodeData nodeData = InternalPCJ.getNodeData();
                InternalCommonGroup group = nodeData.getGroupById(groupId);
                
                if (physicalId == group.getGroupMasterNode()) {
                    int globalThreadId = group.getGlobalThreadId(requesterThreadId);
                    int masterPhysicalId = nodeData.getPhysicalIdByThreadId().get(globalThreadId);
                    SocketChannel requesterSocket = InternalPCJ.getNodeData()
                            .getSocketChannelByPhysicalId().get(masterPhysicalId);

                    MessageValueBroadcastResponse messageResponse
                            = new MessageValueBroadcastResponse(groupId, requestNum, requesterThreadId);

                    InternalPCJ.getNetworker().send(requesterSocket, messageResponse);
                } else {
                    int parentId = group.getParentNode();
                    SocketChannel parentSocket = InternalPCJ.getNodeData()
                            .getSocketChannelByPhysicalId().get(parentId);

                    MessageValueBroadcastInform messageInform
                            = new MessageValueBroadcastInform(groupId, requestNum, requesterThreadId, nodeData.getPhysicalId());

                    InternalPCJ.getNetworker().send(parentSocket, messageInform);
                }
            }
    }
}
