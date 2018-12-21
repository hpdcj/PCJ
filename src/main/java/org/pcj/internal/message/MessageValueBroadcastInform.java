/*
 * Copyright (c) 2011-2018, PCJ Library, Marek Nowicki
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
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.Networker;
import org.pcj.internal.NodeData;
import org.pcj.internal.futures.BroadcastStates;
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
    private Queue<Exception> exceptions;

    public MessageValueBroadcastInform() {
        super(MessageType.VALUE_BROADCAST_INFORM);
    }

    public MessageValueBroadcastInform(int groupId, int requestNum, int requesterThreadId, Queue<Exception> exceptions) {
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

        NodeData nodeData = InternalPCJ.getNodeData();
        Networker networker = InternalPCJ.getNetworker();

        InternalCommonGroup group = nodeData.getGroupById(groupId);

        BroadcastStates states = group.getBroadcastStates();
        BroadcastStates.State state = states.getOrCreate(requestNum, requesterThreadId, group.getChildrenNodes().size());
        boolean exceptionOccurs = in.readBoolean();
        try {
            if (exceptionOccurs) {
                exceptions = (Queue<Exception>) in.readObject();
                exceptions.forEach(state::addException);
            }
        } catch (Exception ex) {
            state.addException(ex);
        }

        boolean lastPhysical = state.checkProcessed();
        if (lastPhysical) {
            int globalThreadId = group.getGlobalThreadId(requesterThreadId);
            int requesterPhysicalId = nodeData.getPhysicalId(globalThreadId);
            if (requesterPhysicalId != nodeData.getPhysicalId()) { // requester is going to receive response
                states.remove(state);
            }

            Message message;
            SocketChannel socket;

            int physicalId = nodeData.getPhysicalId();
            if (physicalId != group.getGroupMasterNode()) {
                int parentId = group.getParentNode();
                socket = nodeData.getSocketChannelByPhysicalId().get(parentId);

                message = new MessageValueBroadcastInform(groupId, requestNum, requesterThreadId, state.getExceptions());
            } else {
                message = new MessageValueBroadcastResponse(groupId, requestNum, requesterThreadId, state.getExceptions());

                socket = nodeData.getSocketChannelByPhysicalId().get(requesterPhysicalId);
            }

            networker.send(socket, message);
        }
    }
}
