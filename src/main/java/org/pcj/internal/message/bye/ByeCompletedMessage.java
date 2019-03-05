/* 
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.bye;

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
 * Message sent by node0 to all nodes about completed of execution.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class ByeCompletedMessage extends Message {

    public ByeCompletedMessage() {
        super(MessageType.BYE_COMPLETED);
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
    }

    @Override
    public void onReceive(SocketChannel sender, MessageDataInputStream in) {
        NodeData nodeData = InternalPCJ.getNodeData();
        Networker networker = InternalPCJ.getNetworker();

        int physicalId = nodeData.getCurrentNodePhysicalId();
        if (physicalId * 2 + 1 < nodeData.getTotalNodeCount()) {
            networker.send(nodeData.getSocketChannelByPhysicalId(physicalId * 2 + 1), this);
        }
        if (physicalId * 2 + 2 < nodeData.getTotalNodeCount()) {
            networker.send(nodeData.getSocketChannelByPhysicalId(physicalId * 2 + 2), this);
        }

        ByeState byeState = nodeData.getByeState();
        byeState.signalDone();
    }
}
