/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.collect;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Map;
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
public final class CollectResponseMessage<T> extends Message {
    private int groupId;
    private int requestNum;
    private int requesterThreadId;
    private Map<Integer, T> valueMap;
    private Queue<Exception> exceptions;

    public CollectResponseMessage() {
        super(MessageType.COLLECT_RESPONSE);
    }

    CollectResponseMessage(int groupId, int requestNum, int requesterThreadId, Map<Integer, T> valueMap, Queue<Exception> exceptions) {
        this();

        this.groupId = groupId;
        this.valueMap = valueMap;
        this.requestNum = requestNum;
        this.requesterThreadId = requesterThreadId;
        this.exceptions = exceptions;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(groupId);
        out.writeInt(requestNum);
        out.writeInt(requesterThreadId);
        boolean exceptionOccurred = ((exceptions != null) && (!exceptions.isEmpty()));
        out.writeBoolean(exceptionOccurred);
        if (exceptionOccurred) {
            out.writeObject(exceptions);
        } else {
            out.writeObject(valueMap);
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
                valueMap = (Map<Integer, T>) in.readObject();
            } else {
                exceptions = (Queue<Exception>) in.readObject();
            }
        } catch (Exception ex) {
            exceptions = new ConcurrentLinkedQueue<>();
            exceptions.add(ex);
        }

        NodeData nodeData = InternalPCJ.getNodeData();
        InternalCommonGroup commonGroup = nodeData.getCommonGroupById(groupId);

        CollectStates states = commonGroup.getCollectStates();
        CollectStates.State<T> state = (CollectStates.State<T>) states.remove(requestNum, requesterThreadId);

        state.signal(valueMap, exceptions);
    }


}
