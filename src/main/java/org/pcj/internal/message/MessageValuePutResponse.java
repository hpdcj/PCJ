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

    public MessageValuePutResponse() {
        super(MessageType.VALUE_PUT_RESPONSE);
    }

    public MessageValuePutResponse(int requestNum, int groupId, int requesterThreadId) {
        this();
        this.requestNum = requestNum;
        this.groupId = groupId;
        this.requesterThreadId = requesterThreadId;
    }

    @Override
    public void readObjects(MessageDataInputStream in) throws IOException, ClassNotFoundException {
        requestNum = in.readInt();
        groupId = in.readInt();
        requesterThreadId = in.readInt();
    }

    @Override
    public void writeObjects(MessageDataOutputStream out) throws IOException {
        out.writeInt(requestNum);
        out.writeInt(groupId);
        out.writeInt(requesterThreadId);
    }

    @Override
    public String paramsToString() {
        return String.format("requestNum:%d,"
                + "groupId:%d,"
                + "requesterThreadId:%d",
                requestNum, groupId, requesterThreadId);
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
        InternalGroup group = pcjThread.getThreadData().getGroupById(groupId);

        PutVariable putVariable = group.getPutVariableMap().remove(requestNum);
        putVariable.signalAll();
    }

}
