/* 
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.NodeInfo;
import org.pcj.internal.network.LoopbackSocketChannel;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * Message sent by new-Client to Server with <b>new client connection data</b>
 *
 * @param port      listen-on port of new-Client (<tt>int</tt>)
 * @param threadIds global ids of new-Client threads (<tt>int[]</tt>)
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
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        port = in.readInt();
        threadIds = in.readIntArray();

        String address = null;
        if (sender instanceof LoopbackSocketChannel == false) {
            address = ((InetSocketAddress) sender.getRemoteAddress()).getHostString();
        }

        NodeInfo currentNodeInfo = new NodeInfo(address, this.port, this.threadIds);

        NodeData nodeData = InternalPCJ.getNodeData();
        NodeData.Node0Data node0Data = nodeData.getNode0Data();

        int currentPhysicalId = -node0Data.getConnectedNodeCount().incrementAndGet();

        nodeData.getSocketChannelByPhysicalId().put(currentPhysicalId, sender);
        node0Data.getNodeInfoByPhysicalId().put(currentPhysicalId, currentNodeInfo);

        int currentThreadCount = node0Data.getConnectedThreadCount().addAndGet(threadIds.length);

        if (currentThreadCount == node0Data.getAllNodesThreadCount()) {
            LOGGER.finest("Received HELLO from all nodes. Ordering physicalIds.");
            int connectedNodeCount = node0Data.getConnectedNodeCount().get();

            node0Data.getFinishedBitmask().enlarge(connectedNodeCount);
            node0Data.getHelloBitmask().enlarge(connectedNodeCount);

            Map<Integer, Integer> physicalIdMapping = new HashMap<>(connectedNodeCount);

            int nextPhysicalId = 0;

            Map<Integer, Integer> physicalIdByThreadId = node0Data.getNodeInfoByPhysicalId()
                    .entrySet().stream()
                    .flatMap(entry -> Arrays.stream(entry.getValue().getThreadIds())
                            .mapToObj(threadId -> new AbstractMap.SimpleEntry<Integer, Integer>(threadId, entry.getKey())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            // threadId -> physicalId
            for (int threadId : physicalIdByThreadId.keySet().stream()
                    .mapToInt(Integer::intValue).sorted().toArray()) {
                int oldPhysicalId = physicalIdByThreadId.remove(threadId);

                /* if that physicalId isn't mapped... */
                if (physicalIdMapping.containsKey(oldPhysicalId) == false) {
                    /* map old-physicalId with new-physicalId (0, 1, 2, ...) */
                    physicalIdMapping.put(oldPhysicalId, nextPhysicalId);

                    /* get socket associeted with old-physicalId */
                    SocketChannel socketChannel = nodeData.getSocketChannelByPhysicalId().remove(oldPhysicalId);
                    NodeInfo nodeInfo = node0Data.getNodeInfoByPhysicalId().remove(oldPhysicalId);


                    /* and put it as new-physicalId */
                    nodeData.getSocketChannelByPhysicalId().put(nextPhysicalId, socketChannel);
                    node0Data.getNodeInfoByPhysicalId().put(nextPhysicalId, nodeInfo);

                    /* prepare for new physicalId */
                    ++nextPhysicalId;
                }

                int newPhysicalId = physicalIdMapping.get(oldPhysicalId);
                nodeData.setPhysicalId(threadId, newPhysicalId);
            }

            nodeData.getSocketChannelByPhysicalId().entrySet().stream()
                    .forEach(element -> {
                        int physicalId = element.getKey();
                        SocketChannel socketChannel = element.getValue();

                        MessageHelloInform helloInform = new MessageHelloInform(physicalId, node0Data.getNodeInfoByPhysicalId());

                        InternalPCJ.getNetworker().send(socketChannel, helloInform);
                    });
        }
    }
}
