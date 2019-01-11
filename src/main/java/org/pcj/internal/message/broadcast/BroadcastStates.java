/*
 * Copyright (c) 2011-2018, PCJ Library, Marek Nowicki
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.pcj.PcjRuntimeException;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.InternalStorages;
import org.pcj.internal.NodeData;
import org.pcj.internal.PcjThread;
import org.pcj.internal.message.Message;
import org.pcj.internal.network.CloneInputStream;

public class BroadcastStates {
    private final AtomicInteger counter;
    private final ConcurrentMap<List<Integer>, State> stateMap;

    public BroadcastStates() {
        counter = new AtomicInteger(0);
        stateMap = new ConcurrentHashMap<>();
    }

    public State create(int threadId, int childrenCount) {
        int requestNum = counter.incrementAndGet();

        BroadcastFuture future = new BroadcastFuture();
        State state = new State(requestNum, threadId, childrenCount, future);

        stateMap.put(Arrays.asList(requestNum, threadId), state);

        return state;
    }

    public State getOrCreate(int requestNum, int requesterThreadId, int childrenCount) {
        return stateMap.computeIfAbsent(Arrays.asList(requestNum, requesterThreadId),
                key -> new State(requestNum, requesterThreadId, childrenCount));
    }

    public State remove(int requestNum, int threadId) {
        return stateMap.remove(Arrays.asList(requestNum, threadId));
    }

    public State remove(State state) {
        return stateMap.remove(Arrays.asList(state.requestNum, state.requesterThreadId));
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

        public BroadcastFuture getFuture() {
            return future;
        }

        public Queue<Exception> getExceptions() {
            return exceptions;
        }

        public void downProcessNode(InternalCommonGroup group, CloneInputStream clonedData, String sharedEnumClassName, String name, int[] indices) {
            NodeData nodeData = InternalPCJ.getNodeData();
            int[] threadsId = group.getLocalThreadsId();
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

        public void upProcessNode(InternalCommonGroup group, Queue<Exception> messageExceptions) {
            if ((messageExceptions != null) && (messageExceptions.isEmpty() == false)) {
                exceptions.addAll(messageExceptions);
            }

            nodeProcessed(group);
        }

        private void nodeProcessed(InternalCommonGroup group) {
            NodeData nodeData = InternalPCJ.getNodeData();
            int leftPhysical = notificationCount.decrementAndGet();
            if (leftPhysical == 0) {
                int globalThreadId = group.getGlobalThreadId(requesterThreadId);
                int requesterPhysicalId = nodeData.getPhysicalId(globalThreadId);
                if (requesterPhysicalId != nodeData.getPhysicalId()) { // requester is going to receive response
                    BroadcastStates.this.remove(this);
                }

                Message message;
                SocketChannel socket;

                int physicalId = nodeData.getPhysicalId();
                if (physicalId != group.getGroupMasterNode()) {
                    int parentId = group.getParentNode();
                    socket = nodeData.getSocketChannelByPhysicalId().get(parentId);

                    message = new BroadcastValueInformMessage(group.getGroupId(), requestNum, requesterThreadId, exceptions);
                } else {
                    message = new BroadcastValueResponseMessage(group.getGroupId(), requestNum, requesterThreadId, exceptions);

                    socket = nodeData.getSocketChannelByPhysicalId().get(requesterPhysicalId);
                }

                InternalPCJ.getNetworker().send(socket, message);
            }
        }

        public void signal(Queue<Exception> messageExceptions) {
            if ((messageExceptions != null) && (messageExceptions.isEmpty() == false)) {
                exceptions.addAll(messageExceptions);
            }
            if (exceptions.isEmpty()) {
                future.signalDone();
            } else {
                PcjRuntimeException ex = new PcjRuntimeException("Exception while broadcasting value.");
                exceptions.forEach(ex::addSuppressed);
                future.signalException(ex);
            }
        }
    }
}
