/* 
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal;

import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.pcj.Group;
import org.pcj.PcjFuture;
import org.pcj.internal.futures.BroadcastState;
import org.pcj.internal.futures.GetVariable;
import org.pcj.internal.futures.PeerBarrierState;
import org.pcj.internal.futures.PutVariable;
import org.pcj.internal.message.MessagePeerBarrier;
import org.pcj.internal.message.MessageValueBroadcastRequest;
import org.pcj.internal.message.MessageValueGetRequest;
import org.pcj.internal.message.MessageValuePutRequest;

/**
 * External class that represents group for grouped communication.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class InternalGroup extends InternalCommonGroup implements Group {

    private final int myThreadId;
    private final AtomicInteger barrierRoundCounter;
    private final AtomicInteger getVariableCounter;
    private final ConcurrentMap<Integer, GetVariable> getVariableMap;
    private final AtomicInteger putVariableCounter;
    private final ConcurrentMap<Integer, PutVariable> putVariableMap;
    private final AtomicInteger broadcastCounter;
    private final ConcurrentMap<Integer, BroadcastState> broadcastStateMap;
    private final ConcurrentMap<Integer, PeerBarrierState> peerBarrierStateMap;

    public InternalGroup(int threadId, InternalCommonGroup internalGroup) {
        super(internalGroup);

        this.myThreadId = threadId;

        barrierRoundCounter = new AtomicInteger(0);

        getVariableCounter = new AtomicInteger(0);
        getVariableMap = new ConcurrentHashMap<>();

        putVariableCounter = new AtomicInteger(0);
        putVariableMap = new ConcurrentHashMap<>();

        broadcastCounter = new AtomicInteger(0);
        broadcastStateMap = new ConcurrentHashMap<>();

        peerBarrierStateMap = new ConcurrentHashMap<>();
    }

    @Override
    public int myId() {
        return myThreadId;
    }

    @Override
    public PcjFuture<Void> asyncBarrier() {
        return super.barrier(myThreadId, barrierRoundCounter.incrementAndGet());
    }

    @Override
    public PcjFuture<Void> asyncBarrier(int threadId) {
        if (myThreadId == threadId) {
            throw new IllegalArgumentException("Given PCJ Thread Id should be different from current PCJ Thread Id.");
        }

        PeerBarrierState peerBarrierState = getPeerBarrierState(threadId);

        int globalThreadId = super.getGlobalThreadId(threadId);
        int physicalId = InternalPCJ.getNodeData().getPhysicalId(globalThreadId);
        SocketChannel socket = InternalPCJ.getNodeData().getSocketChannelByPhysicalId().get(physicalId);

        MessagePeerBarrier message = new MessagePeerBarrier(super.getGroupId(), myThreadId, threadId);
        InternalPCJ.getNetworker().send(socket, message);

        return peerBarrierState.mineBarrier();
    }

    public PeerBarrierState getPeerBarrierState(int threadId) {
        return peerBarrierStateMap.computeIfAbsent(threadId, key -> new PeerBarrierState());
    }

    @Override
    public <T> PcjFuture<T> asyncGet(int threadId, Enum<?> variable, int... indices) {
        int requestNum = getVariableCounter.incrementAndGet();
        GetVariable<T> getVariable = new GetVariable<>();
        getVariableMap.put(requestNum, getVariable);

        int globalThreadId = super.getGlobalThreadId(threadId);
        int physicalId = InternalPCJ.getNodeData().getPhysicalId(globalThreadId);
        SocketChannel socket = InternalPCJ.getNodeData().getSocketChannelByPhysicalId().get(physicalId);

        MessageValueGetRequest message
                = new MessageValueGetRequest(
                        super.getGroupId(), requestNum, myThreadId, threadId,
                        variable.getDeclaringClass().getName(), variable.name(), indices);

        InternalPCJ.getNetworker().send(socket, message);

        return getVariable;
    }

    public GetVariable removeGetVariable(int requestNum) {
        return getVariableMap.remove(requestNum);
    }

    @Override
    public <T> PcjFuture<Void> asyncPut(T newValue, int threadId, Enum<?> variable, int... indices) {
        int requestNum = putVariableCounter.incrementAndGet();
        PutVariable putVariable = new PutVariable();
        putVariableMap.put(requestNum, putVariable);

        int globalThreadId = super.getGlobalThreadId(threadId);
        int physicalId = InternalPCJ.getNodeData().getPhysicalId(globalThreadId);
        SocketChannel socket = InternalPCJ.getNodeData().getSocketChannelByPhysicalId().get(physicalId);

        MessageValuePutRequest message
                = new MessageValuePutRequest(
                        super.getGroupId(), requestNum, myThreadId, threadId,
                        variable.getDeclaringClass().getName(), variable.name(), indices, newValue);

        InternalPCJ.getNetworker().send(socket, message);

        return putVariable;
    }

    public PutVariable removePutVariable(int requestNum) {
        return putVariableMap.remove(requestNum);
    }

    @Override
    public <T> PcjFuture<Void> asyncBroadcast(T newValue, Enum<?> variable) {
        int requestNum = broadcastCounter.incrementAndGet();
        BroadcastState broadcastState = getBroadcastState(requestNum);

        int physicalMasterId = super.getGroupMasterNode();
        SocketChannel masterSocket = InternalPCJ.getNodeData().getSocketChannelByPhysicalId().get(physicalMasterId);

        MessageValueBroadcastRequest message
                = new MessageValueBroadcastRequest(super.getGroupId(), requestNum, myThreadId,
                        variable.getDeclaringClass().getName(), variable.name(), newValue);
        InternalPCJ.getNetworker().send(masterSocket, message);

        return broadcastState;
    }
    
    
    final public BroadcastState getBroadcastState(int requestNum) {
        return broadcastStateMap.computeIfAbsent(requestNum, 
                key -> new BroadcastState(super.getPhysicalBitmask()));
    }
    
    public BroadcastState removeBroadcastState(int requestNum) {
        return broadcastStateMap.remove(requestNum);
    }
}
