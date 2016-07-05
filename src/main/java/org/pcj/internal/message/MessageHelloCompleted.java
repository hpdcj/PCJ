/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.SocketChannel;
import org.pcj.internal.Bitmask;
import org.pcj.internal.InternalGroup;
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
final public class MessageHelloCompleted extends Message {

    private int physicalId;

    public MessageHelloCompleted() {
        super(MessageType.HELLO_COMPLETED);
    }

    public MessageHelloCompleted(int physicalId) {
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
    public void execute(SocketChannel sender, MessageDataInputStream in) {
        try {
            readObjects(in);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

        NodeData nodeData = InternalPCJ.getNodeData();
        InternalGroup globalGroup = nodeData.getGlobalGroup();
        Bitmask bitmask = globalGroup.getPhysicalSync();
        synchronized (bitmask) {
            bitmask.set(physicalId);
            if (bitmask.isSet()) {
                bitmask.clear();
                MessageHelloGo messageHelloGo = new MessageHelloGo();

                // broadcasting:
                InternalPCJ.getNetworker().send(InternalPCJ.getNode0Socket(), messageHelloGo);
            }
        }
    }
}
