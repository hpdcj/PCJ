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
 * Message sent by new-Client to Server with <b>new client connection data</b>
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageHelloGo extends Message {

    public MessageHelloGo() {
        super(MessageType.HELLO_GO);
    }

    public MessageHelloGo(int port, int[] threadIds) {
        this();
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
    }

    @Override
    public void onReceive(SocketChannel sender, MessageDataInputStream in) {
        NodeData nodeData = InternalPCJ.getNodeData();
        int physicalId = nodeData.getPhysicalId();
        if (physicalId * 2 + 1 < nodeData.getTotalNodeCount()) {
            InternalPCJ.getNetworker().send(nodeData.getSocketChannelByPhysicalId().get(physicalId * 2 + 1), this);
        }
        if (physicalId * 2 + 2 < nodeData.getTotalNodeCount()) {
            InternalPCJ.getNetworker().send(nodeData.getSocketChannelByPhysicalId().get(physicalId * 2 + 2), this);
        }

        nodeData.getGlobalWaitObject().signal();
    }
}
