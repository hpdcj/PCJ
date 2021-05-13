/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.hello;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.NodeInfo;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.MessageType;
import org.pcj.internal.network.LoopbackSocketChannel;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * Message sent by new-Client to Server with <b>new client connection data</b>
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public final class HelloMessage extends Message {

    private int port;
    private int[] threadIds;

    public HelloMessage() {
        super(MessageType.HELLO);
    }

    public HelloMessage(int port, int[] threadIds) {
        this();

        this.port = port;
        this.threadIds = threadIds;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(port);
        out.writeIntArray(threadIds);
    }

    @Override
    public void onReceive(SocketChannel sender, MessageDataInputStream in) throws IOException {
        port = in.readInt();
        threadIds = in.readIntArray();

        String address;
        if (sender instanceof LoopbackSocketChannel) {
            address = null;
        } else {
            address = ((InetSocketAddress) sender.getRemoteAddress()).getHostString();
        }

        NodeInfo currentNodeInfo = new NodeInfo(address, this.port);
        Arrays.stream(threadIds).forEach(currentNodeInfo::addThreadId);

        NodeData nodeData = InternalPCJ.getNodeData();
        HelloState state = nodeData.getHelloState();

        int currentPhysicalId = -state.getNextPhysicalId();

        ConcurrentMap<Integer, SocketChannel> socketChannelByPhysicalId = state.getSocketChannelByPhysicalId();
        socketChannelByPhysicalId.put(currentPhysicalId, sender);

        ConcurrentMap<Integer, NodeInfo> nodeInfoByPhysicalId = state.getNodeInfoByPhysicalId();
        nodeInfoByPhysicalId.put(currentPhysicalId, currentNodeInfo);

        int threadsLeftToConnect = state.decrementThreadsLeftToConnect(threadIds.length);

        if (threadsLeftToConnect == 0) {
            LOGGER.finest("Received HELLO from all nodes. Ordering physicalIds.");

            int[] sortedPhysicalIds = nodeInfoByPhysicalId
                                              .entrySet()
                                              .stream()
                                              .flatMap(entry -> entry.getValue()
                                                                        .getThreadIds()
                                                                        .stream()
                                                                        .map(threadId -> new AbstractMap.SimpleEntry<>(threadId, entry.getKey())))
                                              .sorted(Map.Entry.comparingByKey())
                                              .map(Map.Entry::getValue)
                                              .distinct()
                                              .mapToInt(Integer::intValue)
                                              .toArray();

            AtomicInteger atomicInteger = new AtomicInteger(0);
            Map<Integer, Queue<Integer>> givenThreadIds = nodeInfoByPhysicalId
                                                                  .values()
                                                                  .stream()
                                                                  .map(NodeInfo::getThreadIds)
                                                                  .flatMap(Set::stream)
                                                                  .sorted()
                                                                  .collect(HashMap::new,
                                                                          (map, key) -> map.compute(key,
                                                                                  (k, v) -> {
                                                                                      if (v == null) {
                                                                                          v = new LinkedList<>();
                                                                                      }
                                                                                      v.add(atomicInteger.getAndIncrement());
                                                                                      return v;
                                                                                  }),
                                                                          Map::putAll);

            // TODO: najpierw przesortowac ThreadId a dopiero pozniej physicalId

            for (int i = 0; i < sortedPhysicalIds.length; ++i) {
                int givenPhysicalId = sortedPhysicalIds[i];
                int newPhysicalId = i;

                NodeInfo givenNodeInfo = nodeInfoByPhysicalId.remove(givenPhysicalId);
                NodeInfo newNodeInfo = new NodeInfo(givenNodeInfo.getHostname(), givenNodeInfo.getPort());
                for (int givenThreadId : givenNodeInfo.getThreadIds()) {
                    int newThreadId = givenThreadIds.get(givenThreadId).remove();
                    newNodeInfo.addThreadId(newThreadId);
                }

                nodeInfoByPhysicalId.put(newPhysicalId, newNodeInfo);
                socketChannelByPhysicalId.put(newPhysicalId, socketChannelByPhysicalId.remove(givenPhysicalId));
            }

            HelloInformMessage helloInform = new HelloInformMessage(0, nodeInfoByPhysicalId);
            InternalPCJ.getNetworker().send(nodeData.getNode0Socket(), helloInform);
        }
    }
}
