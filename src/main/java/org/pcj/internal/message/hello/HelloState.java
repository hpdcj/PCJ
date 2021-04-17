/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.hello;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalFuture;
import org.pcj.internal.InternalGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.Networker;
import org.pcj.internal.NodeData;
import org.pcj.internal.NodeInfo;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.bye.ByeState;

/*
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class HelloState {

    private final HelloFuture future;
    private final ConcurrentMap<Integer, SocketChannel> socketChannelByPhysicalId; // physicalId -> nodeInfo
    private final ConcurrentMap<Integer, NodeInfo> nodeInfoByPhysicalId; // physicalId -> nodeInfo
    private final AtomicInteger threadsLeftToConnect;
    private final AtomicInteger connectedNodeCount;
    private final AtomicInteger notificationCount;

    public HelloState(int allNodesThreadCount) {
        this.future = new HelloFuture();

        this.socketChannelByPhysicalId = new ConcurrentHashMap<>();
        this.nodeInfoByPhysicalId = new ConcurrentHashMap<>();

        this.threadsLeftToConnect = new AtomicInteger(allNodesThreadCount);
        this.connectedNodeCount = new AtomicInteger(0);

        this.notificationCount = new AtomicInteger(0);
    }

    public void await(long timeoutSeconds) throws InterruptedException, TimeoutException {
        future.await(timeoutSeconds);
    }

    public void signalDone() {
        future.signalDone();
    }

    public int getNextPhysicalId() {
        return connectedNodeCount.incrementAndGet();
    }

    public ConcurrentMap<Integer, SocketChannel> getSocketChannelByPhysicalId() {
        return socketChannelByPhysicalId;
    }

    public ConcurrentMap<Integer, NodeInfo> getNodeInfoByPhysicalId() {
        return nodeInfoByPhysicalId;
    }

    public int decrementThreadsLeftToConnect(int threadConnected) {
        return threadsLeftToConnect.addAndGet(-threadConnected);
    }

    public void processInformMessage(SocketChannel sender, int currentPhysicalId, Map<Integer, NodeInfo> nodeInfoByPhysicalId) {
        int nodesCount = nodeInfoByPhysicalId.size();

        NodeData nodeData = InternalPCJ.getNodeData();
        nodeData.setCurrentNodePhysicalId(currentPhysicalId);
        nodeData.setTotalNodeCount(nodesCount);

        int nodesCountDownTree = nodesCount - currentPhysicalId;
        int childCount = Math.min(Math.max(nodesCount - currentPhysicalId * 2 - 1, 0), 2);
        if (currentPhysicalId == 0) {
            notificationCount.addAndGet(childCount + 1);
        } else {
            notificationCount.addAndGet(nodesCountDownTree);
        }
        nodeData.setByeState(new ByeState(childCount));

        createThreadsMapping(nodeInfoByPhysicalId);

        socketChannelByPhysicalId.put(currentPhysicalId, InternalPCJ.getLoopbackSocketChannel());
        socketChannelByPhysicalId.put((currentPhysicalId - 1) / 2, sender);
        connectToChildNodesAndSendInform(currentPhysicalId, nodeInfoByPhysicalId);
        connectToLowerNodesAndSendBonjour(currentPhysicalId, nodeInfoByPhysicalId);

        nodeProcessed();
    }

    private void createThreadsMapping(Map<Integer, NodeInfo> nodeInfoByPhysicalId) {
        NodeData nodeData = InternalPCJ.getNodeData();

        Map<Integer, Integer> threadsMap = new HashMap<>();
        for (Map.Entry<Integer, NodeInfo> entry : nodeInfoByPhysicalId.entrySet()) {
            int physicalId = entry.getKey();
            NodeInfo nodeInfo = entry.getValue();

            for (int threadId : nodeInfo.getThreadIds()) {
                nodeData.setPhysicalId(threadId, physicalId);
                threadsMap.put(threadId, threadId);
            }
        }

        InternalCommonGroup globalGroup = nodeData.getOrCreateGroup(0, InternalCommonGroup.GLOBAL_GROUP_ID);
        globalGroup.updateThreadsMap(threadsMap);
    }

    private void connectToLowerNodesAndSendBonjour(int currentPhysicalId, Map<Integer, NodeInfo> nodeInfoByPhysicalId) {
        Networker networker = InternalPCJ.getNetworker();

        HelloBonjourMessage helloBonjourMessage = new HelloBonjourMessage(currentPhysicalId);
        nodeInfoByPhysicalId.keySet().stream()
                .filter(physicalId -> physicalId > 0)
                .filter(physicalId -> physicalId < currentPhysicalId)
                .filter(physicalId -> (currentPhysicalId - 1) / 2 != physicalId)
                .forEach(physicalId -> {
                    NodeInfo nodeInfo = nodeInfoByPhysicalId.get(physicalId);

                    SocketChannel socketChannel = socketChannelByPhysicalId.computeIfAbsent(physicalId,
                            key -> networker.tryToConnectTo(nodeInfo.getHostname(), nodeInfo.getPort()));

                    networker.send(socketChannel, helloBonjourMessage);
                });
    }

    private void connectToChildNodesAndSendInform(int currentPhysicalId, Map<Integer, NodeInfo> nodeInfoByPhysicalId) {
        Networker networker = InternalPCJ.getNetworker();

        nodeInfoByPhysicalId.keySet().stream()
                .filter(physicalId -> physicalId > 0)
                .filter(physicalId -> (physicalId - 1) / 2 == currentPhysicalId)
                .forEach(physicalId -> {
                    NodeInfo nodeInfo = nodeInfoByPhysicalId.get(physicalId);

                    SocketChannel socketChannel = socketChannelByPhysicalId.computeIfAbsent(physicalId,
                            key -> networker.tryToConnectTo(nodeInfo.getHostname(), nodeInfo.getPort()));

                    Message messageHelloInform = new HelloInformMessage(physicalId, nodeInfoByPhysicalId);
                    networker.send(socketChannel, messageHelloInform);
                });
    }

    public void processBonjourMessage(int physicalId, SocketChannel sender) {
        socketChannelByPhysicalId.put(physicalId, sender);

        nodeProcessed();
    }

    public void processCompletedMessage() {
        nodeProcessed();
    }

    private void nodeProcessed() {
        int leftPhysical = notificationCount.decrementAndGet();
        if (leftPhysical == 0) {
            NodeData nodeData = InternalPCJ.getNodeData();
            Networker networker = InternalPCJ.getNetworker();

            nodeData.updateSocketChannelByPhysicalId(socketChannelByPhysicalId);

            int currentPhysicalId = nodeData.getCurrentNodePhysicalId();
            if (currentPhysicalId == 0) {
                SocketChannel node0Socket = nodeData.getNode0Socket();

                HelloGoMessage helloGoMessage = new HelloGoMessage();
                networker.send(node0Socket, helloGoMessage);
            } else {
                SocketChannel parentSocketChannel = nodeData.getSocketChannelByPhysicalId((currentPhysicalId - 1) / 2);

                HelloCompletedMessage messageHelloCompleted = new HelloCompletedMessage();
                networker.send(parentSocketChannel, messageHelloCompleted);
            }

        }
    }

    public static class HelloFuture extends InternalFuture<InternalGroup> {
        protected void signalDone() {
            super.signal();
        }

        private void await(long timeoutSeconds) throws InterruptedException, TimeoutException {
            super.await(timeoutSeconds, TimeUnit.SECONDS);
        }
    }
}
