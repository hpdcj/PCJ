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
import org.pcj.internal.Networker;
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
final public class HelloGoMessage extends Message {

    public HelloGoMessage() {
        super(MessageType.HELLO_GO);
    }
    
    @Override
    public void write(MessageDataOutputStream out) {
    }

    @Override
    public void onReceive(SocketChannel sender, MessageDataInputStream in) {
        NodeData nodeData = InternalPCJ.getNodeData();
        Networker networker = InternalPCJ.getNetworker();

        int physicalId = nodeData.getCurrentNodePhysicalId();
        if (physicalId * 2 + 1 < nodeData.getTotalNodeCount()) {
            SocketChannel childSocketChannel = nodeData.getSocketChannelByPhysicalId().get(physicalId * 2 + 1);
            networker.send(childSocketChannel, this);
        }
        if (physicalId * 2 + 2 < nodeData.getTotalNodeCount()) {
            SocketChannel childSocketChannel = nodeData.getSocketChannelByPhysicalId().get(physicalId * 2 + 2);
            networker.send(childSocketChannel, this);
        }

        nodeData.getHelloState().signalDone();
    }
}
