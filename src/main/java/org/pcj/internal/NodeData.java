/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal;

import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.pcj.internal.futures.WaitObject;

/**
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class NodeData {

    final private ConcurrentMap<Integer, InternalCommonGroup> groupById;
    final private ConcurrentMap<String, InternalCommonGroup> groupByName;
    final private ConcurrentMap<Integer, SocketChannel> socketChannelByPhysicalId; // physicalId -> socket
    final private ConcurrentMap<Integer, Integer> physicalIdByThreadId; // threadId -> physicalId
    final private ConcurrentMap<Integer, PcjThread> pcjThreads;
    final private Node0Data node0Data;
    final private WaitObject globalWaitObject;
    private int physicalId;
    private int totalNodeCount;

    public static class Node0Data {

        final private AtomicInteger connectedNodeCount;
        final private AtomicInteger connectedThreadCount;
        final private Bitmask helloBitmask;
        final private Bitmask finishedBitmask;
        final private ConcurrentMap<String, Integer> groupsId; // groupName -> groupId
        final private ConcurrentMap<Integer, Integer> groupsMaster; // groupId -> physicalId
        final private ConcurrentMap<Integer, NodeInfo> nodeInfoByPhysicalId; // physicalId -> nodeInfo
        private int allNodesThreadCount;

        public Node0Data() {
            this.connectedNodeCount = new AtomicInteger(0);
            this.connectedThreadCount = new AtomicInteger(0);
            this.helloBitmask = new Bitmask();
            this.finishedBitmask = new Bitmask();

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
    }

    public NodeData(boolean isCurrentJvmNode0) {
        this.groupById = new ConcurrentHashMap<>();
        this.groupByName = new ConcurrentHashMap<>();
        this.socketChannelByPhysicalId = new ConcurrentHashMap<>();
        this.physicalIdByThreadId = new ConcurrentHashMap<>();
        this.pcjThreads = new ConcurrentHashMap<>();
        this.globalWaitObject = new WaitObject();

        if (isCurrentJvmNode0) {
            node0Data = new Node0Data();
        } else {
            node0Data = null;
        }
    }

    public Node0Data getNode0Data() {
        return node0Data;
    }

    public InternalCommonGroup addGroup(InternalCommonGroup newGroup) {
        InternalCommonGroup group = groupById.putIfAbsent(newGroup.getGroupId(), newGroup);
        if (group != null) {
            return group;
        }

        groupByName.put(newGroup.getGroupName(), newGroup);
        return newGroup;
    }

    public InternalCommonGroup getGroupById(int id) {
        return groupById.get(id);
    }

    public ConcurrentMap<Integer, SocketChannel> getSocketChannelByPhysicalId() {
        return socketChannelByPhysicalId;
    }

    public ConcurrentMap<Integer, Integer> getPhysicalIdByThreadId() {
        return physicalIdByThreadId;
    }

    public ConcurrentMap<Integer, PcjThread> getPcjThreads() {
        return pcjThreads;
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
}
