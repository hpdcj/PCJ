/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import org.pcj.internal.message.barrier.BarrierStates;
import org.pcj.internal.message.broadcast.BroadcastStates;
import org.pcj.internal.message.join.GroupJoinStates;

/**
 * Internal (with common ClassLoader) representation of Group. It contains
 * common data for groups.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class InternalCommonGroup {

    public static final int GLOBAL_GROUP_ID = 0;
    public static final String GLOBAL_GROUP_NAME = "";

    private final int groupId;
    private final String groupName;
    private final ConcurrentHashMap<Integer, Integer> threadsMap; // groupThreadId, globalThreadId
    private final AtomicInteger threadsCounter;
    private final Set<Integer> localIds;
    private final CommunicationTree communicationTree;
    private final BarrierStates barrierStates;
    private final BroadcastStates broadcastStates;
    private final GroupJoinStates groupJoinStates;

    public InternalCommonGroup(InternalCommonGroup g) {
        this.groupId = g.groupId;
        this.groupName = g.groupName;
        this.communicationTree = g.communicationTree;

        this.threadsMap = g.threadsMap;
        this.threadsCounter = g.threadsCounter;
        this.localIds = g.localIds;

        this.barrierStates = g.barrierStates;
        this.broadcastStates = g.broadcastStates;
        this.groupJoinStates = g.groupJoinStates;
    }

    public InternalCommonGroup(int groupMasterNode, int groupId, String groupName) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.communicationTree = new CommunicationTree(groupMasterNode);

        this.threadsMap = new ConcurrentHashMap<>();
        this.threadsCounter = new AtomicInteger(0);
        this.localIds = new CopyOnWriteArraySet<>();

        this.barrierStates = new BarrierStates();
        this.broadcastStates = new BroadcastStates();
        this.groupJoinStates = new GroupJoinStates();
    }

    final public int getGroupId() {
        return groupId;
    }

    final public String getName() {
        return groupName;
    }

    final public int threadCount() {
        return threadsMap.size();
    }

    final public Set<Integer> getLocalThreadsId() {
        return Collections.unmodifiableSet(localIds);
    }

    final public int getGlobalThreadId(int groupThreadId) throws NoSuchElementException {
        Integer globalThreadId = threadsMap.get(groupThreadId);
        if (globalThreadId == null) {
            throw new NoSuchElementException("Group threadId not found: " + groupThreadId);
        }
        return globalThreadId;
    }

    final public int getGroupThreadId(int globalThreadId) throws NoSuchElementException {
        return threadsMap.entrySet().stream()
                       .filter(entry -> entry.getValue() == globalThreadId)
                       .mapToInt(Map.Entry::getKey)
                       .findFirst()
                       .orElseThrow(() -> new NoSuchElementException("Global threadId not found: " + globalThreadId));
    }

    final public int addNewThread(int globalThreadId) {
        int groupThreadId;
        do {
            groupThreadId = threadsCounter.getAndIncrement();
        } while (threadsMap.putIfAbsent(groupThreadId, globalThreadId) != null);

        updateLocalThreads();
        communicationTree.update(threadsMap);

        return groupThreadId;
    }

    final public void updateThreadsMap(Map<Integer, Integer> newThreadsMap) { // groupId, globalId
        threadsMap.putAll(newThreadsMap);

        updateLocalThreads();
        communicationTree.update(this.threadsMap);
    }

    private void updateLocalThreads() {
        int currentPhysicalId = InternalPCJ.getNodeData().getPhysicalId();
        threadsMap.entrySet()
                .stream()
                .filter(entry -> entry.getValue() == currentPhysicalId)
                .map(Map.Entry::getKey)
                .sorted()
                .forEach(localIds::add);
    }

    public Map<Integer, Integer> getThreadsMap() {
        return Collections.unmodifiableMap(threadsMap);
    }

    public BarrierStates getBarrierStates() {
        return barrierStates;
    }

    final public BroadcastStates getBroadcastStates() {
        return broadcastStates;
    }

    public GroupJoinStates getGroupJoinStates() {
        return groupJoinStates;
    }

    public CommunicationTree getCommunicationTree() {
        return communicationTree;
    }

    public static class CommunicationTree {

        private final int masterNode;
        private int parentNode;
        private final Set<Integer> childrenNodes;

        public CommunicationTree(int masterNode) {
            this.masterNode = masterNode;
            this.parentNode = -1;
            this.childrenNodes = new CopyOnWriteArraySet<>();
        }

        final public int getMasterNode() {
            return masterNode;
        }

        final public int getParentNode() {
            return parentNode;
        }

        final public Set<Integer> getChildrenNodes() {
            return Collections.unmodifiableSet(childrenNodes);
        }

        private void update(Map<Integer, Integer> threadsMapping) {
            NodeData nodeData = InternalPCJ.getNodeData();

            Set<Integer> physicalIdsSet = new LinkedHashSet<>();
            physicalIdsSet.add(masterNode);
            threadsMapping.keySet().stream()
                    .sorted()
                    .map(threadsMapping::get)
                    .map(nodeData::getPhysicalId)
                    .forEach(physicalIdsSet::add);
            List<Integer> physicalIds = new ArrayList<>(physicalIdsSet);

            int currentPhysicalId = InternalPCJ.getNodeData().getPhysicalId();
            int currentIndex = physicalIds.indexOf(currentPhysicalId);

            if (currentIndex > 0) {
                parentNode = physicalIds.get((currentIndex - 1) / 2);
            }
            if (currentIndex * 2 + 1 < physicalIds.size()) {
                childrenNodes.add(physicalIds.get(currentIndex * 2 + 1));
            }
            if (currentIndex * 2 + 2 < physicalIds.size()) {
                childrenNodes.add(physicalIds.get(currentIndex * 2 + 2));
            }
        }
    }
}
