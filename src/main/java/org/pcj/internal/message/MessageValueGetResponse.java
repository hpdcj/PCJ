/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj.internal.message;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import org.pcj.Group;
import org.pcj.PcjRuntimeException;
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

    @Override
    public void readObjects(MessageDataInputStream in) throws IOException, ClassNotFoundException {
        requestNum = in.readInt();
        groupId = in.readInt();
        requesterThreadId = in.readInt();
        variableValue = in.readObject();
    }

    @Override
    public void writeObjects(MessageDataOutputStream out) throws IOException {
        out.writeInt(requestNum);
        out.writeInt(groupId);
        out.writeInt(requesterThreadId);
        out.writeObject(variableValue);
    }

    @Override
    public String paramsToString() {
        return String.format("requestNum:%d,"
                + "groupId:%d,"
                + "requesterThreadId:%d,"
                + "object:%s",
                requestNum, groupId, requesterThreadId, Objects.toString(variableValue));
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        try {
            readObjects(in);
        } catch (ClassNotFoundException ex) {
            // TODO: wyjatek przerzucic do wywolujacego
            throw new PcjRuntimeException(ex);
        }

        NodeData nodeData = InternalPCJ.getNodeData();
        int globalThreadId = nodeData.getGroupById(groupId).getGlobalThreadId(requesterThreadId);

        PcjThread pcjThread = nodeData.getPcjThreads().get(globalThreadId);
        Group group = pcjThread.getThreadData().getGroupById(groupId);

        GetVariable getVariable = group.getGetVariableMap().remove(requestNum);
        getVariable.setVariableValue(variableValue);
    }

}
