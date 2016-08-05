/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj.internal.message;

import java.io.IOException;
import java.nio.channels.SocketChannel;
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

    public MessageValuePutResponse(int groupId, int requestNum, int requesterThreadId) {
        this();
        this.groupId = groupId;
        this.requestNum = requestNum;
        this.requesterThreadId = requesterThreadId;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(groupId);
        out.writeInt(requestNum);
        out.writeInt(requesterThreadId);
        out.writeBoolean(exception != null);
        if (exception != null) {
            out.writeObject(exception);
        }
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        groupId = in.readInt();
        requestNum = in.readInt();
        requesterThreadId = in.readInt();

        NodeData nodeData = InternalPCJ.getNodeData();
        int globalThreadId = nodeData.getGroupById(groupId).getGlobalThreadId(requesterThreadId);

        PcjThread pcjThread = nodeData.getPcjThreads().get(globalThreadId);
        InternalGroup group = (InternalGroup) pcjThread.getThreadData().getGroupById(groupId);

        PutVariable putVariable = group.removePutVariable(requestNum);

        boolean exceptionOccurs = in.readBoolean();
        try {
            if (exceptionOccurs) {
                exception = (Exception) in.readObject();
                putVariable.signalException(exception);
            } else {
                putVariable.signalDone();
            }
        } catch (Exception ex) {
            putVariable.signalException(ex);
        }
    }

}
