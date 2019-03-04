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
import java.nio.channels.SocketChannel;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.MessageType;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * Message sent by each node to all nodes with physicalId less than its.
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
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(physicalId);
    }

    @Override
    public void onReceive(SocketChannel sender, MessageDataInputStream in) throws IOException {
        physicalId = in.readInt();

        NodeData nodeData = InternalPCJ.getNodeData();
        HelloState state = nodeData.getHelloState();
        state.processBonjourMessage(physicalId, sender);
    }
}
