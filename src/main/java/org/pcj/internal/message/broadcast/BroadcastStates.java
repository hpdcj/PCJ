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
import org.pcj.internal.network.CloneInputStream;

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

        BroadcastFuture future = new BroadcastFuture();
        State state = new State(requestNum, threadId, commonGroup.getCommunicationTree().getChildrenNodes().size(), future);

        stateMap.put(Arrays.asList(requestNum, threadId), state);

        return state;
    }

    public State getOrCreate(int requestNum, int requesterThreadId, InternalCommonGroup commonGroup) {
        return stateMap.computeIfAbsent(Arrays.asList(requestNum, requesterThreadId),
                key -> new State(requestNum, requesterThreadId, commonGroup.getCommunicationTree().getChildrenNodes().size()));
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
            exceptions = new ConcurrentLinkedDeque<>();
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

        void downProcessNode(InternalCommonGroup group, CloneInputStream clonedData, String sharedEnumClassName, String name, int[] indices) {
            NodeData nodeData = InternalPCJ.getNodeData();
            Set<Integer> threadsId = group.getLocalThreadsId();
            for (int threadId : threadsId) {
                int globalThreadId = group.getGlobalThreadId(threadId);
                PcjThread pcjThread = nodeData.getPcjThread(globalThreadId);
                InternalStorages storage = pcjThread.getThreadData().getStorages();

                try {
                    clonedData.reset();
                    Object newValue = new ObjectInputStream(clonedData).readObject();

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

                int globalThreadId = group.getGlobalThreadId(requesterThreadId);
                int requesterPhysicalId = nodeData.getPhysicalId(globalThreadId);
                if (requesterPhysicalId != nodeData.getCurrentNodePhysicalId()) { // requester is going to receive response
                    BroadcastStates.this.remove(requestNum, requesterThreadId);
                }

                Message message;
                SocketChannel socket;

                int physicalId = nodeData.getCurrentNodePhysicalId();
                if (physicalId != group.getCommunicationTree().getMasterNode()) {
                    int parentId = group.getCommunicationTree().getParentNode();
                    socket = nodeData.getSocketChannelByPhysicalId(parentId);

                    message = new BroadcastValueInformMessage(group.getGroupId(), requestNum, requesterThreadId, exceptions);
                } else {
                    message = new BroadcastValueResponseMessage(group.getGroupId(), requestNum, requesterThreadId, exceptions);

                    socket = nodeData.getSocketChannelByPhysicalId(requesterPhysicalId);
                }

                InternalPCJ.getNetworker().send(socket, message);
            }
        }

        public void signal(Queue<Exception> messageExceptions) {
            if ((messageExceptions != null) && (!messageExceptions.isEmpty())) {
                exceptions.addAll(messageExceptions);
            }
            if (exceptions.isEmpty()) {
                future.signalDone();
            } else {
                PcjRuntimeException ex = new PcjRuntimeException("Broadcasting value failed");
                exceptions.forEach(ex::addSuppressed);
                future.signalException(ex);
            }
        }
    }
}
