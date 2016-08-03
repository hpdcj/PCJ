/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj.internal.message;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Map;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 *
 * @author faramir
 */
public class MessageGroupJoinResponse extends Message {
    
    private int requestNum;
    private String name;
    private int groupId;
    private Map<Integer, Integer> threadsMapping;
    
    public MessageGroupJoinResponse() {
        super(MessageType.GROUP_JOIN_RESPONSE);
    }
    
    public MessageGroupJoinResponse(int requestNum, String name, int groupId, Map<Integer, Integer> threadsMapping) {
        this();
        
        this.requestNum = requestNum;
        this.name = name;
        this.groupId = groupId;
        this.threadsMapping = threadsMapping;
    }
    
    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(requestNum);
        out.writeString(name);
        out.writeInt(groupId);
        out.writeObject(threadsMapping);
    }
    
    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
    }
    
}
