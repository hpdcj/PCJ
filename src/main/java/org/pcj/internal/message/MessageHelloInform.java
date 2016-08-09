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
import java.net.InetAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import org.pcj.internal.Configuration;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.Networker;
import org.pcj.internal.NodeData;
import org.pcj.internal.NodeInfo;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * Message sent by node0 to every node about node's physicalId and with data about all nodes in run.
 *
 * @param physicalId           physicalId of node
 * @param nodeInfoByPhysicalId information about all nodes in run
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageHelloInform extends Message {

    private int physicalId;
    private Map<Integer, NodeInfo> nodeInfoByPhysicalId;

    public MessageHelloInform() {
        super(MessageType.HELLO_INFORM);
    }

    public MessageHelloInform(int physicalId, Map<Integer, NodeInfo> nodeInfoByPhysicalId) {
        this();

        this.physicalId = physicalId;
        this.nodeInfoByPhysicalId = nodeInfoByPhysicalId;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(physicalId);
        out.writeObject(nodeInfoByPhysicalId);
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        physicalId = in.readInt();
        try {
            Object obj = in.readObject();
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<Integer, NodeInfo> map = (Map<Integer, NodeInfo>) obj;
                nodeInfoByPhysicalId = map;
            }
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("Unable to read nodeInfoByPhysicalId", ex);
        }

        NodeData nodeData = InternalPCJ.getNodeData();
        nodeData.setPhysicalId(physicalId);

        Networker networker = InternalPCJ.getNetworker();
        InternalCommonGroup globalGroup = nodeData.createGroup(0, InternalCommonGroup.GLOBAL_GROUP_ID, InternalCommonGroup.GLOBAL_GROUP_NAME);

        nodeData.setTotalNodeCount(nodeInfoByPhysicalId.size());

        List<Integer> keys = new ArrayList<>(nodeInfoByPhysicalId.keySet());
        keys.sort(Integer::compare);
        for (int currentPhysicalId : keys) {
            NodeInfo nodeInfo = nodeInfoByPhysicalId.get(currentPhysicalId);
            Arrays.stream(nodeInfo.getThreadIds())
                    .forEach(threadId -> {
                        nodeData.setPhysicalId(threadId, currentPhysicalId);
                        globalGroup.addThread(threadId, threadId);
                    });

            if (0 < currentPhysicalId && currentPhysicalId < physicalId) {
                SocketChannel socketChannel = connectToNode(nodeInfo.getHostname(), nodeInfo.getPort());
                nodeData.getSocketChannelByPhysicalId().put(currentPhysicalId, socketChannel);

                networker.send(socketChannel, new MessageHelloBonjour(physicalId));
            }
        }

        nodeData.getSocketChannelByPhysicalId().put(physicalId, InternalPCJ.getLoopbackSocketChannel());

        if (nodeData.getSocketChannelByPhysicalId().size() == nodeData.getTotalNodeCount()) {
            InternalPCJ.getNetworker().send(InternalPCJ.getNodeData().getNode0Socket(),
                    new MessageHelloCompleted(nodeData.getPhysicalId()));
        }
    }

    private SocketChannel connectToNode(String hostname, int port) {
        for (int attempt = 0; attempt <= Configuration.RETRY_COUNT; ++attempt) {
            try {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Connecting to: {0}:{1,number,#}",
                            new Object[]{hostname, port});
                }
                InetAddress inetAddressNode0 = InetAddress.getByName(hostname);
                SocketChannel socket = InternalPCJ.getNetworker().connectTo(inetAddressNode0, port);
                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.log(Level.FINER, "Connected to {0}:{1,number,#}: {2}",
                            new Object[]{hostname, port, Objects.toString(socket)});
                }
                return socket;
            } catch (IOException ex) {
                if (attempt < Configuration.RETRY_COUNT) {
                    LOGGER.log(Level.WARNING,
                            "({0,number,#} attempt of {1,number,#}) Connecting to {2}:{3,number,#} failed: {4}. Retrying.",
                            new Object[]{attempt + 1, Configuration.RETRY_COUNT + 1, hostname, port, ex.getMessage()});

                    try {
                        Thread.sleep(Configuration.RETRY_DELAY * 1000 + (int) (Math.random() * 1000));
                    } catch (InterruptedException e) {
                        LOGGER.log(Level.SEVERE, "Interruption occurs while waiting for connection retry.");
                    }
                } else {
                    throw new RuntimeException(
                            String.format("Connecting to %s:%d failed!", hostname, port), ex);
                }
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE,
                        "Interruption occurs while connecting to {0}:{1,number,#}.",
                        new Object[]{hostname, port});
            }
        }
        throw new IllegalStateException("Unreachable code.");
    }
}
