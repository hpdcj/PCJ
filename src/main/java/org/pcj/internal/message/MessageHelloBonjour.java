/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * Message sent by each node to all nodes with physicalId less than its.
 *
 * @param physicalId physicalId of node
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageHelloBonjour extends Message {

    private int physicalId;

    public MessageHelloBonjour() {
        super(MessageType.HELLO_BONJOUR);
    }

    public MessageHelloBonjour(int physicalId) {
        this();

        this.physicalId = physicalId;
    }

    @Override
    public void writeObjects(MessageDataOutputStream out) throws IOException {
        out.writeInt(physicalId);
    }

    @Override
    public void readObjects(MessageDataInputStream in) throws IOException {
        physicalId = in.readInt();
    }

    @Override
    public String paramsToString() {
        return String.format("physicalId: %d", physicalId);
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        readObjects(in);

        NodeData nodeData = InternalPCJ.getNodeData();
        nodeData.getSocketChannelByPhysicalId().put(physicalId, sender);

        if (nodeData.getSocketChannelByPhysicalId().size() == nodeData.getTotalNodeCount()) {
            InternalPCJ.getNetworker().send(InternalPCJ.getNode0Socket(),
                    new MessageHelloCompleted(nodeData.getPhysicalId()));
        }
    }
}
