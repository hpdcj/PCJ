/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.broadcast;

import java.io.ObjectInputStream;
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
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.InternalStorages;
import org.pcj.internal.NodeData;
import org.pcj.internal.PcjThread;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.collect.CollectResponseMessage;
import org.pcj.internal.message.collect.CollectValueMessage;
import org.pcj.internal.network.InputStreamCloner;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class BroadcastStates {

    private final AtomicInteger counter;
    private final ConcurrentMap<List<Integer>, State> stateMap;

    public BroadcastStates() {
        counter = new AtomicInteger(0);
        stateMap = new ConcurrentHashMap<>();
    }

    public State create(int threadId, InternalCommonGroup commonGroup) {
        int requestNum = counter.incrementAndGet();

        NodeData nodeData = InternalPCJ.getNodeData();

        BroadcastFuture future = new BroadcastFuture();
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
        private final BroadcastFuture future;
        private final Queue<Exception> exceptions;

        private State(int requestNum, int requesterThreadId, int childrenCount, BroadcastFuture future) {
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

        void downProcessNode(InternalCommonGroup group, InputStreamCloner inputStreamCloner, String sharedEnumClassName, String name, int[] indices) {
            NodeData nodeData = InternalPCJ.getNodeData();
            Set<Integer> threadsId = group.getLocalThreadsId();
            for (int threadId : threadsId) {
                int globalThreadId = group.getGlobalThreadId(threadId);
                PcjThread pcjThread = nodeData.getPcjThread(globalThreadId);
                InternalStorages storage = pcjThread.getThreadData().getStorages();

                try {
                    InputStreamCloner.ClonedInputStream clonedInputStream = inputStreamCloner.newInputStream();
                    Object newValue = new ObjectInputStream(clonedInputStream).readObject();

                    storage.put(newValue, sharedEnumClassName, name, indices);
                } catch (Exception ex) {
                    exceptions.add(ex);
                }
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
            if (leftPhysical == 0) {
                NodeData nodeData = InternalPCJ.getNodeData();

                int requesterPhysicalId = nodeData.getPhysicalId(group.getGlobalThreadId(requesterThreadId));
                if (requesterPhysicalId != nodeData.getCurrentNodePhysicalId()) { // requester will receive response
                    BroadcastStates.this.remove(requestNum, requesterThreadId);
                }

                Message message;
                SocketChannel socket;

                int parentId = group.getCommunicationTree().getParentNode(requesterPhysicalId);
                if (parentId>=0) {
                    message = new BroadcastInformMessage(group.getGroupId(), requestNum, requesterThreadId, exceptions);
                    socket = nodeData.getSocketChannelByPhysicalId(parentId);
                } else {
                    message = new BroadcastResponseMessage(group.getGroupId(), requestNum, requesterThreadId, exceptions);
                    socket = nodeData.getSocketChannelByPhysicalId(requesterPhysicalId);
                }

                InternalPCJ.getNetworker().send(socket, message);
            }
        }

        public void signal(Queue<Exception> messageExceptions) {
            if ((messageExceptions != null) && (!messageExceptions.isEmpty())) {
                PcjRuntimeException ex = new PcjRuntimeException("Broadcasting value failed", messageExceptions.poll());
                messageExceptions.forEach(ex::addSuppressed);
                future.signalException(ex);
            } else {
                future.signalDone();
            }
        }
    }
}
