/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Queue;
import org.pcj.PcjRuntimeException;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.futures.BroadcastState;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * ....
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageValueBroadcastResponse extends Message {
    
    private int requestNum;
    private int groupId;
    private int requesterThreadId;
    private Queue<Exception> exceptions;
    
    public MessageValueBroadcastResponse() {
        super(MessageType.VALUE_BROADCAST_RESPONSE);
    }
    
    public MessageValueBroadcastResponse(int groupId, int requestNum, int requesterThreadId, Queue<Exception> exceptions) {
        this();
        
        this.groupId = groupId;
        this.requestNum = requestNum;
        this.requesterThreadId = requesterThreadId;
        this.exceptions = exceptions;
    }
    
    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(groupId);
        out.writeInt(requestNum);
        out.writeInt(requesterThreadId);
        
        if ((exceptions != null) && (exceptions.isEmpty() == false)) {
            out.writeBoolean(true);
            out.writeObject(exceptions);
        } else {
            out.writeBoolean(false);
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        groupId = in.readInt();
        requestNum = in.readInt();
        requesterThreadId = in.readInt();
        
        NodeData nodeData = InternalPCJ.getNodeData();
        InternalCommonGroup group = nodeData.getGroupById(groupId);
        
        BroadcastState broadcastState = group.getBroadcastState(requestNum, requesterThreadId);
        
        boolean exceptionOccurs = in.readBoolean();
        try {
            if (!exceptionOccurs) {
                broadcastState.signalDone();
            } else {
                exceptions = (Queue<Exception>) in.readObject();
                
                RuntimeException ex = new RuntimeException("Exception while broadcasting value.");
                exceptions.forEach(ex::addSuppressed);
                
                broadcastState.signalException(ex);
            }
        } catch (Exception ex) {
            broadcastState.signalException(ex);
        }
        
    }
}
