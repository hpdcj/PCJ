/*
 * Copyright (c) 2011-2021, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.gather;

import java.lang.reflect.Array;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.pcj.PcjFuture;
import org.pcj.PcjRuntimeException;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.InternalStorages;
import org.pcj.internal.Networker;
import org.pcj.internal.NodeData;
import org.pcj.internal.PcjThread;
import org.pcj.internal.message.Message;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class GatherStates {

    private final AtomicInteger counter;
    private final ConcurrentMap<List<Integer>, State<?>> stateMap;

    public GatherStates() {
        counter = new AtomicInteger(0);
        stateMap = new ConcurrentHashMap<>();
    }

    public <T> State<T> create(int threadId, InternalCommonGroup commonGroup) {
        int requestNum = counter.incrementAndGet();

        NodeData nodeData = InternalPCJ.getNodeData();

        GatherFuture<T> future = new GatherFuture<>();
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
        private final GatherFuture<T> future;
        private final Queue<Exception> exceptions;
        private final Map<Integer, T> valueMap;
        private String sharedEnumClassName;
        private String variableName;
        private int[] indices;

        private State(int requestNum, int requesterThreadId, int childrenCount, GatherFuture<T> future) {
            this.requestNum = requestNum;
            this.requesterThreadId = requesterThreadId;
            this.future = future;

            // notification from children and from itself
            notificationCount = new AtomicInteger(childrenCount + 1);
            valueMap = Collections.synchronizedMap(new HashMap<>());
            exceptions = new ConcurrentLinkedQueue<>();
        }

        private State(int requestNum, int requesterThreadId, int childrenCount) {
            this(requestNum, requesterThreadId, childrenCount, null);
        }

        public PcjFuture<Map<Integer, T>> getFuture() {
            return future;
        }

        public void downProcessNode(InternalCommonGroup group, String sharedEnumClassName, String variableName, int[] indices) {
            this.sharedEnumClassName = sharedEnumClassName;
            this.variableName = variableName;
            this.indices = indices;

            GatherRequestMessage message = new GatherRequestMessage(
                    group.getGroupId(), this.requestNum, this.requesterThreadId,
                    sharedEnumClassName, variableName, indices);

            NodeData nodeData = InternalPCJ.getNodeData();
            Networker networker = InternalPCJ.getNetworker();

            int requesterPhysicalId = nodeData.getPhysicalId(group.getGlobalThreadId(requesterThreadId));
            group.getCommunicationTree().getChildrenNodes(requesterPhysicalId)
                    .stream()
                    .map(nodeData::getSocketChannelByPhysicalId)
                    .forEach(socket -> networker.send(socket, message));

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

                int requesterPhysicalId = nodeData.getPhysicalId(group.getGlobalThreadId(requesterThreadId));
                if (requesterPhysicalId != nodeData.getCurrentNodePhysicalId()) { // requester will receive response
                    GatherStates.this.remove(requestNum, requesterThreadId);
                }

                if (exceptions.isEmpty()) {
                    try {
                        fillValueMap(group);
                    } catch (Exception ex) {
                        exceptions.add(ex);
                    }
                }

                Message message;
                SocketChannel socket;

                int parentId = group.getCommunicationTree().getParentNode(requesterPhysicalId);
                if (parentId >= 0) {
                    message = new GatherResponseMessage<>(group.getGroupId(), requestNum, requesterThreadId, valueMap, exceptions);
                    socket = nodeData.getSocketChannelByPhysicalId(parentId);
                } else {
                    message = new GatherValueMessage<>(group.getGroupId(), requestNum, requesterThreadId, valueMap, exceptions);
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

        public void signal(Map<Integer, T> valueMap, Queue<Exception> messageExceptions) {
            if ((messageExceptions != null) && (!messageExceptions.isEmpty())) {
                PcjRuntimeException ex = new PcjRuntimeException("Gathering values failed", messageExceptions.poll());
                messageExceptions.forEach(ex::addSuppressed);
                future.signalException(ex);
            } else {
                future.signalDone(valueMap);
            }
        }

        private Object convertMapToArray(Map<Integer, T> valueMap) {
            Class<?> clazz = getValueClass();

            int size = valueMap.size();
            Object array = Array.newInstance(clazz, size);

            for (Map.Entry<Integer, T> entry : valueMap.entrySet()) {
                int index = entry.getKey();
                T t = entry.getValue();

                Array.set(array, index, t);
            }
            return array;
        }

        private Class<?> getValueClass() {
            NodeData nodeData = InternalPCJ.getNodeData();
            PcjThread pcjThread = nodeData.getPcjThread(requesterThreadId);

            InternalStorages storages = pcjThread.getThreadData().getStorages();
            return storages.getClass(this.sharedEnumClassName, this.variableName, this.indices.length);
        }
    }
}
