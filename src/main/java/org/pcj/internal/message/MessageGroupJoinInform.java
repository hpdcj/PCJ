/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj.internal.message;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 *
 * @author faramir
 */
public class MessageGroupJoinInform extends Message {

    private int requestNum;
    private String name;
    private int groupId;
    private int globalThreadId;
    private int groupThreadId;

    public MessageGroupJoinInform() {
        super(MessageType.GROUP_JOIN_INFORM);
    }

    public MessageGroupJoinInform(int requestNum, String name, int groupId, int globalThreadId, int groupThreadId) {
        this();

        this.requestNum = requestNum;
        this.name = name;
        this.groupId = groupId;
        this.globalThreadId = globalThreadId;
        this.groupThreadId = groupThreadId;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(requestNum);
        out.writeString(name);
        out.writeInt(groupId);
        out.writeInt(globalThreadId);
        out.writeInt(groupThreadId);
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
    }

}
