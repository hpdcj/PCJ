/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
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
import org.pcj.internal.InternalStorages;
import org.pcj.internal.NodeData;
import org.pcj.internal.PcjThread;
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

        ReduceFuture<T> future = new ReduceFuture<>();
        State<T> state = new State<>(requestNum, threadId, commonGroup.getCommunicationTree().getChildrenNodes().size(), future);

        stateMap.put(Arrays.asList(requestNum, threadId), state);

        return state;
    }

    @SuppressWarnings("unchecked")
    public <T> State<T> getOrCreate(int requestNum, int requesterThreadId, InternalCommonGroup commonGroup) {
        return (State<T>) stateMap.computeIfAbsent(Arrays.asList(requestNum, requesterThreadId),
                key -> new State<>(requestNum, requesterThreadId, commonGroup.getCommunicationTree().getChildrenNodes().size()));
    }

    public State remove(int requestNum, int threadId) {
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

        public int getRequestNum() {
            return requestNum;
        }

        public PcjFuture<T> getFuture() {
            return future;
        }

        void downProcessNode(InternalCommonGroup group, String sharedEnumClassName, String variableName, int[] indices, ReduceOperation<T> function) {
            this.sharedEnumClassName = sharedEnumClassName;
            this.variableName = variableName;
            this.indices = indices;
            this.function = function;

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

                int globalThreadId = group.getGlobalThreadId(requesterThreadId);
                int requesterPhysicalId = nodeData.getPhysicalId(globalThreadId);
                if (requesterPhysicalId != nodeData.getCurrentNodePhysicalId()) { // requester is going to receive response
                    ReduceStates.this.remove(requestNum, requesterThreadId);
                }

                T reducedValue = null;
                if (exceptions.isEmpty()) {
                    try {
                        reducedValue = getValue(group);
                        for (T value : receivedValues) {
                            reducedValue = function.apply(reducedValue, value);
                        }
                    } catch (Exception ex) {
                        exceptions.add(ex);
                    }
                }

                Message message;
                SocketChannel socket;

                int physicalId = nodeData.getCurrentNodePhysicalId();
                if (physicalId != group.getCommunicationTree().getMasterNode()) {
                    int parentId = group.getCommunicationTree().getParentNode();
                    socket = nodeData.getSocketChannelByPhysicalId(parentId);

                    message = new ReduceValueMessage<>(group.getGroupId(), requestNum, requesterThreadId, reducedValue, exceptions);
                } else {
                    socket = nodeData.getSocketChannelByPhysicalId(requesterPhysicalId);

                    message = new ReduceResponseMessage<>(group.getGroupId(), requestNum, requesterThreadId, reducedValue, exceptions);
                }

                try {
                    InternalPCJ.getNetworker().send(socket, message);
                } catch (Exception ex) {
                    exceptions.add(ex);
                    InternalPCJ.getNetworker().send(socket, message);
                }
            }
        }

        private T getValue(InternalCommonGroup group) {
            NodeData nodeData = InternalPCJ.getNodeData();
            Set<Integer> threadsId = group.getLocalThreadsId();

            boolean foundAny = false;
            T reducedValue = null;
            for (int threadId : threadsId) {
                int globalThreadId = group.getGlobalThreadId(threadId);
                PcjThread pcjThread = nodeData.getPcjThread(globalThreadId);
                InternalStorages storage = pcjThread.getThreadData().getStorages();

                T value = storage.get(this.sharedEnumClassName, this.variableName, this.indices);
                if (!foundAny) {
                    foundAny = true;
                    reducedValue = value;
                } else {
                    reducedValue = function.apply(reducedValue, value);
                }
            }
            return reducedValue;
        }

        public void signal(T value, Queue<Exception> messageExceptions) {
            if ((messageExceptions != null) && (!messageExceptions.isEmpty())) {
                PcjRuntimeException ex = new PcjRuntimeException("Reducing values failed");
                messageExceptions.forEach(ex::addSuppressed);
                future.signalException(ex);
            } else {
                future.signalDone(value);
            }
        }


    }
}
