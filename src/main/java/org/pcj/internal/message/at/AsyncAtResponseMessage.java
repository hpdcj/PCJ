/*
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.at;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import org.pcj.internal.InternalGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.PcjThread;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.MessageType;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class AsyncAtResponseMessage extends Message {

    private int groupId;
    private int requestNum;
    private int requesterThreadId;
    private Object variableValue;
    private Exception exception;

    public AsyncAtResponseMessage() {
        super(MessageType.ASYNC_AT_RESPONSE);
    }

    public AsyncAtResponseMessage(int groupId, int requestNum, int requesterThreadId, Object variableValue) {
        this();

        this.groupId = groupId;
        this.requestNum = requestNum;
        this.requesterThreadId = requesterThreadId;
        this.variableValue = variableValue;
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
        if (exception == null) {
            out.writeObject(variableValue);
        } else {
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

        PcjThread pcjThread = nodeData.getPcjThread(globalThreadId);
        InternalGroup group = pcjThread.getThreadData().getGroupById(groupId);

        boolean exceptionOccurs = in.readBoolean();
        try {
            if (!exceptionOccurs) {
                variableValue = in.readObject();
            } else {
                exception = (Exception) in.readObject();
            }
        } catch (Exception ex) {
            exception = ex;
        }

        AsyncAtStates states = group.getAsyncAtStates();
        AsyncAtStates.State<?> state = states.remove(requestNum);
        state.signal(variableValue, exception);
    }
}
