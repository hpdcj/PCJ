/*
 * Copyright (c) 2011-2021, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.pcj.internal.message.barrier.BarrierStates;
import org.pcj.internal.message.broadcast.BroadcastStates;
import org.pcj.internal.message.gather.GatherStates;
import org.pcj.internal.message.reduce.ReduceStates;
import org.pcj.internal.message.scatter.ScatterStates;
import org.pcj.internal.message.splitgroup.SplitGroupStates;

/**
 * Internal (with common ClassLoader) representation of Group. It contains
 * common data for groups.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class InternalCommonGroup {

    public static final int GLOBAL_GROUP_ID = 0;

    private final int groupId;
    private final ConcurrentHashMap<Integer, Integer> threadsMap; // groupThreadId, globalThreadId
    private final AtomicInteger threadsCounter;
    private final Set<Integer> localIds;
    private final CommunicationTree communicationTree;
    private final BarrierStates barrierStates;
    private final BroadcastStates broadcastStates;
    private final ScatterStates scatterStates;
    private final GatherStates gatherStates;
    private final ReduceStates reduceStates;
    private final SplitGroupStates splitGroupStates;

    public InternalCommonGroup(InternalCommonGroup g) {
        this.groupId = g.groupId;
        this.communicationTree = g.communicationTree;

        this.threadsMap = g.threadsMap;
        this.threadsCounter = g.threadsCounter;
        this.localIds = g.localIds;

        this.barrierStates = g.barrierStates;
        this.broadcastStates = g.broadcastStates;
        this.scatterStates = g.scatterStates;
        this.gatherStates = g.gatherStates;
        this.reduceStates = g.reduceStates;
        this.splitGroupStates = g.splitGroupStates;
    }

    public InternalCommonGroup(int groupId) {
        this.groupId = groupId;
        this.communicationTree = new CommunicationTree();

        this.threadsMap = new ConcurrentHashMap<>();
        this.threadsCounter = new AtomicInteger(0);
        this.localIds = ConcurrentHashMap.newKeySet();

        this.barrierStates = new BarrierStates();
        this.broadcastStates = new BroadcastStates();
        this.scatterStates = new ScatterStates();
        this.gatherStates = new GatherStates();
        this.reduceStates = new ReduceStates();
        this.splitGroupStates = new SplitGroupStates();
    }

    public final int getGroupId() {
        return groupId;
    }

    public final int threadCount() {
        return threadsMap.size();
    }

    public final Set<Integer> getLocalThreadsId() {
        return Collections.unmodifiableSet(localIds);
    }

    public final int getGlobalThreadId(int groupThreadId) throws NoSuchElementException {
        Integer globalThreadId = threadsMap.get(groupThreadId);
        if (globalThreadId == null) {
            throw new NoSuchElementException("Group threadId not found: " + groupThreadId);
        }
        return globalThreadId;
    }

    public final void updateThreadsMap(Map<Integer, Integer> newThreadsMap) { // groupId, globalId
        threadsMap.putAll(newThreadsMap);

        updateLocalThreads();
        communicationTree.update();
    }

    private void updateLocalThreads() {
        NodeData nodeData = InternalPCJ.getNodeData();
        int currentPhysicalId = nodeData.getCurrentNodePhysicalId();
        threadsMap.entrySet()
                .stream()
                .filter(entry -> nodeData.getPhysicalId(entry.getValue()) == currentPhysicalId)
                .map(Map.Entry::getKey)
                .sorted()
                .forEach(localIds::add);
    }

    /**
     * Map with current group threads id mapped to the global threads id.
     * <p>
     * key: groupThreadId; value: globalThreadId
     *
     * @return map with thread mapping
     */
    public Map<Integer, Integer> getThreadsMap() {
        return Collections.unmodifiableMap(threadsMap);
    }

    public BarrierStates getBarrierStates() {
        return barrierStates;
    }

    public final BroadcastStates getBroadcastStates() {
        return broadcastStates;
    }

    public final ScatterStates getScatterStates() {
        return scatterStates;
    }

    public final GatherStates getGatherStates() {
        return gatherStates;
    }

    public final ReduceStates getReduceStates() {
        return reduceStates;
    }

    public SplitGroupStates getSplitGroupStates() {
        return splitGroupStates;
    }

    public CommunicationTree getCommunicationTree() {
        return communicationTree;
    }

    public class CommunicationTree {

        private List<Integer> physicalIds;
        private int currentIndex;

        private CommunicationTree() {
            this.currentIndex = -1;
            this.physicalIds = Collections.emptyList();
        }

        public int getParentNode() {
            return getParentNode(0);
        }

        public int getParentNode(int shift) {
            if (currentIndex < 0) {
                return -1;
            }
            if (currentIndex == shift) { // if root after shift
                return -1;
            }
            int index = (((currentIndex - shift - 1 + physicalIds.size()) % physicalIds.size()) / 2 + shift) % physicalIds.size();
            return physicalIds.get(index);
        }

        public Set<Integer> getChildrenNodes() {
            return getChildrenNodes(0);
        }

        public Set<Integer> getChildrenNodes(int shift) {
            if (currentIndex < 0) {
                return Collections.emptySet();
            }
            Set<Integer> children = new HashSet<>();
            int shiftIndex = (currentIndex - shift + physicalIds.size()) % physicalIds.size();
            if (shiftIndex * 2 + 1 < physicalIds.size()) {
                children.add(physicalIds.get((shiftIndex * 2 + 1 + shift) % physicalIds.size()));
            }
            if (shiftIndex * 2 + 2 < physicalIds.size()) {
                children.add(physicalIds.get((shiftIndex * 2 + 2 + shift) % physicalIds.size()));
            }
            return children;
        }

        public List<Integer> getSubtree(int shift, int subTreeRootNode) {
            List<Integer> subTree = new ArrayList<>();

            Queue<Integer> queue = new ArrayDeque<>();
            queue.offer(physicalIds.indexOf(subTreeRootNode));
            while (!queue.isEmpty()) {
                int index = queue.poll();
                subTree.add(physicalIds.get(index));
                int shiftIndex = (index - shift + physicalIds.size()) % physicalIds.size();
                if (shiftIndex * 2 + 1 < physicalIds.size()) {
                    queue.offer((shiftIndex * 2 + 1 + shift) % physicalIds.size());
                }
                if (shiftIndex * 2 + 2 < physicalIds.size()) {
                    queue.offer((shiftIndex * 2 + 2 + shift) % physicalIds.size());
                }
            }

            return subTree;
        }

        private void update() {
            NodeData nodeData = InternalPCJ.getNodeData();

            physicalIds = threadsMap.keySet().stream()
                    .sorted()
                    .map(threadsMap::get)
                    .map(nodeData::getPhysicalId)
                    .distinct()
                    .collect(Collectors.toList());

            currentIndex = physicalIds.indexOf(nodeData.getCurrentNodePhysicalId());
        }
    }
}
