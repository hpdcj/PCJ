/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.collect;

import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.pcj.PcjFuture;
import org.pcj.PcjRuntimeException;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.InternalStorages;
import org.pcj.internal.NodeData;
import org.pcj.internal.PcjThread;
import org.pcj.internal.message.Message;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class CollectStates {
    private final AtomicInteger counter;
    private final ConcurrentMap<List<Integer>, State<?>> stateMap;

    public CollectStates() {
        counter = new AtomicInteger(0);
        stateMap = new ConcurrentHashMap<>();
    }

    public <T> State<T> create(int threadId, InternalCommonGroup commonGroup) {
        int requestNum = counter.incrementAndGet();

        CollectFuture<T> future = new CollectFuture<>();
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
        private final CollectFuture<T> future;
        private final Queue<Exception> exceptions;
        private final ConcurrentMap<Integer, T> valueMap;
        private String sharedEnumClassName;
        private String variableName;
        private int[] indices;

        private State(int requestNum, int requesterThreadId, int childrenCount, CollectFuture<T> future) {
            this.requestNum = requestNum;
            this.requesterThreadId = requesterThreadId;
            this.future = future;

            // notification from children and from itself
            notificationCount = new AtomicInteger(childrenCount + 1);
            valueMap = new ConcurrentHashMap<>();
            exceptions = new ConcurrentLinkedDeque<>();
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

        void downProcessNode(InternalCommonGroup group, String sharedEnumClassName, String variableName, int[] indices) {
            this.sharedEnumClassName = sharedEnumClassName;
            this.variableName = variableName;
            this.indices = indices;

            nodeProcessed(group);
        }

        void upProcessNode(InternalCommonGroup group, Map<Integer, T> receivedValueMap, Queue<Exception> messageExceptions) {
            if ((messageExceptions != null) && (!messageExceptions.isEmpty())) {
                exceptions.addAll(messageExceptions);
            } else {
                valueMap.putAll(receivedValueMap);
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
                    CollectStates.this.remove(requestNum, requesterThreadId);
                }

                fillValueMap(group);

                Message message;
                SocketChannel socket;

                int physicalId = nodeData.getCurrentNodePhysicalId();
                if (physicalId != group.getCommunicationTree().getMasterNode()) {
                    int parentId = group.getCommunicationTree().getParentNode();
                    socket = nodeData.getSocketChannelByPhysicalId(parentId);

                    message = new CollectValueMessage<>(group.getGroupId(), requestNum, requesterThreadId, valueMap, exceptions);
                } else {
                    socket = nodeData.getSocketChannelByPhysicalId(requesterPhysicalId);

                    message = new CollectResponseMessage<T>(group.getGroupId(), valueMap, requestNum, requesterThreadId, exceptions);
                }

                InternalPCJ.getNetworker().send(socket, message);
            }
        }

        public Class<?> getValueClass() {
            NodeData nodeData = InternalPCJ.getNodeData();
            PcjThread pcjThread = nodeData.getPcjThread(requesterThreadId);

            InternalStorages storages = pcjThread.getThreadData().getStorages();
            return storages.getClass(this.sharedEnumClassName, this.variableName, this.indices.length);
        }

        private void fillValueMap(InternalCommonGroup group) {
            NodeData nodeData = InternalPCJ.getNodeData();
            Set<Integer> threadsId = group.getLocalThreadsId();
            for (int threadId : threadsId) {
                int globalThreadId = group.getGlobalThreadId(threadId);
                PcjThread pcjThread = nodeData.getPcjThread(globalThreadId);
                InternalStorages storage = pcjThread.getThreadData().getStorages();

                valueMap.put(threadId, storage.get(this.sharedEnumClassName, this.variableName, this.indices));
            }
        }

        public void signal(Object values, Queue<Exception> messageExceptions) {
            if ((messageExceptions != null) && (!messageExceptions.isEmpty())) {
                exceptions.addAll(messageExceptions);
            }
            if (exceptions.isEmpty()) {
                future.signalDone(values);
            } else {
                PcjRuntimeException ex = new PcjRuntimeException("Collecting values failed");
                exceptions.forEach(ex::addSuppressed);
                future.signalException(ex);
            }
        }
    }
}
