/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import org.pcj.internal.InternalGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.NodeInfo;
import org.pcj.internal.network.LoopbackSocketChannel;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * Message sent by node0 to all nodes about completed of execution.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageByeCompleted extends Message {

    public MessageByeCompleted() {
        super(MessageType.BYE_COMPLETED);
    }

    @Override
    public void writeObjects(MessageDataOutputStream out) throws IOException {
    }

    @Override
    public void readObjects(MessageDataInputStream in) throws IOException {
    }

    @Override
    public String paramsToString() {
        return "";
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) {
        NodeData nodeData = InternalPCJ.getNodeData();
        int physicalId = nodeData.getPhysicalId();
        if (physicalId * 2 + 1 < nodeData.getTotalNodeCount()) {
            InternalPCJ.getNetworker().send(nodeData.getSocketChannelByPhysicalId().get(physicalId * 2 + 1), this);
        }
        if (physicalId * 2 + 2 < nodeData.getTotalNodeCount()) {
            InternalPCJ.getNetworker().send(nodeData.getSocketChannelByPhysicalId().get(physicalId * 2 + 2), this);
        }

        nodeData.getFinishedObject().signal();
    }
}
