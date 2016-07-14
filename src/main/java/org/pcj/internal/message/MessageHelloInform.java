/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import org.pcj.internal.Configuration;
import org.pcj.internal.InternalGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.Networker;
import org.pcj.internal.NodeData;
import org.pcj.internal.NodeInfo;
import org.pcj.internal.network.LoopbackSocketChannel;
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
    public void writeObjects(MessageDataOutputStream out) throws IOException {
        out.writeInt(physicalId);
        out.writeObject(nodeInfoByPhysicalId);
    }

    @Override
    public void readObjects(MessageDataInputStream in) throws IOException {
        physicalId = in.readInt();
        try {
            Object obj = in.readObject();
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<Integer, NodeInfo> map = (Map<Integer, NodeInfo>) obj;
                nodeInfoByPhysicalId = map;
            }
        } catch (ClassNotFoundException ex) {
            LOGGER.log(Level.SEVERE, "Unable to read nodeInfoByPhysicalId", ex);
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String paramsToString() {
        return String.format("physicalId: %d, nodeInfoByPhysicalId:%s",
                physicalId, Objects.toString(nodeInfoByPhysicalId));
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        readObjects(in);

        NodeData nodeData = InternalPCJ.getNodeData();
        nodeData.setPhysicalId(physicalId);

        Networker networker = InternalPCJ.getNetworker();
        InternalGroup globalGroup = new InternalGroup(0, InternalGroup.GLOBAL_GROUP_ID, InternalGroup.GLOBAL_GROUP_NAME);
        nodeData.addGroup(globalGroup);

        nodeData.setTotalNodeCount(nodeInfoByPhysicalId.size());

        for (Map.Entry<Integer, NodeInfo> entry : nodeInfoByPhysicalId.entrySet()) {
            Integer currentPhysicalId = entry.getKey();
            NodeInfo nodeInfo = entry.getValue();
            Arrays.stream(nodeInfo.getThreadIds())
                    .forEach(threadId -> {
                        nodeData.getPhysicalIdByThreadId().put(threadId, currentPhysicalId);
                        globalGroup.addThread(currentPhysicalId, threadId, threadId);
                    });

            if (0 < currentPhysicalId && currentPhysicalId < physicalId) {
                SocketChannel socketChannel = connectToNode(nodeInfo.getHostname(), nodeInfo.getPort());
                nodeData.getSocketChannelByPhysicalId().put(currentPhysicalId, socketChannel);

                networker.send(socketChannel, new MessageHelloBonjour(physicalId));
            }
        }

        nodeData.getSocketChannelByPhysicalId().put(physicalId, LoopbackSocketChannel.getInstance());

        if (nodeData.getSocketChannelByPhysicalId().size() == nodeData.getTotalNodeCount()) {
            InternalPCJ.getNetworker().send(InternalPCJ.getNode0Socket(),
                    new MessageHelloCompleted(nodeData.getPhysicalId()));
        }
    }

    private SocketChannel connectToNode(String hostname, int port) {
        for (int attempt = 0; attempt <= Configuration.RETRY_COUNT; ++attempt) {
            try {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(String.format("Connecting to: %s:%s",
                            hostname, port));
                }
                InetAddress inetAddressNode0 = InetAddress.getByName(hostname);
                SocketChannel socket = InternalPCJ.getNetworker().connectTo(inetAddressNode0, port);
                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.finer(String.format("Connected to %s:%s: %s",
                            hostname, port, Objects.toString(socket)));
                }
                return socket;
            } catch (IOException ex) {
                if (attempt < Configuration.RETRY_COUNT) {
                    LOGGER.warning(String.format("(%d attempt of %d) Connecting to %s:%d failed: %s.",
                            attempt + 1, Configuration.RETRY_COUNT + 1,
                            hostname, port, ex.getMessage()));

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
                        String.format("Interruption occurs while connecting to %s:%d.", hostname, port));
            }
        }
        throw new IllegalStateException("Unreachable code.");
    }
}
