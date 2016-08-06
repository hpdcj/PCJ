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
import java.nio.channels.SocketChannel;
import org.pcj.internal.Bitmask;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData.Node0Data;
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
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(physicalId);
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        physicalId = in.readInt();

        Node0Data node0Data = InternalPCJ.getNodeData().getNode0Data();
        Bitmask bitmask = node0Data.getHelloBitmask();
        synchronized (bitmask) {
            bitmask.set(physicalId);
            if (bitmask.isSet()) {
                bitmask.clear();
                MessageHelloGo messageHelloGo = new MessageHelloGo();

                // broadcasting:
                InternalPCJ.getNetworker().send(InternalPCJ.getNodeData().getNode0Socket(), messageHelloGo);
            }
        }
    }
}
