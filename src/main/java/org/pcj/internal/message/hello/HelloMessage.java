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
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.NodeInfo;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.MessageType;
import org.pcj.internal.network.LoopbackSocketChannel;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * Message sent by new-Client to Server with <b>new client connection data</b>
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public final class HelloMessage extends Message {

    private int port;
    private int[] threadIds;

    public HelloMessage() {
        super(MessageType.HELLO);
    }

    public HelloMessage(int port, int[] threadIds) {
        this();

        this.port = port;
        this.threadIds = threadIds;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(port);
        out.writeIntArray(threadIds);
    }

    @Override
    public void onReceive(SocketChannel sender, MessageDataInputStream in) throws IOException {
        port = in.readInt();
        threadIds = in.readIntArray();

        NodeData nodeData = InternalPCJ.getNodeData();
        HelloState state = nodeData.getHelloState();
        state.processHelloMessage(sender, port, threadIds);
    }
}
