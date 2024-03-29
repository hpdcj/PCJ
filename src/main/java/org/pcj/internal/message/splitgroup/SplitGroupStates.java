/*
 * Copyright (c) 2011-2021, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.splitgroup;

import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.pcj.Group;
import org.pcj.PcjFuture;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.PcjThread;
import org.pcj.internal.message.Message;

/*
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class SplitGroupStates {

    private final ConcurrentMap<Integer, AtomicInteger> counterMap;
    private final ConcurrentMap<Integer, State> stateMap;

    public SplitGroupStates() {
        counterMap = new ConcurrentHashMap<>();
        stateMap = new ConcurrentHashMap<>();
    }

    public int getNextRound(int threadId) {
        AtomicInteger roundCounter = counterMap.computeIfAbsent(threadId, key -> new AtomicInteger(0));
        return roundCounter.incrementAndGet();
    }

    public State getOrCreate(int round, InternalCommonGroup commonGroup) {
        return stateMap.computeIfAbsent(round,
                _round -> new State(_round, commonGroup.getLocalThreadsId().size(), commonGroup.getCommunicationTree().getChildrenNodes().size()));
    }

    public State remove(int round) {
        return stateMap.remove(round);
    }

    public static class State {
        private final int round;
        private final AtomicReference<NotificationCount> notificationCount;
        private final AtomicInteger readyToGoNotificationCount;
        private final ConcurrentMap<Integer, SplitGroupFuture> futureMap;
        private final ConcurrentMap<Integer, Integer> splitMap;
        private final ConcurrentMap<Integer, Integer> orderingMap;

        private State(int round, int localCount, int childrenCount) {
            this.round = round;

            futureMap = new ConcurrentHashMap<>();
            splitMap = new ConcurrentHashMap<>();
            orderingMap = new ConcurrentHashMap<>();

            notificationCount = new AtomicReference<>(new NotificationCount(localCount, childrenCount));
            readyToGoNotificationCount = new AtomicInteger(childrenCount + 1);
        }

        public PcjFuture<Group> getFuture(int threadId) {
            return futureMap.get(threadId);
        }

        public void processLocal(InternalCommonGroup group, int threadId, Integer split, int ordering) {
            futureMap.put(threadId, new SplitGroupFuture());
            if (split != null) {
                splitMap.put(threadId, split);
                orderingMap.put(threadId, ordering);
            } else {
                futureMap.get(threadId).signalDone();
            }

            NotificationCount count = notificationCount.updateAndGet(
                    old -> new NotificationCount(old.local - 1, old.physical));

            if (count.isDone()) {
                nodeProcessed(group);
            }
        }

        protected void processPhysical(InternalCommonGroup group,
                                       Map<Integer, Integer> splitMap, Map<Integer, Integer> orderingMap) {
            this.splitMap.putAll(splitMap);
            this.orderingMap.putAll(orderingMap);

            NotificationCount count = notificationCount.updateAndGet(
                    old -> new NotificationCount(old.local, old.physical - 1));

            if (count.isDone()) {
                nodeProcessed(group);
            }
        }

        private void nodeProcessed(InternalCommonGroup group) {
            Message message;
            SocketChannel socket;
            NodeData nodeData = InternalPCJ.getNodeData();

            int parentId = group.getCommunicationTree().getParentNode();
            if (group.getCommunicationTree().getParentNode() >= 0) {
                message = new SplitGroupRequestMessage(group.getGroupId(), round, splitMap, orderingMap);
                socket = nodeData.getSocketChannelByPhysicalId(parentId);
            } else {
                int splitCount = (int) splitMap.values().stream().distinct().count();
                message = new SplitGroupQueryMessage(group.getGroupId(), round, splitCount);
                socket = nodeData.getNode0Socket();
            }

            InternalPCJ.getNetworker().send(socket, message);
        }

        protected void signalDone() {
            futureMap.values().forEach(SplitGroupFuture::signalDone);
        }

        protected void groupIdsAnswer(InternalCommonGroup group, int[] groupIds) {
            int[] splitIds = splitMap.values().stream().mapToInt(Integer::intValue).distinct().toArray();
            // mapping: split number -> group id
            Map<Integer, Integer> splitNumToGroupIdMap = IntStream.range(0, groupIds.length)
                                                                 .collect(HashMap::new,
                                                                         (map, key) -> map.put(splitIds[key], groupIds[key]),
                                                                         Map::putAll);

            // mapping: groupId -> List of thread groupId
            Map<Integer, List<Integer>> threadGroupIdMap
                    = orderingMap.entrySet().stream()
                              .sorted(Map.Entry.<Integer, Integer>comparingByValue()
                                              .thenComparing(Map.Entry.comparingByKey()))
                              .map(Map.Entry::getKey)
                              .collect(Collectors.groupingBy(threadId -> splitNumToGroupIdMap.get(splitMap.get(threadId))));

            createGroups(group, threadGroupIdMap);
        }

        protected void createGroups(InternalCommonGroup group, Map<Integer, List<Integer>> threadGroupIdMap) {
            NodeData nodeData = InternalPCJ.getNodeData();

            Message message = new SplitGroupResponseMessage(group.getGroupId(), round, threadGroupIdMap);
            group.getCommunicationTree().getChildrenNodes().stream()
                    .map(nodeData::getSocketChannelByPhysicalId)
                    .forEach(socket -> InternalPCJ.getNetworker().send(socket, message));


            Set<Integer> localThreadsId = group.getLocalThreadsId();
            // mapping: thread groupId -> groupId
            for (Map.Entry<Integer, List<Integer>> entry : threadGroupIdMap.entrySet()) {
                if (Collections.disjoint(localThreadsId, entry.getValue())) {
                    continue;
                }

                InternalCommonGroup newCommonGroup = nodeData.getOrCreateCommonGroup(entry.getKey());

                Map<Integer, Integer> groupIdGlobalIdMap = new HashMap<>();
                int newThreadId = 0;
                for (int threadId : entry.getValue()) {
                    int globalThreadId = group.getGlobalThreadId(threadId);
                    groupIdGlobalIdMap.put(newThreadId, globalThreadId);

                    if (localThreadsId.contains(threadId)) {
                        InternalGroup threadGroup = new InternalGroup(newThreadId, newCommonGroup);
                        PcjThread pcjThread = nodeData.getPcjThread(globalThreadId);
                        pcjThread.getThreadData().addGroup(threadGroup);
                        futureMap.get(threadId).setGroup(threadGroup);
                    }

                    ++newThreadId;
                }
                newCommonGroup.updateThreadsMap(groupIdGlobalIdMap);
            }

            readyToGo(group);
        }

        protected void readyToGo(InternalCommonGroup group) {
            int leftPhysical = readyToGoNotificationCount.decrementAndGet();
            if (leftPhysical == 0) {
                NodeData nodeData = InternalPCJ.getNodeData();

                Message message;
                SocketChannel socket;

                int parentId = group.getCommunicationTree().getParentNode();
                if (parentId >= 0) {
                    message = new SplitGroupWaitingMessage(group.getGroupId(), round);
                    socket = nodeData.getSocketChannelByPhysicalId(parentId);
                } else {
                    message = new SplitGroupGoMessage(group.getGroupId(), round);
                    socket = nodeData.getSocketChannelByPhysicalId(nodeData.getCurrentNodePhysicalId());
                }

                InternalPCJ.getNetworker().send(socket, message);
            }
        }

        private static class NotificationCount {

            private final int local;
            private final int physical;

            public NotificationCount(int local, int physical) {
                this.local = local;
                this.physical = physical;
            }

            boolean isDone() {
                return local == 0 && physical == 0;
            }
        }
    }
}
