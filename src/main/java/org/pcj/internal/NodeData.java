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

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public final class NodeData {

    private final ConcurrentMap<Integer, InternalCommonGroup> groupById;
    private final ConcurrentMap<Integer, SocketChannel> socketChannelByPhysicalId; // physicalId -> socket
    private final ConcurrentMap<Integer, Integer> physicalIdByThreadId; // threadId -> physicalId
    private final ConcurrentMap<Integer, PcjThread> pcjThreads; // threadId -> pcjThread
    private final AliveState aliveState;
    private SocketChannel node0Socket;
    private Node0Data node0Data;
    private HelloState helloState;
    private ByeState byeState;
    private int physicalId;
    private int totalNodeCount;

    public NodeData() {
        this.groupById = new ConcurrentHashMap<>();
        this.socketChannelByPhysicalId = new ConcurrentHashMap<>();
        this.physicalIdByThreadId = new ConcurrentHashMap<>();
        this.pcjThreads = new ConcurrentHashMap<>();

        this.aliveState = new AliveState();
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

    public InternalCommonGroup getOrCreateCommonGroup(int groupId) {
        return groupById.computeIfAbsent(groupId,
                key -> {
                    System.out.println(InternalPCJ.getNodeData().getCurrentNodePhysicalId()+" creating group "+groupId);return new InternalCommonGroup(groupId);});
    }

    public InternalCommonGroup getCommonGroupById(int id) {
        return groupById.get(id);
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

    public static class Node0Data {

        private final AtomicInteger groupIdCounter;

        Node0Data() {
            this.groupIdCounter = new AtomicInteger(1);
        }

        public int reserveGroupId() {
            return groupIdCounter.getAndIncrement();
        }
    }
}
