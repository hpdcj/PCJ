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
import org.pcj.internal.futures.GetVariable;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 *
 * @author faramir
 */
class MessageValueGetResponse extends Message {

    private int requestNum;
    private int groupId;
    private int requesterThreadId;
    private Object variableValue;
    private Exception exception;

    public MessageValueGetResponse() {
        super(MessageType.VALUE_GET_RESPONSE);
    }

    public MessageValueGetResponse(int requestNum, int groupId, int requesterThreadId, Object variableValue) {
        this();
        this.requestNum = requestNum;
        this.groupId = groupId;
        this.requesterThreadId = requesterThreadId;
        this.variableValue = variableValue;
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
        if (exception == null) {
            out.writeObject(variableValue);
        } else {
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

        GetVariable getVariable = group.getGetVariableMap().remove(requestNum);

        boolean exceptionOccurs = in.readBoolean();
        try {
            if (!exceptionOccurs) {
                variableValue = in.readObject();
                getVariable.setVariableValue(variableValue);
            } else {
                exception = (Exception) in.readObject();
                getVariable.setException(exception);
            }
        } catch (Exception ex) {
            getVariable.setException(new PcjRuntimeException(ex));
        }

    }

}
