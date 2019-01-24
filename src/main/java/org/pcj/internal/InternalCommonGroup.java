/* 
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.pcj.internal.message.barrier.BarrierStates;
import org.pcj.internal.message.broadcast.BroadcastStates;
import org.pcj.internal.message.join.GroupJoinState;

/**
 * Internal (with common ClassLoader) representation of Group. It contains
 * common data for groups.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class InternalCommonGroup {

    public static final int GLOBAL_GROUP_ID = 0;
    public static final String GLOBAL_GROUP_NAME = "";

    private final ConcurrentMap<Integer, Integer> threadsMapping; // groupThreadId, globalThreadId
    private final Object joinGroupSynchronizer;
    private final int groupId;
    private final String groupName;
    private final List<Integer> localIds;
    private final List<Integer> physicalIds;
    private final Bitmask localBitmask;
    private final Bitmask physicalBitmask;
    final private AtomicInteger threadsCounter;
    final private CommunicationTree physicalTree;
    private final BarrierStates barrierStates;
    private final BroadcastStates broadcastStates;
    private final ConcurrentMap<List<Integer>, GroupJoinState> groupJoinStateMap;

    public InternalCommonGroup(InternalCommonGroup g) {
        this.groupId = g.groupId;
        this.groupName = g.groupName;
        this.physicalTree = g.physicalTree;

        this.threadsMapping = g.threadsMapping;
        this.localBitmask = g.localBitmask;
        this.physicalBitmask = g.physicalBitmask;

        this.barrierStates = g.barrierStates;
        this.broadcastStates = g.broadcastStates;
        this.groupJoinStateMap = g.groupJoinStateMap;

        this.localIds = g.localIds;
        this.physicalIds = g.physicalIds;

        this.threadsCounter = g.threadsCounter;
        this.joinGroupSynchronizer = g.joinGroupSynchronizer;
    }

    public InternalCommonGroup(int groupMaster, int groupId, String groupName) {
        this.groupId = groupId;
        this.groupName = groupName;
        physicalTree = new CommunicationTree(groupMaster);

        threadsMapping = new ConcurrentHashMap<>();

        localBitmask = new Bitmask();
        physicalBitmask = new Bitmask();

        barrierStates = new BarrierStates();
        broadcastStates = new BroadcastStates();
        groupJoinStateMap = new ConcurrentHashMap<>();

        localIds = new ArrayList<>();
        physicalIds = new CopyOnWriteArrayList<>();
        updateCommunicationTree(groupMaster);

        threadsCounter = new AtomicInteger(0);
        joinGroupSynchronizer = new Object();
    }

    final public int getGroupId() {
        return groupId;
    }

    final public String getName() {
        return groupName;
    }

    final public int getGroupMasterNode() {
        return physicalTree.getRootNode();
    }

    final public int getParentNode() {
        return physicalTree.getParentNode();
    }

    final public List<Integer> getChildrenNodes() {
        return physicalTree.getChildrenNodes();
    }

    final public int threadCount() {
        return threadsMapping.size();
    }

    final public int[] getLocalThreadsId() {
        return localIds.stream().mapToInt(Integer::intValue).toArray();
    }

    final public int getGlobalThreadId(int groupThreadId) throws NoSuchElementException {
        Integer globalThreadId = threadsMapping.get(groupThreadId);
        if (globalThreadId == null) {
            throw new NoSuchElementException("Group threadId not found: " + groupThreadId);
        }
        return globalThreadId;
    }

    final public int getGroupThreadId(int globalThreadId) throws NoSuchElementException {
        if (threadsMapping.containsValue(globalThreadId)) {
            for (Map.Entry<Integer, Integer> entry : threadsMapping.entrySet()) {
                if (entry.getValue() == globalThreadId) {
                    return entry.getKey();
                }
            }
        }
        throw new NoSuchElementException("Global threadId not found: " + globalThreadId);
    }

    final public int addNewThread(int globalThreadId) {
        int groupThreadId;
        int physicalId = InternalPCJ.getNodeData().getPhysicalId(globalThreadId);
        synchronized (joinGroupSynchronizer) {
            do {
                groupThreadId = threadsCounter.getAndIncrement();
            } while (threadsMapping.putIfAbsent(groupThreadId, globalThreadId) != null);

            updateCommunicationTree(physicalId);
            updateLocalBitmask(physicalId, groupThreadId);
        }

        return groupThreadId;
    }

    final public void addThread(int globalThreadId, int groupThreadId) {
        int physicalId = InternalPCJ.getNodeData().getPhysicalId(globalThreadId);
        synchronized (joinGroupSynchronizer) {
            if (threadsMapping.putIfAbsent(groupThreadId, globalThreadId) != null) {
                return;
            }

            updateCommunicationTree(physicalId);
            updateLocalBitmask(physicalId, groupThreadId);
        }

    }

    private void updateCommunicationTree(int physicalId) {
        if (physicalIds.contains(physicalId)) {
            return;
        }

        physicalIds.add(physicalId);
        int index = physicalIds.lastIndexOf(physicalId);
        if (index > 0) {
            int currentPhysicalId = InternalPCJ.getNodeData().getPhysicalId();
            int parentId = physicalIds.get((index - 1) / 2);

            if (currentPhysicalId == physicalId) {
                physicalTree.setParentNode(parentId);
            }
            if (currentPhysicalId == parentId) {
                physicalTree.getChildrenNodes().add(physicalId);
            }
        }

        physicalBitmask.enlarge(physicalId + 1);
        physicalBitmask.set(physicalId);
    }

    private void updateLocalBitmask(int physicalId, int groupThreadId) {
        int currentPhysicalId = InternalPCJ.getNodeData().getPhysicalId();

        if (physicalId == currentPhysicalId) {
            localIds.add(groupThreadId);
            localBitmask.enlarge(groupThreadId + 1);
            localBitmask.set(groupThreadId);
        }
    }

    public Map<Integer, Integer> getThreadsMapping() {
        return Collections.unmodifiableMap(threadsMapping);
    }

    public BarrierStates getBarrierStates() {
        return barrierStates;
    }

    final public BroadcastStates getBroadcastStates() {
        return broadcastStates;
    }

    public GroupJoinState getGroupJoinState(int requestNum, int threadId, List<Integer> childrenNodes) {
        return groupJoinStateMap.computeIfAbsent(Arrays.asList(requestNum, threadId),
                key -> new GroupJoinState(groupId, requestNum, threadId, childrenNodes));
    }

    public GroupJoinState removeGroupJoinState(int requestNum, int threadId) {
        return groupJoinStateMap.remove(Arrays.asList(requestNum, threadId));
    }


    /**
     * Class for representing part of communication tree.
     *
     * @author Marek Nowicki (faramir@mat.umk.pl)
     */
    public static class CommunicationTree {

        private final int rootNode;
        private int parentNode;
        private final List<Integer> childrenNodes;

        public CommunicationTree(int rootNode) {
            this.rootNode = rootNode;
            this.parentNode = -1;
            childrenNodes = new CopyOnWriteArrayList<>();
        }

        public int getRootNode() {
            return rootNode;
        }

        public void setParentNode(int parentNode) {
            this.parentNode = parentNode;
        }

        public int getParentNode() {
            return parentNode;
        }

        public List<Integer> getChildrenNodes() {
            return childrenNodes;
        }
    }
}
