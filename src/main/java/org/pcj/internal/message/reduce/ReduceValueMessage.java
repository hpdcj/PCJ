/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.reduce;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.MessageType;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public final class ReduceValueMessage<T> extends Message {

    private int groupId;
    private int requestNum;
    private int requesterThreadId;
    private T value;
    private Queue<Exception> exceptions;

    public ReduceValueMessage() {
        super(MessageType.REDUCE_VALUE);
    }

    ReduceValueMessage(int groupId, int requestNum, int requesterThreadId, T value, Queue<Exception> exceptions) {
        this();
        this.groupId = groupId;
        this.requestNum = requestNum;
        this.requesterThreadId = requesterThreadId;
        this.value = value;
        this.exceptions = exceptions;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(groupId);
        out.writeInt(requestNum);
        out.writeInt(requesterThreadId);
        boolean exception = ((exceptions != null) && (!exceptions.isEmpty()));
        out.writeBoolean(exception);
        if (exception) {
            out.writeObject(exceptions);
        } else {
            out.writeObject(value);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onReceive(SocketChannel sender, MessageDataInputStream in) throws IOException {
        groupId = in.readInt();
        requestNum = in.readInt();
        requesterThreadId = in.readInt();

        boolean exceptionOccurred = in.readBoolean();
        try {
            if (!exceptionOccurred) {
                value = (T) in.readObject();
            } else {
                exceptions = (Queue<Exception>) in.readObject();
            }
        } catch (Exception ex) {
            exceptions = new ConcurrentLinkedQueue<>();
            exceptions.add(ex);
        }

        NodeData nodeData = InternalPCJ.getNodeData();
        InternalCommonGroup commonGroup = nodeData.getCommonGroupById(groupId);

        ReduceStates states = commonGroup.getReduceStates();
        ReduceStates.State<T> state = (ReduceStates.State<T>) states.remove(requestNum, requesterThreadId);

        state.signal(value, exceptions);
    }
}
