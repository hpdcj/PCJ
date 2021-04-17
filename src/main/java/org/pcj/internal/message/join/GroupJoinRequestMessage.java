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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class GroupJoinRequestMessage extends Message {

    private int requestNum;
    private String groupName;
    private int groupId;
    private int physicalId;
    private int requesterGlobalThreadId;

    public GroupJoinRequestMessage() {
        super(MessageType.GROUP_JOIN_REQUEST);
    }

    public GroupJoinRequestMessage(int requestNum, String name, int groupId, int physicalId, int requesterGlobalThreadId) {
        this();

        this.requestNum = requestNum;
        this.groupName = name;
        this.groupId = groupId;
        this.physicalId = physicalId;
        this.requesterGlobalThreadId = requesterGlobalThreadId;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(requestNum);
        out.writeString(groupName);
        out.writeInt(groupId);
        out.writeInt(physicalId);
        out.writeInt(requesterGlobalThreadId);
    }

    @Override
    public void onReceive(SocketChannel sender, MessageDataInputStream in) throws IOException {
        this.requestNum = in.readInt();
        this.groupName = in.readString();
        this.groupId = in.readInt();
        this.physicalId = in.readInt();
        this.requesterGlobalThreadId = in.readInt();

        NodeData nodeData = InternalPCJ.getNodeData();
        InternalCommonGroup commonGroup = nodeData.getOrCreateGroup(nodeData.getCurrentNodePhysicalId(), groupId);

        commonGroup.addNewThread(requesterGlobalThreadId);

        Map<Integer, Integer> groupThreadsMap = new HashMap<>(commonGroup.getThreadsMap());

        int senderPhysicalId = nodeData.getPhysicalIdBySocketChannel(sender);
        commonGroup.getCommunicationTree().setParentNode(senderPhysicalId);

        List<Integer> childrenNodes = new ArrayList<>(commonGroup.getCommunicationTree().getChildrenNodes());

        GroupJoinInformMessage message
                = new GroupJoinInformMessage(requestNum, groupId, requesterGlobalThreadId, groupThreadsMap);

        GroupJoinStates states = commonGroup.getGroupJoinStates();
        GroupJoinStates.State state = states.create(requestNum, requesterGlobalThreadId, childrenNodes.size());

        childrenNodes.stream()
                .map(nodeData::getSocketChannelByPhysicalId)
                .forEach(socket -> InternalPCJ.getNetworker().send(socket, message));

        state.processNode(commonGroup);
    }
}
