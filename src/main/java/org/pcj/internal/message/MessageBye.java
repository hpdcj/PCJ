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
import org.pcj.internal.NodeData.Node0Data;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * Message sent by each node to node0 about finished execution.
 *
 * @param physicalId physicalId of node
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageBye extends Message {

    private int physicalId;

    public MessageBye() {
        super(MessageType.BYE);
    }

    public MessageBye(int physicalId) {
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

        Node0Data node0Data = InternalPCJ.getNodeData().getNode0Data();
        Bitmask bitmask = node0Data.getFinishedBitmask();
        synchronized (bitmask) {
            bitmask.set(physicalId);
            if (bitmask.isSet()) {
                bitmask.clear();
                MessageByeCompleted messageByeCompleted = new MessageByeCompleted();

                // broadcasting:
                InternalPCJ.getNetworker().send(InternalPCJ.getNode0Socket(), messageByeCompleted);
            }
        }
    }
}
