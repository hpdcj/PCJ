/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.join;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import org.pcj.internal.InternalCommonGroup;
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
public class GroupJoinResponseMessage extends Message {

    private int requestNum;
    private int groupId;
    private int globalThreadId;
    private int groupThreadId;

    public GroupJoinResponseMessage() {
        super(MessageType.GROUP_JOIN_RESPONSE);
    }

    public GroupJoinResponseMessage(int requestNum, int groupId, int globalThreadId, int groupThreadId) {
        this();

        this.requestNum = requestNum;
        this.groupId = groupId;
        this.globalThreadId = globalThreadId;
        this.groupThreadId = groupThreadId;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(requestNum);
        out.writeInt(groupId);
        out.writeInt(globalThreadId);
        out.writeInt(groupThreadId);
    }

    @Override
    public void onReceive(SocketChannel sender, MessageDataInputStream in) throws IOException {
        requestNum = in.readInt();
        groupId = in.readInt();
        globalThreadId = in.readInt();
        groupThreadId = in.readInt();

        NodeData nodeData = InternalPCJ.getNodeData();

        InternalCommonGroup internalCommonGroup = nodeData.getCommonGroupById(groupId);
        InternalGroup threadGroup = new InternalGroup(groupThreadId, internalCommonGroup);
        PcjThread pcjThread = nodeData.getPcjThread(globalThreadId);
        pcjThread.getThreadData().addGroup(threadGroup);

        GroupJoinStates states = nodeData.getGroupJoinStates();
        GroupJoinStates.State state = states.remove(requestNum);
        state.signal(threadGroup);
    }
}
