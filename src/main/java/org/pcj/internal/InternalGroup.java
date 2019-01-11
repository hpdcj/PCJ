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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.pcj.AsyncTask;
import org.pcj.Group;
import org.pcj.PcjFuture;
import org.pcj.internal.futures.AsyncAtExecution;
import org.pcj.internal.message.broadcast.BroadcastStates;
import org.pcj.internal.futures.GetVariable;
import org.pcj.internal.futures.PeerBarrierState;
import org.pcj.internal.futures.PutVariable;
import org.pcj.internal.message.MessageAsyncAtRequest;
import org.pcj.internal.message.MessagePeerBarrier;
import org.pcj.internal.message.broadcast.BroadcastValueRequestMessage;
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
    private final AtomicInteger asyncAtExecutionCounter;
    private final ConcurrentMap<Integer, AsyncAtExecution> asyncAtExecutionMap;
    private final ConcurrentMap<Integer, PeerBarrierState> peerBarrierStateMap;

    public InternalGroup(int threadId, InternalCommonGroup internalGroup) {
        super(internalGroup);

        this.myThreadId = threadId;

        barrierRoundCounter = new AtomicInteger(0);

        getVariableCounter = new AtomicInteger(0);
        getVariableMap = new ConcurrentHashMap<>();

        putVariableCounter = new AtomicInteger(0);
        putVariableMap = new ConcurrentHashMap<>();

        asyncAtExecutionCounter = new AtomicInteger(0);
        asyncAtExecutionMap = new ConcurrentHashMap<>();

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
    public <T> PcjFuture<Void> asyncBroadcast(T newValue, Enum<?> variable, int... indices) {
        BroadcastStates states = super.getBroadcastStates();
        BroadcastStates.State state = states.create(myThreadId, getChildrenNodes().size());

        BroadcastValueRequestMessage message
                = new BroadcastValueRequestMessage(
                        super.getGroupId(), state.getRequestNum(), myThreadId,
                        variable.getDeclaringClass().getName(), variable.name(), indices, newValue);

        int physicalMasterId = super.getGroupMasterNode();
        SocketChannel masterSocket = InternalPCJ.getNodeData().getSocketChannelByPhysicalId().get(physicalMasterId);

        InternalPCJ.getNetworker().send(masterSocket, message);

        return state.getFuture();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> PcjFuture<T> asyncAt(int threadId, AsyncTask<T> asyncTask) {
        int requestNum = asyncAtExecutionCounter.incrementAndGet();
        AsyncAtExecution asyncAtExecution = new AsyncAtExecution();
        asyncAtExecutionMap.put(requestNum, asyncAtExecution);

        int globalThreadId = super.getGlobalThreadId(threadId);
        int physicalId = InternalPCJ.getNodeData().getPhysicalId(globalThreadId);
        SocketChannel socket = InternalPCJ.getNodeData().getSocketChannelByPhysicalId().get(physicalId);

        MessageAsyncAtRequest<T> message
                = new MessageAsyncAtRequest<>(super.getGroupId(), requestNum, myThreadId, threadId, asyncTask);

        InternalPCJ.getNetworker().send(socket, message);

        return asyncAtExecution;
    }

    public AsyncAtExecution removeAsyncAtExecution(int requestNum) {
        return asyncAtExecutionMap.remove(requestNum);
    }

}
