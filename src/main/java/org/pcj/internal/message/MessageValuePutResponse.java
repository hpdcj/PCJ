/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj.internal.message;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import org.pcj.PcjRuntimeException;
import org.pcj.internal.InternalGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.PcjThread;
import org.pcj.internal.futures.PutVariable;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 *
 * @author faramir
 */
class MessageValuePutResponse extends Message {

    private int requestNum;
    private int groupId;
    private int requesterThreadId;
    private Exception exception;

    public MessageValuePutResponse() {
        super(MessageType.VALUE_PUT_RESPONSE);
    }

    public MessageValuePutResponse(int requestNum, int groupId, int requesterThreadId) {
        this();
        this.requestNum = requestNum;
        this.groupId = groupId;
        this.requesterThreadId = requesterThreadId;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(requestNum);
        out.writeInt(groupId);
        out.writeInt(requesterThreadId);
        out.writeBoolean(exception != null);
        if (exception != null) {
            out.writeObject(exception);
        }
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        requestNum = in.readInt();
        groupId = in.readInt();
        requesterThreadId = in.readInt();

        NodeData nodeData = InternalPCJ.getNodeData();
        int globalThreadId = nodeData.getGroupById(groupId).getGlobalThreadId(requesterThreadId);

        PcjThread pcjThread = nodeData.getPcjThreads().get(globalThreadId);
        InternalGroup group = pcjThread.getThreadData().getGroupById(groupId);

        PutVariable putVariable = group.getPutVariableMap().remove(requestNum);

        boolean exceptionOccurs = in.readBoolean();
        try {
            if (exceptionOccurs) {
                exception = (Exception) in.readObject();
                putVariable.setException(exception);
            } else {
                putVariable.signalAll();
            }
        } catch (Exception ex) {
            putVariable.setException(new PcjRuntimeException(ex));
        }
    }

}
