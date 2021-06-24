/*
 * Copyright (c) 2011-2021, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.reduce;

import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.pcj.PcjFuture;
import org.pcj.PcjRuntimeException;
import org.pcj.ReduceOperation;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.Networker;
import org.pcj.internal.NodeData;
import org.pcj.internal.PcjThread;
import org.pcj.internal.PcjThreadData;
import org.pcj.internal.message.Message;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class ReduceStates {

    private final AtomicInteger counter;
    private final ConcurrentMap<List<Integer>, State<?>> stateMap;

    public ReduceStates() {
        counter = new AtomicInteger(0);
        stateMap = new ConcurrentHashMap<>();
    }

    public <T> State<T> create(int threadId, InternalCommonGroup commonGroup) {
        int requestNum = counter.incrementAndGet();

        NodeData nodeData = InternalPCJ.getNodeData();

        ReduceFuture<T> future = new ReduceFuture<>();
        State<T> state = new State<>(requestNum, threadId,
                commonGroup.getCommunicationTree().getChildrenNodes(nodeData.getCurrentNodePhysicalId()).size(),
                future);

        stateMap.put(Arrays.asList(requestNum, threadId), state);

        return state;
    }

    @SuppressWarnings("unchecked")
    public <T> State<T> getOrCreate(int requestNum, int requesterThreadId, InternalCommonGroup commonGroup) {
        NodeData nodeData = InternalPCJ.getNodeData();
        int requesterPhysicalId = nodeData.getPhysicalId(commonGroup.getGlobalThreadId(requesterThreadId));
        return (State<T>) stateMap.computeIfAbsent(Arrays.asList(requestNum, requesterThreadId),
                key -> new State<>(requestNum, requesterThreadId,
                        commonGroup.getCommunicationTree().getChildrenNodes(requesterPhysicalId).size()));
    }

    public State<?> remove(int requestNum, int threadId) {
        return stateMap.remove(Arrays.asList(requestNum, threadId));
    }

    public class State<T> {

        private final int requestNum;
        private final int requesterThreadId;
        private final AtomicInteger notificationCount;
        private final ReduceFuture<T> future;
        private final Queue<T> receivedValues;
        private final Queue<Exception> exceptions;
        private String sharedEnumClassName;
        private String variableName;
        private int[] indices;
        private ReduceOperation<T> function;

        private State(int requestNum, int requesterThreadId, int childrenCount, ReduceFuture<T> future) {
            this.requestNum = requestNum;
            this.requesterThreadId = requesterThreadId;
            this.future = future;

            // notification from children and from itself
            notificationCount = new AtomicInteger(childrenCount + 1);
            receivedValues = new ConcurrentLinkedQueue<>();
            exceptions = new ConcurrentLinkedQueue<>();
        }

        private State(int requestNum, int requesterThreadId, int childrenCount) {
            this(requestNum, requesterThreadId, childrenCount, null);
        }

        public PcjFuture<T> getFuture() {
            return future;
        }

        public void downProcessNode(InternalCommonGroup group, String sharedEnumClassName, String variableName, int[] indices, ReduceOperation<T> function) {
            this.sharedEnumClassName = sharedEnumClassName;
            this.variableName = variableName;
            this.indices = indices;
            this.function = function;

            ReduceRequestMessage<T> message = new ReduceRequestMessage<>(
                    group.getGroupId(), this.requestNum, this.requesterThreadId,
                    sharedEnumClassName, variableName, indices, function
            );

            NodeData nodeData = InternalPCJ.getNodeData();
            Networker networker = InternalPCJ.getNetworker();

            int requesterPhysicalId = nodeData.getPhysicalId(group.getGlobalThreadId(requesterThreadId));
            group.getCommunicationTree().getChildrenNodes(requesterPhysicalId)
                    .stream()
                    .map(nodeData::getSocketChannelByPhysicalId)
                    .forEach(socket -> networker.send(socket, message));

            nodeProcessed(group);
        }

        void upProcessNode(InternalCommonGroup group, T receivedValue, Queue<Exception> messageExceptions) {
            if ((messageExceptions != null) && (!messageExceptions.isEmpty())) {
                exceptions.addAll(messageExceptions);
            } else {
                receivedValues.add(receivedValue);
            }

            nodeProcessed(group);
        }

        private void nodeProcessed(InternalCommonGroup group) {
            int leftPhysical = notificationCount.decrementAndGet();
            if (leftPhysical == 0) {
                NodeData nodeData = InternalPCJ.getNodeData();

                int requesterPhysicalId = nodeData.getPhysicalId(group.getGlobalThreadId(requesterThreadId));
                if (requesterPhysicalId != nodeData.getCurrentNodePhysicalId()) { // requester will receive response
                    ReduceStates.this.remove(requestNum, requesterThreadId);
                }

                T reducedValue = null;
                if (exceptions.isEmpty()) {
                    try {
                        reducedValue = getCurrentNodeReducedValue(group);
                        for (T value : receivedValues) {
                            reducedValue = function.apply(reducedValue, value);
                        }
                    } catch (Exception ex) {
                        exceptions.add(ex);
                    }
                }

                Message message;
                SocketChannel socket;

                int parentId = group.getCommunicationTree().getParentNode(requesterPhysicalId);
                if (parentId >= 0) {
                    message = new ReduceResponseMessage<>(group.getGroupId(), requestNum, requesterThreadId, reducedValue, exceptions);
                    socket = nodeData.getSocketChannelByPhysicalId(parentId);
                } else {
                    message = new ReduceValueMessage<>(group.getGroupId(), requestNum, requesterThreadId, reducedValue, exceptions);
                    socket = nodeData.getSocketChannelByPhysicalId(nodeData.getCurrentNodePhysicalId());
                }

                Networker networker = InternalPCJ.getNetworker();
                try {
                    networker.send(socket, message);
                } catch (Exception ex) {
                    exceptions.add(ex);
                    networker.send(socket, message);
                }
            }
        }

        private T getCurrentNodeReducedValue(InternalCommonGroup group) {
            NodeData nodeData = InternalPCJ.getNodeData();
            Set<Integer> threadsId = group.getLocalThreadsId();

            return threadsId.stream()
                    .map(group::getGlobalThreadId)
                    .map(nodeData::getPcjThread)
                    .map(PcjThread::getThreadData)
                    .map(PcjThreadData::getStorages)
                    .map(storages -> storages.<T>get(this.sharedEnumClassName, this.variableName, this.indices))
                    .reduce(function)
                    .orElse(null);
        }

        public void signal(T value, Queue<Exception> messageExceptions) {
            if ((messageExceptions != null) && (!messageExceptions.isEmpty())) {
                PcjRuntimeException ex = new PcjRuntimeException("Reducing values failed", messageExceptions.poll());
                messageExceptions.forEach(ex::addSuppressed);
                future.signalException(ex);
            } else {
                future.signalDone(value);
            }
        }


    }
}
