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
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.futures.GroupJoinQuery;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class MessageGroupJoinAnswer extends Message {

    private int requestNum;
    private String groupName;
    private int groupId;
    private int masterPhysicalId;

    public MessageGroupJoinAnswer() {
        super(MessageType.GROUP_JOIN_ANSWER);
    }

    public MessageGroupJoinAnswer(int requestNum, String name, int groupId, int masterPhysicalId) {
        this();

        this.requestNum = requestNum;
        this.groupName = name;
        this.groupId = groupId;
        this.masterPhysicalId = masterPhysicalId;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(requestNum);
        out.writeString(groupName);
        out.writeInt(groupId);
        out.writeInt(masterPhysicalId);
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        this.requestNum = in.readInt();
        this.groupName = in.readString();
        this.groupId = in.readInt();
        this.masterPhysicalId = in.readInt();

        GroupJoinQuery groupQuery = InternalPCJ.getNodeData().getGroupJoinQuery(requestNum);
        groupQuery.setGroupId(groupId);
        groupQuery.setGroupMasterId(masterPhysicalId);

        groupQuery.getWaitObject().signalAll();
    }

}
