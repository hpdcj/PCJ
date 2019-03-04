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
import java.util.Map;
import org.pcj.PcjRuntimeException;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.NodeInfo;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.MessageType;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class HelloInformMessage extends Message {

    private int currentPhysicalId;
    private Map<Integer, NodeInfo> nodeInfoByPhysicalId;

    public HelloInformMessage() {
        super(MessageType.HELLO_INFORM);
    }

    public HelloInformMessage(int currentPhysicalId, Map<Integer, NodeInfo> nodeInfoByPhysicalId) {
        this();

        this.currentPhysicalId = currentPhysicalId;
        this.nodeInfoByPhysicalId = nodeInfoByPhysicalId;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(currentPhysicalId);
        out.writeObject(nodeInfoByPhysicalId);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onReceive(SocketChannel sender, MessageDataInputStream in) throws IOException {
        currentPhysicalId = in.readInt();
        try {
            nodeInfoByPhysicalId = (Map<Integer, NodeInfo>) in.readObject();
        } catch (Exception ex) {
            throw new PcjRuntimeException("Unable to read nodeInfoByPhysicalId", ex);
        }

        NodeData nodeData = InternalPCJ.getNodeData();
        HelloState state = nodeData.getHelloState();
        state.processInformMessage(sender, currentPhysicalId, nodeInfoByPhysicalId);
    }
}
