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
 * Message sent by new-Client to Server with <b>new client connection data</b>
 *
 * @param port      listen-on port of new-Client (<tt>int</tt>)
 * @param threadIds global ids of new-Client threads (<tt>int[]</tt>)
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

        InternalGroup globalGroup = nodeData.getGlobalGroup();
        globalGroup.getSyncObject().signal();

    }
}
