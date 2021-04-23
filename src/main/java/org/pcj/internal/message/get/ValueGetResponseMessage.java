/*
 * Copyright (c) 2011-2021, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.get;

import java.io.IOException;
import java.io.WriteAbortedException;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
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
public class ValueGetResponseMessage extends Message {

    private int groupId;
    private int requestNum;
    private int requesterThreadId;
    private Object variableValue;
    private Exception exception;

    public ValueGetResponseMessage() {
        super(MessageType.VALUE_GET_RESPONSE);
    }

    private ValueGetResponseMessage(int groupId, int requestNum, int requesterThreadId) {
        this();

        this.groupId = groupId;
        this.requestNum = requestNum;
        this.requesterThreadId = requesterThreadId;
    }

    public ValueGetResponseMessage(int groupId, int requestNum, int requesterThreadId, Object variableValue) {
        this(groupId, requestNum, requesterThreadId);

        this.variableValue = variableValue;
    }

    public ValueGetResponseMessage(int groupId, int requestNum, int requesterThreadId, Exception exception) {
        this(groupId, requestNum, requesterThreadId);

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
        } else {
            out.writeObject(variableValue);
        }
    }

    @Override
    public void onReceive(SocketChannel sender, MessageDataInputStream in) throws IOException {
        groupId = in.readInt();
        requestNum = in.readInt();
        requesterThreadId = in.readInt();

        boolean exceptionOccurred = in.readBoolean();
        try {
            if (!exceptionOccurred) {
                variableValue = in.readObject();
            } else {
                exception = (Exception) in.readObject();
            }
        } catch (WriteAbortedException ex) {
            LOGGER.log(Level.WARNING, "WriteAbortedException occurred: {0}", ex.getMessage());
            return;
        } catch (Exception ex) {
            exception = ex;
        }

        NodeData nodeData = InternalPCJ.getNodeData();
        PcjThread pcjThread = nodeData.getPcjThread(groupId, requesterThreadId);

        InternalGroup group = pcjThread.getThreadData().getGroupById(groupId);

        ValueGetStates states = group.getValueGetStates();
        ValueGetStates.State<?> state = states.remove(requestNum);
        state.signal(variableValue, exception);
    }
}
