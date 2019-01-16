/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
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
final public class BroadcastValueResponseMessage extends Message {

    private int groupId;
    private int requestNum;
    private int requesterThreadId;
    private Queue<Exception> exceptions;

    public BroadcastValueResponseMessage() {
        super(MessageType.VALUE_BROADCAST_RESPONSE);
    }

    public BroadcastValueResponseMessage(int groupId, int requestNum, int requesterThreadId, Queue<Exception> exceptions) {
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

        boolean exceptionOccurs = in.readBoolean();
        if (exceptionOccurs) {
            try {
                exceptions = (Queue<Exception>) in.readObject();
            } catch (Exception ex) {
                exceptions = new ConcurrentLinkedQueue<>();
                exceptions.add(ex);
            }
        }

        NodeData nodeData = InternalPCJ.getNodeData();
        PcjThread pcjThread = nodeData.getPcjThread(groupId, requesterThreadId);

        InternalGroup group = pcjThread.getThreadData().getGroupById(groupId);

        BroadcastStates states = group.getBroadcastStates();
        BroadcastStates.State state = states.remove(requestNum, requesterThreadId);
        state.signal(exceptions);
    }
}
