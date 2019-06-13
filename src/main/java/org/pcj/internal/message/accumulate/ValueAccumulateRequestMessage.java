/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.accumulate;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import org.pcj.ReduceOperation;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.InternalStorages;
import org.pcj.internal.NodeData;
import org.pcj.internal.PcjThread;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.MessageType;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public final class ValueAccumulateRequestMessage<T> extends Message {

    private int requestNum;
    private int groupId;
    private int requesterThreadId;
    private int threadId;
    private String sharedEnumClassName;
    private String name;
    private int[] indices;
    private T newValue;
    private ReduceOperation<T> function;

    public ValueAccumulateRequestMessage() {
        super(MessageType.VALUE_ACCUMULATE_REQUEST);
    }

    public ValueAccumulateRequestMessage(int groupId, int requestNum, int requesterThreadId, int threadId, String storageName, String name, int[] indices, ReduceOperation<T> function, T newValue) {
        this();

        this.groupId = groupId;
        this.requestNum = requestNum;
        this.requesterThreadId = requesterThreadId;
        this.threadId = threadId;
        this.sharedEnumClassName = storageName;
        this.name = name;
        this.indices = indices;
        this.function = function;
        this.newValue = newValue;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(groupId);
        out.writeInt(requestNum);
        out.writeInt(requesterThreadId);
        out.writeInt(threadId);
        out.writeString(sharedEnumClassName);
        out.writeString(name);
        out.writeIntArray(indices);
        out.writeObject(function);
        out.writeObject(newValue);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onReceive(SocketChannel sender, MessageDataInputStream in) throws IOException {
        groupId = in.readInt();
        requestNum = in.readInt();
        requesterThreadId = in.readInt();
        threadId = in.readInt();
        sharedEnumClassName = in.readString();
        name = in.readString();
        indices = in.readIntArray();

        NodeData nodeData = InternalPCJ.getNodeData();
        PcjThread pcjThread = nodeData.getPcjThread(groupId, threadId);

        InternalStorages storage = pcjThread.getThreadData().getStorages();

        ValueAccumulateResponseMessage valueAccumulateResponseMessage = new ValueAccumulateResponseMessage(groupId, requestNum, requesterThreadId);
        try {
            function = (ReduceOperation<T>) in.readObject();
            newValue = (T) in.readObject();
            storage.accumulate(function, newValue, sharedEnumClassName, name, indices);
        } catch (Exception ex) {
            valueAccumulateResponseMessage.setException(ex);
        }

        InternalPCJ.getNetworker().send(sender, valueAccumulateResponseMessage);
    }
}
