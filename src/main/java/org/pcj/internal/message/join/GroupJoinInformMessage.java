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
import java.util.List;
import java.util.Map;
import org.pcj.PcjRuntimeException;
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
public class GroupJoinInformMessage extends Message {

    private int requestNum;
    private int groupId;
    private int requesterGlobalThreadId;
    private Map<Integer, Integer> groupThreadsMap;

    public GroupJoinInformMessage() {
        super(MessageType.GROUP_JOIN_INFORM);
    }

    public GroupJoinInformMessage(int requestNum, int groupId, int requesterGlobalThreadId, Map<Integer, Integer> groupThreadsMap) {
        this();

        this.requestNum = requestNum;
        this.groupId = groupId;
        this.requesterGlobalThreadId = requesterGlobalThreadId;
        this.groupThreadsMap = groupThreadsMap;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(requestNum);
        out.writeInt(groupId);
        out.writeInt(requesterGlobalThreadId);
        out.writeObject(groupThreadsMap);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onReceive(SocketChannel sender, MessageDataInputStream in) throws IOException {
        requestNum = in.readInt();
        groupId = in.readInt();
        requesterGlobalThreadId = in.readInt();

        try {
            groupThreadsMap = (Map<Integer, Integer>) in.readObject();
        } catch (Exception ex) {
            throw new PcjRuntimeException("Unable to read groupThreadsMap", ex);
        }

        NodeData nodeData = InternalPCJ.getNodeData();
        InternalCommonGroup commonGroup = nodeData.getCommonGroupById(groupId);

        commonGroup.updateThreadsMap(groupThreadsMap);

        int senderPhysicalId = nodeData.getPhysicalIdBySocketChannel(sender);
        commonGroup.getCommunicationTree().setParentNode(senderPhysicalId);

        List<Integer> childrenNodes = new ArrayList<>(commonGroup.getCommunicationTree().getChildrenNodes());

        GroupJoinStates states = commonGroup.getGroupJoinStates();
        GroupJoinStates.State state = states.create(requestNum, requesterGlobalThreadId, childrenNodes.size());

        childrenNodes.stream()
                .map(nodeData::getSocketChannelByPhysicalId)
                .forEach(socket -> InternalPCJ.getNetworker().send(socket, this));

        state.processNode(commonGroup);
    }
}
