/* 
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.get;

import java.io.IOException;
import java.nio.channels.SocketChannel;
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
public class ValueGetRequestMessage extends Message {

    private int groupId;
    private int requestNum;
    private int requesterThreadId;
    private int threadId;
    private String sharedEnumClassName;
    private String name;
    private int[] indices;

    public ValueGetRequestMessage() {
        super(MessageType.VALUE_GET_REQUEST);
    }

    public ValueGetRequestMessage(int groupId, int requestNum, int requesterThreadId, int threadId, String storageName, String name, int[] indices) {
        this();

        this.groupId = groupId;
        this.requestNum = requestNum;
        this.requesterThreadId = requesterThreadId;
        this.threadId = threadId;
        this.sharedEnumClassName = storageName;
        this.name = name;
        this.indices = indices;
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
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
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

        ValueGetResponseMessage valueGetResponseMessage;
        try {
            Object variableValue = storage.get(sharedEnumClassName, name, indices);
            valueGetResponseMessage = new ValueGetResponseMessage(groupId, requestNum, requesterThreadId, variableValue);
        } catch (Exception ex) {
            valueGetResponseMessage = new ValueGetResponseMessage(groupId, requestNum, requesterThreadId, ex);
        }

        InternalPCJ.getNetworker().send(sender, valueGetResponseMessage);
    }
}
