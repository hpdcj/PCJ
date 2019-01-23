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
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData.Node0Data;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.MessageType;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class GroupQueryMessage extends Message {

    private int requestNum;
    private int requesterPhysialId;
    private String groupName;

    public GroupQueryMessage() {
        super(MessageType.GROUP_JOIN_QUERY);
    }

    public GroupQueryMessage(int requestNum, int requesterPhysialId, String groupName) {
        this();

        this.requestNum = requestNum;
        this.requesterPhysialId = requesterPhysialId;
        this.groupName = groupName;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(requestNum);
        out.writeInt(requesterPhysialId);
        out.writeString(groupName);
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        requestNum = in.readInt();
        requesterPhysialId = in.readInt();
        groupName = in.readString();

        Node0Data node0Data = InternalPCJ.getNodeData().getNode0Data();

        int groupId = node0Data.getGroupId(groupName);
        int masterPhysicalId = node0Data.getGroupMaster(groupId, requesterPhysialId);

        GroupQueryAnswerMessage message = new GroupQueryAnswerMessage(requestNum, groupName, groupId, masterPhysicalId);
        InternalPCJ.getNetworker().send(sender, message);
    }

}
