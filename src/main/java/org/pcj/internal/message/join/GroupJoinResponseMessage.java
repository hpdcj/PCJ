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
    private int requesterGlobalThreadId;
    private int requesterGroupThreadId;

    public GroupJoinResponseMessage() {
        super(MessageType.GROUP_JOIN_RESPONSE);
    }

    public GroupJoinResponseMessage(int requestNum, int groupId, int requesterGlobalThreadId, int requesterGroupThreadId) {
        this();

        this.requestNum = requestNum;
        this.groupId = groupId;
        this.requesterGlobalThreadId = requesterGlobalThreadId;
        this.requesterGroupThreadId = requesterGroupThreadId;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(requestNum);
        out.writeInt(groupId);
        out.writeInt(requesterGlobalThreadId);
        out.writeInt(requesterGroupThreadId);
    }

    @Override
    public void onReceive(SocketChannel sender, MessageDataInputStream in) throws IOException {
        requestNum = in.readInt();
        groupId = in.readInt();
        requesterGlobalThreadId = in.readInt();
        requesterGroupThreadId = in.readInt();

        NodeData nodeData = InternalPCJ.getNodeData();

        InternalCommonGroup commonGroup = nodeData.getCommonGroupById(groupId);
        InternalGroup threadGroup = new InternalGroup(requesterGroupThreadId, commonGroup);
        PcjThread pcjThread = nodeData.getPcjThread(requesterGlobalThreadId);
        pcjThread.getThreadData().addGroup(threadGroup);

        GroupJoinStates states = nodeData.getGroupJoinStates();
        GroupJoinStates.Notification notification = states.removeNotification(requestNum, requesterGlobalThreadId);
        notification.signal(threadGroup);
    }
}
