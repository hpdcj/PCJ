/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.pcj.internal.futures.BarrierState;
import org.pcj.internal.futures.BroadcastState;
import org.pcj.internal.message.MessageGroupBarrierGo;
import org.pcj.internal.message.MessageGroupBarrierWaiting;

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
    private final List<Integer> localIds;
    private final List<Integer> physicalIds;
//    final private Bitmask localBarrierBitmask;
    private final Bitmask localBitmask;
//    private final ConcurrentMap<Integer, Bitmask> physicalBitmaskMap;
    private final ConcurrentMap<Integer, BarrierState> barrierStateMap;
    private final ConcurrentMap<List<Integer>, BroadcastState> broadcastStateMap;
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

//        groupBarrierWaitingMessage = new MessageGroupBarrierWaiting(groupId, InternalPCJ.getNodeData().getPhysicalId());
//        this.joinBitmaskMap = new ConcurrentHashMap<>();
//
//        syncMessage = new MessageSyncWait();
//        syncMessage.setGroupId(groupId);
//
        localIds = new ArrayList<>();
        physicalIds = new ArrayList<>();

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

    final protected String getGroupName() {
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

    final public int getGlobalThreadId(int groupThreadId) {
        Integer globalThreadId = threadsMapping.get(groupThreadId);
        if (globalThreadId == null) {
            throw new IllegalArgumentException("Invail group threadId: " + groupThreadId);
        }
        return globalThreadId;
    }

    final public void addThread(int physicalId, int groupThreadId, int globalThreadId) {
        int currentPhysicalId = InternalPCJ.getNodeData().getPhysicalId();

        if (threadsMapping.put(groupThreadId, globalThreadId) != null) {
            return;
        }
        
        synchronized (physicalIds) {
            if (physicalIds.contains(physicalId) == false) {
                physicalIds.add(physicalId);

                int index = physicalIds.size() - 1;
                if (index > 0) {
                    if (physicalId == currentPhysicalId) {
                        physicalTree.setParentNode((index - 1) / 2);
                    }
                    if (physicalIds.get((index - 1) / 2) == currentPhysicalId) {
                        physicalTree.getChildrenNodes().add(physicalId);
                    }
                }
            }
        }

        if (physicalId == currentPhysicalId) {
            synchronized (localIds) {
                localIds.add(groupThreadId);
                if (groupThreadId + 1 > localBitmask.getSize()) {
                    localBitmask.setSize(groupThreadId + 1);
                }
                localBitmask.set(groupThreadId);
            }
        }
    }

    final public int getNextGroupThreadId() {
        int groupThreadId = threadsCounter.getAndIncrement();
        while (threadsMapping.containsKey(groupThreadId)) {
            groupThreadId = threadsCounter.getAndIncrement();
        }
        return groupThreadId;
    }

    final protected BarrierState barrier(int threadId, int barrierRound) {
        BarrierState barrierState = getBarrierState(barrierRound);

        synchronized (barrierState) {
            barrierState.setLocal(threadId);

            if (barrierState.isLocalSet() && barrierState.isPhysicalSet()) {
                int physicalId = InternalPCJ.getNodeData().getPhysicalId();
                if (physicalId == this.getGroupMasterNode()) {
                    MessageGroupBarrierGo messageGroupBarrierGo = new MessageGroupBarrierGo(groupId, barrierRound);

                    int groupMasterId = this.getGroupMasterNode();
                    SocketChannel groupMasterSocket = InternalPCJ.getNodeData()
                            .getSocketChannelByPhysicalId().get(groupMasterId);

                    InternalPCJ.getNetworker().send(groupMasterSocket, messageGroupBarrierGo);
                } else {
                    int parentId = this.getParentNode();
                    SocketChannel parentSocket = InternalPCJ.getNodeData()
                            .getSocketChannelByPhysicalId().get(parentId);

                    MessageGroupBarrierWaiting message = new MessageGroupBarrierWaiting(
                            groupId, barrierRound, physicalId);

                    InternalPCJ.getNetworker().send(parentSocket, message);
                }
            }
        }

        return barrierState;
    }

    final public BarrierState getBarrierState(int barrierRound) {
        return barrierStateMap.computeIfAbsent(barrierRound,
                round -> new BarrierState(round, localBitmask, getChildrenNodes()));
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
            childrenNodes = new ArrayList<>();
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
