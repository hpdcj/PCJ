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
public class GroupQueryAnswerMessage extends Message {

    private int requestNum;
    private String groupName;
    private int groupId;
    private int masterPhysicalId;

    public GroupQueryAnswerMessage() {
        super(MessageType.GROUP_JOIN_ANSWER);
    }

    public GroupQueryAnswerMessage(int requestNum, String name, int groupId, int masterPhysicalId) {
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
    public void onReceive(SocketChannel sender, MessageDataInputStream in) throws IOException {
        this.requestNum = in.readInt();
        this.groupName = in.readString();
        this.groupId = in.readInt();
        this.masterPhysicalId = in.readInt();

        NodeData nodeData = InternalPCJ.getNodeData();
        InternalCommonGroup internalCommonGroup = nodeData.getOrCreateGroup(masterPhysicalId, groupId, groupName);

        GroupQueryStates states = nodeData.getGroupQueryStates();
        GroupQueryStates.State state = states.remove(requestNum);
        state.signal(internalCommonGroup);
    }
}
