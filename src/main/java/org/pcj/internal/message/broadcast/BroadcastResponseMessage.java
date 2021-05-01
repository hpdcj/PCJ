/*
 * Copyright (c) 2011-2021, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.broadcast;

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
public final class BroadcastResponseMessage extends Message {

    private int groupId;
    private int requestNum;
    private int requesterThreadId;
    private Queue<Exception> exceptions;

    public BroadcastResponseMessage() {
        super(MessageType.VALUE_BROADCAST_RESPONSE);
    }

    public BroadcastResponseMessage(int groupId, int requestNum, int requesterThreadId, Queue<Exception> exceptions) {
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

        if ((exceptions != null) && (!exceptions.isEmpty())) {
            out.writeBoolean(true);
            out.writeObject(exceptions);
        } else {
            out.writeBoolean(false);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onReceive(SocketChannel sender, MessageDataInputStream in) throws IOException {
        groupId = in.readInt();
        requestNum = in.readInt();
        requesterThreadId = in.readInt();

        boolean exceptionOccurred = in.readBoolean();
        if (exceptionOccurred) {
            try {
                exceptions = (Queue<Exception>) in.readObject();
            } catch (Exception ex) {
                exceptions = new ConcurrentLinkedQueue<>();
                exceptions.add(ex);
            }
        }

        NodeData nodeData = InternalPCJ.getNodeData();
        InternalCommonGroup commonGroup = nodeData.getCommonGroupById(groupId);

        BroadcastStates states = commonGroup.getBroadcastStates();
        BroadcastStates.State state = states.getOrCreate(requestNum, requesterThreadId, commonGroup);
        state.upProcessNode(commonGroup, exceptions);
    }
}