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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.futures.GroupJoinState;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class MessageGroupJoinRequest extends Message {

    private int requestNum;
    private String groupName;
    private int groupId;
    private int physicalId;
    private int globalThreadId;

    public MessageGroupJoinRequest() {
        super(MessageType.GROUP_JOIN_REQUEST);
    }

    public MessageGroupJoinRequest(int requestNum, String name, int groupId, int physicalId, int globalThreadId) {
        this();

        this.requestNum = requestNum;
        this.groupName = name;
        this.groupId = groupId;
        this.physicalId = physicalId;
        this.globalThreadId = globalThreadId;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(requestNum);
        out.writeString(groupName);
        out.writeInt(groupId);
        out.writeInt(physicalId);
        out.writeInt(globalThreadId);
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        this.requestNum = in.readInt();
        this.groupName = in.readString();
        this.groupId = in.readInt();
        this.physicalId = in.readInt();
        this.globalThreadId = in.readInt();

        NodeData nodeData = InternalPCJ.getNodeData();
        InternalCommonGroup commonGroup = nodeData.getGroupById(groupId);
        if (commonGroup == null) {
            commonGroup = nodeData.createGroup(nodeData.getPhysicalId(), groupId, groupName);
        }

        commonGroup.addNewThread(globalThreadId);

        int currentPhysicalId = nodeData.getPhysicalId();
        Map<Integer, Integer> threadsMapping = new HashMap<>(commonGroup.getThreadsMapping());
        CopyOnWriteArrayList<Integer> physicalIds = new CopyOnWriteArrayList<>();
        physicalIds.add(commonGroup.getGroupMasterNode());
        List<Integer> childrenNodes = threadsMapping.keySet().stream()
                .sorted()
                .mapToInt(threadsMapping::get)
                .map(nodeData::getPhysicalId)
                .filter(physicalIds::addIfAbsent)
                .map(physicalIds::lastIndexOf)
                .filter(index -> index > 0)
                .filter(index -> physicalIds.get((index - 1) / 2) == currentPhysicalId)
                .map(physicalIds::get)
                .boxed().collect(Collectors.toList());

        MessageGroupJoinInform message
                = new MessageGroupJoinInform(requestNum, groupId, globalThreadId,
                        threadsMapping);

        GroupJoinState groupJoinState = commonGroup.getGroupJoinState(requestNum, globalThreadId, childrenNodes);

        childrenNodes.stream()
                .map(nodeData.getSocketChannelByPhysicalId()::get)
                .forEach(socket -> InternalPCJ.getNetworker().send(socket, message));

        if (groupJoinState.processPhysical(currentPhysicalId)) {
            commonGroup.removeGroupJoinState(requestNum, globalThreadId);
        }
    }
}
