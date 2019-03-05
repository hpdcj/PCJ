/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.bye;

import java.nio.channels.SocketChannel;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.MessageType;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class ByeMessage extends Message {

    public ByeMessage() {
        super(MessageType.BYE);
    }

    @Override
    public void write(MessageDataOutputStream out) {
    }

    @Override
    public void onReceive(SocketChannel sender, MessageDataInputStream in) {
        NodeData nodeData = InternalPCJ.getNodeData();
        ByeState byeState = nodeData.getByeState();
        byeState.nodeProcessed();
    }
}
