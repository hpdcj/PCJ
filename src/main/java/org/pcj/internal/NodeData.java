/* 
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal;

import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.pcj.internal.futures.GroupJoinQuery;
import org.pcj.internal.futures.WaitObject;

/**
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class NodeData {

    private final SocketChannel node0Socket;
    private final ConcurrentMap<Integer, InternalCommonGroup> groupById;
    private final ConcurrentMap<Integer, SocketChannel> socketChannelByPhysicalId; // physicalId -> socket
    private final ConcurrentMap<Integer, Integer> physicalIdByThreadId; // threadId -> physicalId
    private final ConcurrentMap<Integer, PcjThread> pcjThreads;
    private final Node0Data node0Data;
    private final WaitObject globalWaitObject;
    private final AtomicInteger groupJoinCounter;
    private final ConcurrentMap<Integer, GroupJoinQuery> groupJoinQueryMap;
    private int physicalId;
    private int totalNodeCount;

    public static class Node0Data {

        private final AtomicInteger connectedNodeCount;
        private final AtomicInteger connectedThreadCount;
        private final Bitmask helloBitmask;
        private final Bitmask finishedBitmask;
        private final AtomicInteger groupIdCounter;
        private final ConcurrentMap<String, Integer> groupsId; // groupName -> groupId
        private final ConcurrentMap<Integer, Integer> groupsMaster; // groupId -> physicalId
        private final ConcurrentMap<Integer, NodeInfo> nodeInfoByPhysicalId; // physicalId -> nodeInfo
        private int allNodesThreadCount;

        public Node0Data() {
            this.connectedNodeCount = new AtomicInteger(0);
            this.connectedThreadCount = new AtomicInteger(0);
            this.helloBitmask = new Bitmask();
            this.finishedBitmask = new Bitmask();

            this.groupIdCounter = new AtomicInteger(1);
            this.groupsId = new ConcurrentHashMap<>();
            this.groupsMaster = new ConcurrentHashMap<>();
            this.nodeInfoByPhysicalId = new ConcurrentHashMap<>();

            groupsId.put("", 0);
            groupsMaster.put(0, 0);
        }

        public int getAllNodesThreadCount() {
            return allNodesThreadCount;
        }

        void setAllNodesThreadCount(int allNodesThreadCount) {
            this.allNodesThreadCount = allNodesThreadCount;
        }

        public AtomicInteger getConnectedNodeCount() {
            return connectedNodeCount;
        }

        public AtomicInteger getConnectedThreadCount() {
            return connectedThreadCount;
        }

        public ConcurrentMap<Integer, NodeInfo> getNodeInfoByPhysicalId() {
            return nodeInfoByPhysicalId;
        }

        public Bitmask getHelloBitmask() {
            return helloBitmask;
        }

        public Bitmask getFinishedBitmask() {
            return finishedBitmask;
        }

        public int getGroupId(String name) {
            synchronized (groupsId) {
                return groupsId.computeIfAbsent(name, key -> groupIdCounter.getAndIncrement());
            }
        }

        public int getGroupMaster(int groupId, int physicalId) {
            return groupsMaster.computeIfAbsent(groupId, key -> physicalId);
        }

        public ConcurrentMap<Integer, Integer> getPhysicalIdByThreadId() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    public NodeData(SocketChannel node0Socket, boolean isCurrentJvmNode0) {
        this.node0Socket = node0Socket;
        this.groupById = new ConcurrentHashMap<>();
        this.socketChannelByPhysicalId = new ConcurrentHashMap<>();
        this.physicalIdByThreadId = new ConcurrentHashMap<>();
        this.pcjThreads = new ConcurrentHashMap<>();
        this.globalWaitObject = new WaitObject();
        this.groupJoinCounter = new AtomicInteger(0);
        this.groupJoinQueryMap = new ConcurrentHashMap<>();

        if (isCurrentJvmNode0) {
            node0Data = new Node0Data();
        } else {
            node0Data = null;
        }
    }

    public Node0Data getNode0Data() {
        return node0Data;
    }

    public SocketChannel getNode0Socket() {
        return node0Socket;
    }

    public InternalCommonGroup createGroup(int groupMaster, int groupId, String groupName) {
        return groupById.computeIfAbsent(groupId,
                key -> new InternalCommonGroup(groupMaster, groupId, groupName));
    }

    public InternalCommonGroup getGroupById(int id) {
        return groupById.get(id);
    }

    public InternalCommonGroup getGroupByName(String name) {
        return groupById.values().stream()
                .filter(groups -> name.equals(groups.getGroupName()))
                .findFirst().orElse(null);
    }
    public ConcurrentMap<Integer, SocketChannel> getSocketChannelByPhysicalId() {
        return socketChannelByPhysicalId;
    }

    public void setPhysicalId(int globalThreadId, int physicalId) {
        physicalIdByThreadId.put(globalThreadId, physicalId);
    }

    public int getPhysicalId(int globalThreadId) {
        return physicalIdByThreadId.get(globalThreadId);
    }

    public void putPcjThread(PcjThread pcjThread) {
        pcjThreads.putIfAbsent(pcjThread.getThreadId(), pcjThread);
    }

    public PcjThread getPcjThread(int pcjThread) {
        return pcjThreads.get(pcjThread);
    }

    public int getPhysicalId() {
        return physicalId;
    }

    public void setPhysicalId(int physicalId) {
        this.physicalId = physicalId;
    }

    public int getTotalNodeCount() {
        return totalNodeCount;
    }

    public void setTotalNodeCount(int totalNodeCount) {
        this.totalNodeCount = totalNodeCount;
    }

    public WaitObject getGlobalWaitObject() {
        return globalWaitObject;
    }

    public AtomicInteger getGroupJoinCounter() {
        return groupJoinCounter;
    }

    public GroupJoinQuery getGroupJoinQuery(int requestNum) {
        return groupJoinQueryMap.computeIfAbsent(requestNum, key -> new GroupJoinQuery());
    }

    public GroupJoinQuery removeGroupJoinQuery(int requestNum) {
        return groupJoinQueryMap.remove(requestNum);
    }
}
