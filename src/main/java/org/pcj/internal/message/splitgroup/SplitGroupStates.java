/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.splitgroup;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.pcj.Group;
import org.pcj.PcjFuture;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
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
        private final ConcurrentMap<Integer, SplitGroupFuture> futureMap;
        private final ConcurrentMap<Integer, Integer> splitMap;
        private final ConcurrentMap<Integer, Integer> orderingMap;

        private State(int round, int localCount, int physicalCount) {
            this.round = round;

            futureMap = new ConcurrentHashMap<>();
            splitMap = new ConcurrentHashMap<>();
            orderingMap = new ConcurrentHashMap<>();

            notificationCount = new AtomicReference<>(new NotificationCount(localCount, physicalCount));
        }

        public PcjFuture<Group> getFuture(int threadId) {
            return futureMap.get(threadId);
        }

        public void processLocal(InternalCommonGroup group, int threadId, Integer split, int ordering) {
            futureMap.put(threadId, new SplitGroupFuture());
            if (split != null) {
                splitMap.put(group.getGlobalThreadId(threadId), split);
                orderingMap.put(group.getGlobalThreadId(threadId), ordering);
            } else {
                futureMap.get(threadId).signalDone(null);
            }

            NotificationCount count = notificationCount.updateAndGet(
                    old -> new NotificationCount(old.local - 1, old.physical));

            if (count.isDone()) {
                nodeProcessed(group);
            }
        }

        public void processPhysical(InternalCommonGroup group,
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

        public void signalDone() {
            futureMap.values().forEach(future -> future.signalDone(null)); // TODO: rzeczywista grupa
        }

        public void groupIdsAnswer(InternalCommonGroup group, int[] groupIds) {
            int[] splitIds = splitMap.values().stream().mapToInt(Integer::intValue).distinct().toArray();
            // mapping: split number -> group id
            Map<Integer, Integer> splitNumToGroupIdMap = IntStream.range(0, groupIds.length)
                    .collect(HashMap::new,
                            (map, key) -> map.put(splitIds[key], groupIds[key]),
                            Map::putAll);

            // mapping: groupId -> List of thread globalId
            Map<Integer, List<Integer>> threadGroupIdMap
                    = orderingMap.entrySet().stream()
                    .sorted(Map.Entry.<Integer, Integer>comparingByValue()
                            .thenComparing(Map.Entry.comparingByKey()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.groupingBy(threadId -> splitNumToGroupIdMap.get(splitMap.get(threadId))));



            NodeData nodeData = InternalPCJ.getNodeData();

//            Message message = new SplitGroupResponseMessage(group.getGroupId(), round, threadGroupIdMap);
//            SocketChannel socket = nodeData.getSocketChannelByPhysicalId(nodeData.getCurrentNodePhysicalId());
//
//            InternalPCJ.getNetworker().send(socket, message);
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
