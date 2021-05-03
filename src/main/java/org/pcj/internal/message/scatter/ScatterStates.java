/*
 * Copyright (c) 2011-2021, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.scatter;

import java.nio.channels.SocketChannel;
import java.util.Arrays;
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
public class ScatterStates {

    private final AtomicInteger counter;
    private final ConcurrentMap<List<Integer>, State> stateMap;

    public ScatterStates() {
        counter = new AtomicInteger(0);
        stateMap = new ConcurrentHashMap<>();
    }

    public State create(int threadId, InternalCommonGroup commonGroup) {
        int requestNum = counter.incrementAndGet();

        NodeData nodeData = InternalPCJ.getNodeData();

        ScatterFuture future = new ScatterFuture();
        State state = new State(requestNum, threadId,
                commonGroup.getCommunicationTree().getChildrenNodes(nodeData.getCurrentNodePhysicalId()).size(),
                future);

        stateMap.put(Arrays.asList(requestNum, threadId), state);

        return state;
    }

    public State getOrCreate(int requestNum, int requesterThreadId, InternalCommonGroup commonGroup) {
        NodeData nodeData = InternalPCJ.getNodeData();
        int requesterPhysicalId = nodeData.getPhysicalId(commonGroup.getGlobalThreadId(requesterThreadId));
        return stateMap.computeIfAbsent(Arrays.asList(requestNum, requesterThreadId),
                key -> new State(requestNum, requesterThreadId,
                        commonGroup.getCommunicationTree().getChildrenNodes(requesterPhysicalId).size()));
    }

    public State remove(int requestNum, int threadId) {
        return stateMap.remove(Arrays.asList(requestNum, threadId));
    }

    public class State {

        private final int requestNum;
        private final int requesterThreadId;
        private final AtomicInteger notificationCount;
        private final ScatterFuture future;
        private final Queue<Exception> exceptions;

        private State(int requestNum, int requesterThreadId, int childrenCount, ScatterFuture future) {
            this.requestNum = requestNum;
            this.requesterThreadId = requesterThreadId;
            this.future = future;

            // notification from children and from itself
            notificationCount = new AtomicInteger(childrenCount + 1);
            exceptions = new ConcurrentLinkedQueue<>();
        }

        private State(int requestNum, int requesterThreadId, int childrenCount) {
            this(requestNum, requesterThreadId, childrenCount, null);
        }

        public int getRequestNum() {
            return requestNum;
        }

        public PcjFuture<Void> getFuture() {
            return future;
        }

        void downProcessNode(InternalCommonGroup group, String sharedEnumClassName, String name, int[] indices, Map<Integer, Object> newValueMap) {
            NodeData nodeData = InternalPCJ.getNodeData();
            Set<Integer> threadsId = group.getLocalThreadsId();
            for (int threadId : threadsId) {
                int globalThreadId = group.getGlobalThreadId(threadId);
                PcjThread pcjThread = nodeData.getPcjThread(globalThreadId);
                InternalStorages storage = pcjThread.getThreadData().getStorages();

                try {
                    Object newValue = newValueMap.remove(threadId);

                    storage.put(newValue, sharedEnumClassName, name, indices);
                } catch (Exception ex) {
                    exceptions.add(ex);
                }
            }

            Networker networker = InternalPCJ.getNetworker();
            int requesterPhysicalId = nodeData.getPhysicalId(group.getGlobalThreadId(requesterThreadId));
            Set<Integer> childrenNodes = group.getCommunicationTree().getChildrenNodes(requesterPhysicalId);
            Map<Integer, Integer> groupIdToGlobalIdMap = group.getThreadsMap();
            for (int childrenNode : childrenNodes) {
                List<Integer> subTree = group.getCommunicationTree().getSubtree(requesterPhysicalId, childrenNode);
                Map<Integer, Object> subTreeNewValueMap = groupIdToGlobalIdMap.entrySet()
                        .stream()
                        .filter(entry -> subTree.contains(nodeData.getPhysicalId(entry.getValue())))
                        .map(Map.Entry::getKey)
                        .collect(HashMap::new, (map, key) -> map.put(key, newValueMap.get(key)), HashMap::putAll);

                ScatterRequestMessage message = new ScatterRequestMessage(
                        group.getGroupId(), requestNum, requesterThreadId,
                        sharedEnumClassName, name, indices, subTreeNewValueMap);
                SocketChannel socket = nodeData.getSocketChannelByPhysicalId(childrenNode);
                networker.send(socket, message);
            }

            nodeProcessed(group);
        }

        void upProcessNode(InternalCommonGroup group, Queue<Exception> messageExceptions) {
            if ((messageExceptions != null) && (!messageExceptions.isEmpty())) {
                exceptions.addAll(messageExceptions);
            }

            nodeProcessed(group);
        }

        private void nodeProcessed(InternalCommonGroup group) {
            int leftPhysical = notificationCount.decrementAndGet();
            NodeData nodeData = InternalPCJ.getNodeData();
            if (leftPhysical == 0) {
                int requesterPhysicalId = nodeData.getPhysicalId(group.getGlobalThreadId(requesterThreadId));
                if (requesterPhysicalId != nodeData.getCurrentNodePhysicalId()) { // requester will receive response
                    ScatterStates.this.remove(requestNum, requesterThreadId);
                }

                int parentId = group.getCommunicationTree().getParentNode(requesterPhysicalId);
                if (parentId >= 0) {
                    Message message = new ScatterResponseMessage(group.getGroupId(), requestNum, requesterThreadId, exceptions);
                    SocketChannel socket = nodeData.getSocketChannelByPhysicalId(parentId);
                    InternalPCJ.getNetworker().send(socket, message);
                } else {
                    ScatterStates.State state = ScatterStates.this.remove(requestNum, requesterThreadId);
                    state.signal(exceptions);
                }
            }
        }

        public void signal(Queue<Exception> messageExceptions) {
            if ((messageExceptions != null) && (!messageExceptions.isEmpty())) {
                PcjRuntimeException ex = new PcjRuntimeException("Scatter value array failed", messageExceptions.poll());
                messageExceptions.forEach(ex::addSuppressed);
                future.signalException(ex);
            } else {
                future.signalDone();
            }
        }

        protected void addException(Exception ex) {
            exceptions.add(ex);
        }
    }
}
