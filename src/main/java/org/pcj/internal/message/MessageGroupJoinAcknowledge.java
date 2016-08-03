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
public class MessageGroupJoinAcknowledge extends Message {

    private int requestNum;
    private String name;
    private int groupId;
    private int physicalId;

    public MessageGroupJoinAcknowledge() {
        super(MessageType.GROUP_JOIN_ACKNOWLEDGE);
    }

    public MessageGroupJoinAcknowledge(int requestNum, String name, int groupId, int physicalId) {
        this();

        this.requestNum = requestNum;
        this.name = name;
        this.groupId = groupId;
        this.physicalId = physicalId;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(requestNum);
        out.writeString(name);
        out.writeInt(groupId);
        out.writeInt(physicalId);
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
    }

}
