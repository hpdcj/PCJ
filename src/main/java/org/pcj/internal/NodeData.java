/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal;

import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.pcj.internal.message.alive.AliveState;
import org.pcj.internal.message.bye.ByeState;
import org.pcj.internal.message.hello.HelloState;
import org.pcj.internal.message.join.GroupJoinStates;
import org.pcj.internal.message.join.GroupQueryStates;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class NodeData {

    private final ConcurrentMap<Integer, InternalCommonGroup> groupById;
    private final ConcurrentMap<Integer, SocketChannel> socketChannelByPhysicalId; // physicalId -> socket
    private final ConcurrentMap<Integer, Integer> physicalIdByThreadId; // threadId -> physicalId
    private final ConcurrentMap<Integer, PcjThread> pcjThreads; // threadId -> pcjThread
    private final GroupQueryStates groupQueryStates;
    private final GroupJoinStates groupJoinStates;
    private SocketChannel node0Socket;
    private Node0Data node0Data;
    private HelloState helloState;
    private AliveState aliveState;
    private ByeState byeState;
    private int physicalId;
    private int totalNodeCount;

    public NodeData() {
        this.groupById = new ConcurrentHashMap<>();
        this.socketChannelByPhysicalId = new ConcurrentHashMap<>();
        this.physicalIdByThreadId = new ConcurrentHashMap<>();
        this.pcjThreads = new ConcurrentHashMap<>();

        this.aliveState = new AliveState();
        this.groupQueryStates = new GroupQueryStates();
        this.groupJoinStates = new GroupJoinStates();
    }

    public SocketChannel getNode0Socket() {
        return node0Socket;
    }

    void setNode0Socket(SocketChannel node0Socket) {
        this.node0Socket = node0Socket;
        this.socketChannelByPhysicalId.put(0, node0Socket);
    }

    public Node0Data getNode0Data() {
        return node0Data;
    }

    void setNode0Data(Node0Data node0Data) {
        this.node0Data = node0Data;
    }

    public InternalCommonGroup getOrCreateGroup(int groupMaster, int groupId, String groupName) {
        return groupById.computeIfAbsent(groupId,
                key -> new InternalCommonGroup(groupMaster, groupId, groupName));
    }

    public InternalCommonGroup getCommonGroupById(int id) {
        return groupById.get(id);
    }

    InternalCommonGroup getInternalCommonGroupByName(String name) {
        return groupById.values().stream()
                       .filter(groups -> name.equals(groups.getName()))
                       .findFirst().orElse(null);
    }

    public SocketChannel getSocketChannelByPhysicalId(int physicalId) {
        return socketChannelByPhysicalId.get(physicalId);
    }

    public void updateSocketChannelByPhysicalId(ConcurrentMap<Integer, SocketChannel> newSocketChannelByPhysicalId) {
        socketChannelByPhysicalId.putAll(newSocketChannelByPhysicalId);
    }

    public int getPhysicalIdBySocketChannel(SocketChannel socketChannel) {
        return socketChannelByPhysicalId.entrySet()
                       .stream()
                       .filter(entry -> entry.getValue().equals(socketChannel))
                       .map(Map.Entry::getKey)
                       .findAny()
                       .orElseThrow(() -> new IllegalStateException("Unknown socket channel: " + socketChannel));
    }

    public void setPhysicalId(int globalThreadId, int physicalId) {
        physicalIdByThreadId.put(globalThreadId, physicalId);
    }

    public int getPhysicalId(int globalThreadId) {
        return physicalIdByThreadId.get(globalThreadId);
    }

    void updatePcjThreads(Map<Integer, PcjThread> pcjThreadMap) {
        pcjThreads.putAll(pcjThreadMap);
    }

    public PcjThread getPcjThread(int globalThreadId) {
        return pcjThreads.get(globalThreadId);
    }

    public PcjThread getPcjThread(int groupId, int threadId) {
        InternalCommonGroup commonGroup = getCommonGroupById(groupId);
        int globalThreadId = commonGroup.getGlobalThreadId(threadId);

        return getPcjThread(globalThreadId);
    }

    public int getCurrentNodePhysicalId() {
        return physicalId;
    }

    public void setCurrentNodePhysicalId(int physicalId) {
        this.physicalId = physicalId;
    }

    public int getTotalNodeCount() {
        return totalNodeCount;
    }

    public void setTotalNodeCount(int totalNodeCount) {
        this.totalNodeCount = totalNodeCount;
    }

    public HelloState getHelloState() {
        return helloState;
    }

    void setHelloState(HelloState helloState) {
        this.helloState = helloState;
    }

    public AliveState getAliveState() {
        return aliveState;
    }

    public ByeState getByeState() {
        return byeState;
    }

    public void setByeState(ByeState byeState) {
        this.byeState = byeState;
    }

    public GroupQueryStates getGroupQueryStates() {
        return groupQueryStates;
    }

    public GroupJoinStates getGroupJoinStates() {
        return groupJoinStates;
    }

    public static class Node0Data {

        private final AtomicInteger groupIdCounter;
        private final ConcurrentMap<String, Integer> groupsId; // groupName -> groupId
        private final ConcurrentMap<Integer, Integer> groupsMaster; // groupId -> physicalId

        Node0Data() {
            this.groupIdCounter = new AtomicInteger(1);
            this.groupsId = new ConcurrentHashMap<>();
            this.groupsMaster = new ConcurrentHashMap<>();

            groupsId.put("", 0);
            groupsMaster.put(0, 0);
        }

        public int getGroupId(String name) {
            return groupsId.computeIfAbsent(name, key -> groupIdCounter.getAndIncrement());
        }

        public int getGroupMaster(int groupId, int physicalId) {
            return groupsMaster.computeIfAbsent(groupId, key -> physicalId);
        }
    }
}
