/* 
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import org.pcj.internal.InternalGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.futures.BroadcastState;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * ....
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageValueBroadcastInform extends Message {

    private int groupId;
    private int requestNum;
    private int requesterThreadId;
    private int physicalId;
    private Queue<Exception> exceptions;

    public MessageValueBroadcastInform() {
        super(MessageType.VALUE_BROADCAST_INFORM);
    }

    public MessageValueBroadcastInform(int groupId, int requestNum, int requesterThreadId, int physicalId, Queue<Exception> exceptions) {
        this();

        this.groupId = groupId;
        this.requestNum = requestNum;
        this.requesterThreadId = requesterThreadId;
        this.physicalId = physicalId;
        this.exceptions = exceptions;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(groupId);
        out.writeInt(requestNum);
        out.writeInt(requesterThreadId);
        out.writeInt(physicalId);

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
        physicalId = in.readInt();

        NodeData nodeData = InternalPCJ.getNodeData();
        
        
        InternalGroup group = nodeData.getPcjThread(requesterThreadId).getThreadData().getGroupById(groupId);

        BroadcastState broadcastState = group.getBroadcastState(requestNum);
        boolean exceptionOccurs = in.readBoolean();
        try {
            if (exceptionOccurs) {
                exceptions = (Queue<Exception>) in.readObject();
                exceptions.forEach(broadcastState::addException);
            }
        } catch (Exception ex) {
            broadcastState.addException(ex);
        }

        if (broadcastState.processPhysical(physicalId)) {
            if (broadcastState.isExceptionOccurs() == false) {
                broadcastState.signalDone();
            } else {
                RuntimeException ex = new RuntimeException("Exception while broadcasting value.");
                broadcastState.getExceptions().forEach(ex::addSuppressed);

                broadcastState.signalException(ex);
            }
        }
    }
}
