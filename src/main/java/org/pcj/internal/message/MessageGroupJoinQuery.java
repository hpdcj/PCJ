/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj.internal.message;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData.Node0Data;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 *
 * @author faramir
 */
public class MessageGroupJoinQuery extends Message {

    private int requestNum;
    private int requesterPhysialId;
    private String groupName;

    public MessageGroupJoinQuery() {
        super(MessageType.GROUP_JOIN_QUERY);
    }

    public MessageGroupJoinQuery(int requestNum, int requesterPhysialId, String groupName) {
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
        
        MessageGroupJoinAnswer message = new MessageGroupJoinAnswer(requestNum, groupName, groupId, masterPhysicalId);
        InternalPCJ.getNetworker().send(sender, message);
    }

}
