/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.pcj.internal.futures.BarrierState;
import org.pcj.internal.futures.BroadcastState;
import org.pcj.internal.futures.GroupJoinState;

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
    private final int groupId;
    private final String groupName;
    private final CopyOnWriteArrayList<Integer> localIds;
    private final CopyOnWriteArrayList<Integer> physicalIds;
//    final private Bitmask localBarrierBitmask;
    private final Bitmask localBitmask;
//    private final ConcurrentMap<Integer, Bitmask> physicalBitmaskMap;
    private final ConcurrentMap<Integer, BarrierState> barrierStateMap;
    private final ConcurrentMap<List<Integer>, BroadcastState> broadcastStateMap;
    private final ConcurrentMap<List<Integer>, GroupJoinState> groupJoinStateMap;
//    final private MessageGroupBarrierWaiting groupBarrierWaitingMessage;
//    final private ConcurrentMap<Integer, BitMask> joinBitmaskMap;
    final private AtomicInteger threadsCounter;
//    /**
//     * list of local node group ids
//     */
//    final private ArrayList<Integer> localIds;
//    /**
//     * list of remote computers ids in this group (for broadcast)
//     */
//    final private List<Integer> physicalIds;
//    /**
//     * sync
//     */
//    final private BitMask localSync;
//    final private BitMask localSyncMask;
//    final private BitMask physicalSync;
//    /**
//     * Physical Parent, Left, Right
//     */
    final private CommunicationTree physicalTree;

    //private final InternalCommonGroup g;
    public InternalCommonGroup(InternalCommonGroup g) {
        this.groupId = g.groupId;
        this.groupName = g.groupName;
        this.physicalTree = g.physicalTree;

        this.threadsMapping = g.threadsMapping;
//        this.physicalBitmaskMap = g.physicalBitmaskMap;
        this.localBitmask = g.localBitmask;
        this.barrierStateMap = g.barrierStateMap;

        this.broadcastStateMap = g.broadcastStateMap;
        this.groupJoinStateMap = g.groupJoinStateMap;

//        this.groupBarrierWaitingMessage = g.groupBarrierWaitingMessage;
//        this.joinBitmaskMap = g.joinBitmaskMap;
//
//        this.syncMessage = g.syncMessage;
//
        this.localIds = g.localIds;
        this.physicalIds = g.physicalIds;

        this.threadsCounter = g.threadsCounter;
//
//        this.localSync = g.localSync;
//        this.localSyncMask = g.localSyncMask;
    }

    public InternalCommonGroup(int groupMaster, int groupId, String groupName) {
        this.groupId = groupId;
        this.groupName = groupName;
        physicalTree = new CommunicationTree(groupMaster);

        threadsMapping = new ConcurrentHashMap<>();

//        physicalBitmaskMap = new ConcurrentHashMap<>();
        localBitmask = new Bitmask();
        barrierStateMap = new ConcurrentHashMap<>();
        broadcastStateMap = new ConcurrentHashMap<>();
        groupJoinStateMap = new ConcurrentHashMap<>();

//        groupBarrierWaitingMessage = new MessageGroupBarrierWaiting(groupId, InternalPCJ.getNodeData().getPhysicalId());
//        this.joinBitmaskMap = new ConcurrentHashMap<>();
//
//        syncMessage = new MessageSyncWait();
//        syncMessage.setGroupId(groupId);
//
        localIds = new CopyOnWriteArrayList<>();
        physicalIds = new CopyOnWriteArrayList<>();

        threadsCounter = new AtomicInteger(0);

//        localSync = new BitMask();
//        localSyncMask = new BitMask();
//
//        waitObject = new WaitObject();
//
    }

    final protected int getGroupId() {
        return groupId;
    }

    final public String getGroupName() {
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

    protected int myId() {
        throw new IllegalStateException("This method has to be overriden!");
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
        int physicalId = InternalPCJ.getNodeData().getPhysicalId(globalThreadId);
        int groupThreadId;
        do {
            groupThreadId = threadsCounter.getAndIncrement();
        } while (threadsMapping.putIfAbsent(groupThreadId, globalThreadId) != null);

        updateCommunicationTree(physicalId);
        updateLocalBitmask(physicalId, groupThreadId);

        return groupThreadId;
    }

    final public void addThread(int globalThreadId, int groupThreadId) {
        int physicalId = InternalPCJ.getNodeData().getPhysicalId(globalThreadId);
        if (threadsMapping.putIfAbsent(groupThreadId, globalThreadId) != null) {
            return;
        }

        updateCommunicationTree(physicalId);
        updateLocalBitmask(physicalId, groupThreadId);

    }

    private void updateCommunicationTree(int physicalId) {
        synchronized (physicalIds) {
            if (physicalIds.addIfAbsent(physicalId)) {
                int currentPhysicalId = InternalPCJ.getNodeData().getPhysicalId();

                int index = physicalIds.indexOf(physicalId);
                if (index > 0) {
                    if (physicalId == currentPhysicalId) {
                        physicalTree.setParentNode(physicalIds.get((index - 1) / 2));
                    }
                    if (physicalIds.get((index - 1) / 2) == currentPhysicalId) {
                        physicalTree.getChildrenNodes().add(physicalId);
                    }
                }
            }
        }
    }

    private void updateLocalBitmask(int physicalId, int groupThreadId) {
        int currentPhysicalId = InternalPCJ.getNodeData().getPhysicalId();

        synchronized (localIds) {
            if (physicalId == currentPhysicalId) {
                localIds.add(groupThreadId);
                localBitmask.enlarge(groupThreadId + 1);
                localBitmask.set(groupThreadId);
            }
        }
    }

    public Map<Integer, Integer> getThreadsMapping() {
        return Collections.unmodifiableMap(threadsMapping);
    }

    final protected BarrierState barrier(int threadId, int barrierRound) {
        BarrierState barrierState = getBarrierState(barrierRound);
        barrierState.processLocal(threadId);

        return barrierState;
    }

    final public BarrierState getBarrierState(int barrierRound) {
        return barrierStateMap.computeIfAbsent(barrierRound,
                round -> new BarrierState(groupId, round, localBitmask, getChildrenNodes()));
    }

    final public BarrierState removeBarrierState(int barrierRound) {
        return barrierStateMap.remove(barrierRound);
    }

    final public BroadcastState getBroadcastState(int requestNum, int requesterThreadId) {
        List<Integer> key = Arrays.asList(requestNum, requesterThreadId);
        return broadcastStateMap.computeIfAbsent(key,
                k -> new BroadcastState(this.groupId, requestNum, requesterThreadId, getChildrenNodes()));
    }

    final public BroadcastState removeBroadcastState(int requestNum, int requesterThreadId) {
        List<Integer> key = Arrays.asList(requestNum, requesterThreadId);
        return broadcastStateMap.remove(key);
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
