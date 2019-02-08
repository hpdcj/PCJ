/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.peerbarrier;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import org.pcj.internal.InternalGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.PcjThread;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.MessageType;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class PeerBarrierMessage extends Message {

    private int groupId;
    private int requesterThreadId;
    private int threadId;

    public PeerBarrierMessage() {
        super(MessageType.PEER_BARRIER);
    }

    public PeerBarrierMessage(int groupId, int requesterThreadId, int threadId) {
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
    public void onReceive(SocketChannel sender, MessageDataInputStream in) throws IOException {
        groupId = in.readInt();
        requesterThreadId = in.readInt();
        threadId = in.readInt();

        NodeData nodeData = InternalPCJ.getNodeData();
        PcjThread pcjThread = nodeData.getPcjThread(groupId, threadId);

        InternalGroup group = pcjThread.getThreadData().getGroupById(groupId);

        PeerBarrierStates peerBarrierStates = group.getPeerBarrierStates();
        PeerBarrierStates.State state = peerBarrierStates.getOrCreate(requesterThreadId);
        state.doPeerBarrier();
    }
}
