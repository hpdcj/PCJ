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
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
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
final public class MessageHello extends Message {

    private int port;
    private int[] threadIds;

    public MessageHello() {
        super(MessageType.HELLO);
    }

    public MessageHello(int port, int[] threadIds) {
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

        NodeInfo currentNodeInfo = new NodeInfo(address, this.port, this.threadIds);

        NodeData nodeData = InternalPCJ.getNodeData();
        HelloState state = nodeData.getHelloState();

        int currentPhysicalId = -state.getNextPhysicalId();

        ConcurrentMap<Integer, SocketChannel> socketChannelByPhysicalId = state.getSocketChannelByPhysicalId();
        socketChannelByPhysicalId.put(currentPhysicalId, sender);

        ConcurrentMap<Integer, NodeInfo> nodeInfoByPhysicalId = state.getNodeInfoByPhysicalId();
        nodeInfoByPhysicalId.put(currentPhysicalId, currentNodeInfo);

        int threadsLeftToConnect = state.decrementThreadsLeftToConnect(threadIds.length);

        if (threadsLeftToConnect == 0) {
            int nodesConnected = state.getNextPhysicalId() - 1;
            nodeData.getNode0Data().getFinishedBitmask().enlarge(nodesConnected);

            LOGGER.finest("Received HELLO from all nodes. Ordering physicalIds.");

            Set<Integer> physicalIdsSet = new LinkedHashSet<>();
            int[] sortedPhysicalIds = nodeInfoByPhysicalId
                                              .entrySet()
                                              .stream()
                                              .flatMap(entry -> entry.getValue()
                                                                        .getThreadIds()
                                                                        .stream()
                                                                        .map(threadId -> new AbstractMap.SimpleEntry<>(threadId, entry.getKey())))
                                              .sorted(Comparator.comparing(Map.Entry::getKey))
                                              .map(Map.Entry::getValue)
                                              .filter(physicalIdsSet::add)
                                              .mapToInt(Integer::intValue)
                                              .toArray();

            for (int i = 0; i < sortedPhysicalIds.length; ++i) {
                int oldPhysicalId = sortedPhysicalIds[i];
                int newPhysicalId = i;

                nodeInfoByPhysicalId.put(newPhysicalId, nodeInfoByPhysicalId.remove(oldPhysicalId));
                socketChannelByPhysicalId.put(newPhysicalId, socketChannelByPhysicalId.remove(oldPhysicalId));
            }
            
            MessageHelloInform helloInform = new MessageHelloInform(0, nodeInfoByPhysicalId);
            InternalPCJ.getNetworker().send(nodeData.getNode0Socket(), helloInform);
        }
    }
}
