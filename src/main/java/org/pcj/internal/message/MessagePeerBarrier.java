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
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.PcjThread;
import org.pcj.internal.futures.PeerBarrierState;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * Message sent by each node to node0 about finished execution.
 *
 * @param physicalId physicalId of node
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessagePeerBarrier extends Message {

    private int groupId;
    private int requesterThreadId;
    private int threadId;

    public MessagePeerBarrier() {
        super(MessageType.PEER_BARRIER);
    }

    public MessagePeerBarrier(int groupId, int requesterThreadId, int threadId) {
        this();

        this.groupId = groupId;
        this.requesterThreadId = requesterThreadId;
        this.threadId = threadId;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(groupId);
        out.writeInt(requesterThreadId);
        out.writeInt(threadId);
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        groupId = in.readInt();
        requesterThreadId = in.readInt();
        threadId = in.readInt();

        NodeData nodeData = InternalPCJ.getNodeData();
        InternalCommonGroup commonGroup = nodeData.getGroupById(groupId);

        int globalThreadId = commonGroup.getGlobalThreadId(threadId);
        PcjThread pcjThread = nodeData.getPcjThread(globalThreadId);

        InternalGroup group = (InternalGroup) pcjThread.getThreadData().getGroupById(groupId);

        PeerBarrierState peerBarrierState = group.getPeerBarrierState(requesterThreadId);
        peerBarrierState.peerBarrier();
    }
}
