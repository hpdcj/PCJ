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
public class MessageGroupJoinConfirm extends Message {

    private int requestNum;
    private int groupId;
    private int physicalId;
    private int globalThreadId;

    public MessageGroupJoinConfirm() {
        super(MessageType.GROUP_JOIN_CONFIRM);
    }

    public MessageGroupJoinConfirm(int requestNum, int groupId, int globalThreadId, int physicalId) {
        this();

        this.requestNum = requestNum;
        this.groupId = groupId;
        this.globalThreadId = globalThreadId;
        this.physicalId = physicalId;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(requestNum);
        out.writeInt(groupId);
        out.writeInt(globalThreadId);
        out.writeInt(physicalId);
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        requestNum = in.readInt();
        groupId = in.readInt();
        globalThreadId = in.readInt();
        physicalId = in.readInt();

        NodeData nodeData = InternalPCJ.getNodeData();

        InternalCommonGroup commonGroup = nodeData.getGroupById(groupId);

        GroupJoinState groupJoinState = commonGroup.getGroupJoinState(requestNum, globalThreadId, commonGroup.getChildrenNodes());

        if (groupJoinState.processPhysical(physicalId)) {
            int requesterPhysicalId = nodeData.getPhysicalId(globalThreadId);
            if (requesterPhysicalId != nodeData.getPhysicalId()) {
                commonGroup.removeGroupJoinState(requestNum, globalThreadId);
            }
        }
    }

}
