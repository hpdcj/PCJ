/*
 * Copyright (c) 2011-2021, PCJ Library, Marek Nowicki
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
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.pcj.PcjFuture;
import org.pcj.PcjRuntimeException;
import org.pcj.SerializableSupplier;
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
public class CollectStates {

    private final AtomicInteger counter;
    private final ConcurrentMap<List<Integer>, State<?, ?, ?>> stateMap;

    public CollectStates() {
        counter = new AtomicInteger(0);
        stateMap = new ConcurrentHashMap<>();
    }

    public <T, A, R> State<T, A, R> create(int threadId, InternalCommonGroup commonGroup) {
        int requestNum = counter.incrementAndGet();

        NodeData nodeData = InternalPCJ.getNodeData();

        CollectFuture<R> future = new CollectFuture<>();
        State<T, A, R> state = new State<>(requestNum, threadId,
                commonGroup.getCommunicationTree().getChildrenNodes(nodeData.getCurrentNodePhysicalId()).size(),
                future);

        stateMap.put(Arrays.asList(requestNum, threadId), state);

        return state;
    }

    @SuppressWarnings("unchecked")
    public <T, A, R> State<T, A, R> getOrCreate(int requestNum, int requesterThreadId, InternalCommonGroup commonGroup) {
        NodeData nodeData = InternalPCJ.getNodeData();
        int requesterPhysicalId = nodeData.getPhysicalId(commonGroup.getGlobalThreadId(requesterThreadId));
        return (State<T, A, R>) stateMap.computeIfAbsent(Arrays.asList(requestNum, requesterThreadId),
                key -> new State<>(requestNum, requesterThreadId,
                        commonGroup.getCommunicationTree().getChildrenNodes(requesterPhysicalId).size()));
    }

    public State<?, ?, ?> remove(int requestNum, int threadId) {
        return stateMap.remove(Arrays.asList(requestNum, threadId));
    }

    public class State<T, A, R> {

        private final int requestNum;
        private final int requesterThreadId;
        private final AtomicInteger notificationCount;
        private final CollectFuture<R> future;
        private final Queue<A> receivedValues;
        private final Queue<Exception> exceptions;
        private String sharedEnumClassName;
        private String variableName;
        private int[] indices;
        private SerializableSupplier<Collector<T, ?, R>> collectorSupplier;

        private State(int requestNum, int requesterThreadId, int childrenCount, CollectFuture<R> future) {
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

        public PcjFuture<R> getFuture() {
            return future;
        }

        public void downProcessNode(InternalCommonGroup group, String sharedEnumClassName, String variableName, int[] indices, SerializableSupplier<Collector<T, ?, R>> collectorSupplier) {
            this.sharedEnumClassName = sharedEnumClassName;
            this.variableName = variableName;
            this.indices = indices;
            this.collectorSupplier = collectorSupplier;

            CollectRequestMessage<T, ?, R> message = new CollectRequestMessage<T, A, R>(
                    group.getGroupId(), this.requestNum, this.requesterThreadId,
                    sharedEnumClassName, variableName, indices, collectorSupplier
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

        void upProcessNode(InternalCommonGroup group, A receivedValue, Queue<Exception> messageExceptions) {
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
                    CollectStates.this.remove(requestNum, requesterThreadId);
                }

                Collector<T, Object, R> collector = (Collector<T, Object, R>) collectorSupplier.get();
                A resultContainer = (A) collector.supplier().get();
                if (exceptions.isEmpty()) {
                    try {



                        BinaryOperator<A> combiner = (BinaryOperator<A>) collector.combiner();
                        for (A value : receivedValues) {
                            combiner.apply(resultContainer, value);
                        }

                        BiConsumer<A, T> accumulator = (BiConsumer<A, T>) collector.accumulator();
                        for (T value : getCurrentNodeValues(group)) {
                            accumulator.accept(resultContainer, value);
                        }

                    } catch (Exception ex) {
                        exceptions.add(ex);
                    }
                }

                Message message;
                SocketChannel socket;

                int parentId = group.getCommunicationTree().getParentNode(requesterPhysicalId);
                if (parentId >= 0) {
                    message = new CollectResponseMessage<>(group.getGroupId(), requestNum, requesterThreadId, resultContainer, exceptions);
                    socket = nodeData.getSocketChannelByPhysicalId(parentId);
                } else {
                    R result = collector.finisher().apply(resultContainer);
                    message = new CollectValueMessage<>(group.getGroupId(), requestNum, requesterThreadId, result, exceptions);
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

        private List<T> getCurrentNodeValues(InternalCommonGroup group) {
            NodeData nodeData = InternalPCJ.getNodeData();
            Set<Integer> threadsId = group.getLocalThreadsId();

            return threadsId.stream()
                           .map(group::getGlobalThreadId)
                           .map(nodeData::getPcjThread)
                           .map(PcjThread::getThreadData)
                           .map(PcjThreadData::getStorages)
                           .map(storages -> storages.<T>get(this.sharedEnumClassName, this.variableName, this.indices))
                           .collect(Collectors.toList());
        }

        public void signal(R value, Queue<Exception> messageExceptions) {
            if ((messageExceptions != null) && (!messageExceptions.isEmpty())) {
                PcjRuntimeException ex = new PcjRuntimeException("Collecting values failed", messageExceptions.poll());
                messageExceptions.forEach(ex::addSuppressed);
                future.signalException(ex);
            } else {
                future.signalDone(value);
            }
        }


    }
}
