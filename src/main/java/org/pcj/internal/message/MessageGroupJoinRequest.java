/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj.internal.message;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.futures.GroupJoinState;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 *
 * @author faramir
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
            commonGroup = new InternalCommonGroup(nodeData.getPhysicalId(),
                    groupId, groupName);
            commonGroup = nodeData.addGroup(commonGroup);
        }

        int groupThreadId = commonGroup.addNewThread(globalThreadId);

        MessageGroupJoinInform message
                = new MessageGroupJoinInform(requestNum, groupId, globalThreadId,
                        commonGroup.getThreadsMapping());

        GroupJoinState groupJoinState = commonGroup.getGroupJoinState(requestNum, globalThreadId, commonGroup.getChildrenNodes());
        groupJoinState.setGroupThreadId(groupThreadId);

        commonGroup.getChildrenNodes().stream()
                .map(nodeData.getSocketChannelByPhysicalId()::get)
                .forEach(socket -> InternalPCJ.getNetworker().send(socket, message));

        if (groupJoinState.processPhysical(nodeData.getPhysicalId())) {
            int requesterPhysicalId = nodeData.getPhysicalId(globalThreadId);
            if (requesterPhysicalId != nodeData.getPhysicalId()) {
                commonGroup.removeGroupJoinState(requestNum, globalThreadId);
            }
        }
    }
}
