/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal;

import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.pcj.AsyncTask;
import org.pcj.Group;
import org.pcj.PcjFuture;
import org.pcj.PcjRuntimeException;
import org.pcj.ReduceOperation;
import org.pcj.internal.message.accumulate.ValueAccumulateRequestMessage;
import org.pcj.internal.message.accumulate.ValueAccumulateStates;
import org.pcj.internal.message.at.AsyncAtRequestMessage;
import org.pcj.internal.message.at.AsyncAtStates;
import org.pcj.internal.message.barrier.BarrierStates;
import org.pcj.internal.message.broadcast.BroadcastRequestMessage;
import org.pcj.internal.message.broadcast.BroadcastStates;
import org.pcj.internal.message.collect.CollectStates;
import org.pcj.internal.message.get.ValueGetRequestMessage;
import org.pcj.internal.message.get.ValueGetStates;
import org.pcj.internal.message.join.GroupJoinRequestMessage;
import org.pcj.internal.message.join.GroupJoinStates;
import org.pcj.internal.message.join.GroupQueryMessage;
import org.pcj.internal.message.join.GroupQueryStates;
import org.pcj.internal.message.peerbarrier.PeerBarrierMessage;
import org.pcj.internal.message.peerbarrier.PeerBarrierStates;
import org.pcj.internal.message.put.ValuePutRequestMessage;
import org.pcj.internal.message.put.ValuePutStates;
import org.pcj.internal.message.reduce.ReduceRequestMessage;
import org.pcj.internal.message.reduce.ReduceStates;

/**
 * External class that represents group for grouped communication.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public final class InternalGroup extends InternalCommonGroup implements Group {

    private final int myThreadId;
    private final ValueGetStates valueGetStates;
    private final ValuePutStates valuePutStates;
    private final ValueAccumulateStates valueAccumulateStates;
    private final AsyncAtStates asyncAtStates;
    private final PeerBarrierStates peerBarrierStates;

    public InternalGroup(int threadId, InternalCommonGroup internalGroup) {
        super(internalGroup);

        this.myThreadId = threadId;

        this.valueGetStates = new ValueGetStates();
        this.valuePutStates = new ValuePutStates();
        this.valueAccumulateStates = new ValueAccumulateStates();
        this.asyncAtStates = new AsyncAtStates();
        this.peerBarrierStates = new PeerBarrierStates();
    }
//
//    public static InternalGroup asyncSplitGroup(Integer split, int ordering) {
//        SplitGroupStates states = super.getSplitGroupStates();
//        int round = states.getNextRound(myThreadId);
//        SplitGroupStates.State state = states.getOrCreate(round, split, ordering, this);
//        state.processLocal(this);
//
//        return state.getFuture();
//    }

    public int myId() {
        return myThreadId;
    }

    @Override
    public PcjFuture<Void> asyncBarrier() {
        BarrierStates states = super.getBarrierStates();
        int round = states.getNextRound(myThreadId);
        BarrierStates.State state = states.getOrCreate(round, this);
        state.processLocal(this);

        return state.getFuture();
    }

    @Override
    public PcjFuture<Void> asyncBarrier(int threadId) {
        if (myThreadId == threadId) {
            throw new IllegalArgumentException("Cannot barrier with myself: " + threadId);
        }

        PeerBarrierStates.State state = peerBarrierStates.getOrCreate(threadId);

        int globalThreadId = super.getGlobalThreadId(threadId);
        int physicalId = InternalPCJ.getNodeData().getPhysicalId(globalThreadId);
        SocketChannel socket = InternalPCJ.getNodeData().getSocketChannelByPhysicalId(physicalId);

        PeerBarrierMessage message = new PeerBarrierMessage(super.getGroupId(), myThreadId, threadId);

        InternalPCJ.getNetworker().send(socket, message);

        return state.doMineBarrier();
    }

    public PeerBarrierStates getPeerBarrierStates() {
        return peerBarrierStates;
    }

    @Override
    public <T> PcjFuture<T> asyncGet(int threadId, Enum<?> variable, int... indices) {
        ValueGetStates.State<T> state = valueGetStates.create();

        int globalThreadId = super.getGlobalThreadId(threadId);
        int physicalId = InternalPCJ.getNodeData().getPhysicalId(globalThreadId);
        SocketChannel socket = InternalPCJ.getNodeData().getSocketChannelByPhysicalId(physicalId);

        ValueGetRequestMessage message = new ValueGetRequestMessage(
                super.getGroupId(), state.getRequestNum(), myThreadId, threadId,
                variable.getDeclaringClass().getName(), variable.name(), indices);

        InternalPCJ.getNetworker().send(socket, message);

        return state.getFuture();
    }

    @Override
    public <T> PcjFuture<T> asyncCollect(Enum<?> variable, int... indices) {
        String sharedEnumClassName = variable.getDeclaringClass().getName();
        String variableName = variable.name();

        CollectStates states = super.getCollectStates();
        CollectStates.State<T> state = states.create(myThreadId, this);

        state.downProcessNode(this, sharedEnumClassName, variableName, indices);

        return state.getFuture();
    }

    @Override
    public <T> PcjFuture<T> asyncReduce(ReduceOperation<T> function, Enum<?> variable, int... indices) {
        String sharedEnumClassName = variable.getDeclaringClass().getName();
        String variableName = variable.name();

        ReduceStates states = super.getReduceStates();
        ReduceStates.State<T> state = states.create(myThreadId, this);

        state.downProcessNode(this, sharedEnumClassName, variableName, indices, function);

        return state.getFuture();
    }

    public ValueGetStates getValueGetStates() {
        return valueGetStates;
    }

    @Override
    public <T> PcjFuture<Void> asyncPut(T newValue, int threadId, Enum<?> variable, int... indices) {
        ValuePutStates.State state = valuePutStates.create();

        int globalThreadId = super.getGlobalThreadId(threadId);
        int physicalId = InternalPCJ.getNodeData().getPhysicalId(globalThreadId);
        SocketChannel socket = InternalPCJ.getNodeData().getSocketChannelByPhysicalId(physicalId);

        ValuePutRequestMessage message = new ValuePutRequestMessage(
                super.getGroupId(), state.getRequestNum(), myThreadId, threadId,
                variable.getDeclaringClass().getName(), variable.name(), indices, newValue);

        try {
            InternalPCJ.getNetworker().send(socket, message);
        } catch (PcjRuntimeException ex) {
            state.signal(ex);
        }

        return state.getFuture();
    }

    public ValuePutStates getValuePutStates() {
        return valuePutStates;
    }

    @Override
    public <T> PcjFuture<Void> asyncAccumulate(ReduceOperation<T> function, T newValue, int threadId, Enum<?> variable, int... indices) {
        ValueAccumulateStates.State state = valueAccumulateStates.create();

        int globalThreadId = super.getGlobalThreadId(threadId);
        int physicalId = InternalPCJ.getNodeData().getPhysicalId(globalThreadId);
        SocketChannel socket = InternalPCJ.getNodeData().getSocketChannelByPhysicalId(physicalId);

        ValueAccumulateRequestMessage<T> message = new ValueAccumulateRequestMessage<>(
                super.getGroupId(), state.getRequestNum(), myThreadId, threadId,
                variable.getDeclaringClass().getName(), variable.name(), indices, function, newValue);

        try {
            InternalPCJ.getNetworker().send(socket, message);
        } catch (PcjRuntimeException ex) {
            state.signal(ex);
        }

        return state.getFuture();
    }

    public ValueAccumulateStates getValueAccumulateStates() {
        return valueAccumulateStates;
    }

    @Override
    public <T> PcjFuture<Void> asyncBroadcast(T newValue, Enum<?> variable, int... indices) {
        BroadcastStates states = super.getBroadcastStates();
        BroadcastStates.State state = states.create(myThreadId, this);

        BroadcastRequestMessage message = new BroadcastRequestMessage(
                super.getGroupId(), state.getRequestNum(), myThreadId,
                variable.getDeclaringClass().getName(), variable.name(), indices, newValue);

        NodeData nodeData = InternalPCJ.getNodeData();
        SocketChannel socket = nodeData.getSocketChannelByPhysicalId(nodeData.getCurrentNodePhysicalId());
        try {
            InternalPCJ.getNetworker().send(socket, message);
        } catch (PcjRuntimeException ex) {
            Queue<Exception> queue = new ConcurrentLinkedQueue<>();
            queue.add(ex);
            state.signal(queue);
        }

        return state.getFuture();
    }

    @Override
    public <T> PcjFuture<T> asyncAt(int threadId, AsyncTask<T> asyncTask) {
        AsyncAtStates.State<T> state = asyncAtStates.create();

        int globalThreadId = super.getGlobalThreadId(threadId);
        int physicalId = InternalPCJ.getNodeData().getPhysicalId(globalThreadId);
        SocketChannel socket = InternalPCJ.getNodeData().getSocketChannelByPhysicalId(physicalId);

        AsyncAtRequestMessage<T> message = new AsyncAtRequestMessage<>(
                super.getGroupId(), state.getRequestNum(), myThreadId,
                threadId, asyncTask);

        try {
            InternalPCJ.getNetworker().send(socket, message);
        } catch (PcjRuntimeException ex) {
            state.signal(null, ex);
        }

        return state.getFuture();
    }

    public AsyncAtStates getAsyncAtStates() {
        return asyncAtStates;
    }
}
