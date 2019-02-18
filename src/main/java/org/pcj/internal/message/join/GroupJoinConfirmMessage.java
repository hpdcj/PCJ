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
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.MessageType;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class GroupJoinConfirmMessage extends Message {

    private int requestNum;
    private int groupId;
    private int physicalId;
    private int requesterGlobalThreadId;

    public GroupJoinConfirmMessage() {
        super(MessageType.GROUP_JOIN_CONFIRM);
    }

    public GroupJoinConfirmMessage(int requestNum, int groupId, int requesterGlobalThreadId, int physicalId) {
        this();

        this.requestNum = requestNum;
        this.groupId = groupId;
        this.requesterGlobalThreadId = requesterGlobalThreadId;
        this.physicalId = physicalId;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(requestNum);
        out.writeInt(groupId);
        out.writeInt(requesterGlobalThreadId);
        out.writeInt(physicalId);
    }

    @Override
    public void onReceive(SocketChannel sender, MessageDataInputStream in) throws IOException {
        requestNum = in.readInt();
        groupId = in.readInt();
        requesterGlobalThreadId = in.readInt();
        physicalId = in.readInt();

        NodeData nodeData = InternalPCJ.getNodeData();

        InternalCommonGroup commonGroup = nodeData.getCommonGroupById(groupId);

        GroupJoinStates states = commonGroup.getGroupJoinStates();
        GroupJoinStates.State state = states.get(requestNum, requesterGlobalThreadId);
        state.processNode(physicalId, commonGroup);
    }

}
