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
import org.pcj.internal.futures.GroupJoinState;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 *
 * @author faramir
 */
public class MessageGroupJoinResponse extends Message {

    private int requestNum;
    private int groupId;
    private int globalThreadId;
    private int groupThreadId;

    public MessageGroupJoinResponse() {
        super(MessageType.GROUP_JOIN_RESPONSE);
    }

    public MessageGroupJoinResponse(int requestNum, int groupId, int globalThreadId, int groupThreadId) {
        this();

        this.requestNum = requestNum;
        this.groupId = groupId;
        this.globalThreadId = globalThreadId;
        this.groupThreadId = groupThreadId;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(requestNum);
        out.writeInt(groupId);
        out.writeInt(globalThreadId);
        out.writeInt(groupThreadId);
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        requestNum = in.readInt();
        groupId = in.readInt();
        globalThreadId = in.readInt();
        groupThreadId = in.readInt();

        InternalCommonGroup commonGroup = InternalPCJ.getNodeData().getGroupById(groupId);

        GroupJoinState groupJoinState = commonGroup.removeGroupJoinState(requestNum, globalThreadId);
        groupJoinState.setGroupThreadId(groupThreadId);

        groupJoinState.getWaitObject().signalAll();
    }

}
