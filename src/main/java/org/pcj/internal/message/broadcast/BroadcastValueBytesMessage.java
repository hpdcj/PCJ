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
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.Networker;
import org.pcj.internal.NodeData;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.MessageType;
import org.pcj.internal.network.CloneInputStream;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class BroadcastValueBytesMessage extends Message {

    private int groupId;
    private int requestNum;
    private int requesterThreadId;
    private String sharedEnumClassName;
    private String variableName;
    private int[] indices;
    private CloneInputStream clonedData;

    public BroadcastValueBytesMessage() {
        super(MessageType.VALUE_BROADCAST_BYTES);
    }

    public BroadcastValueBytesMessage(int groupId, int requestNum, int requesterThreadId, String storageName, String variableName, int[] indices, CloneInputStream clonedData) {
        this();

        this.groupId = groupId;
        this.requestNum = requestNum;
        this.requesterThreadId = requesterThreadId;
        this.sharedEnumClassName = storageName;
        this.variableName = variableName;
        this.indices = indices;

        this.clonedData = clonedData;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(groupId);
        out.writeInt(requestNum);
        out.writeInt(requesterThreadId);
        out.writeString(sharedEnumClassName);
        out.writeString(variableName);
        out.writeIntArray(indices);

        clonedData.writeInto(out);
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        groupId = in.readInt();
        requestNum = in.readInt();
        requesterThreadId = in.readInt();

        sharedEnumClassName = in.readString();
        variableName = in.readString();
        indices = in.readIntArray();

        clonedData = CloneInputStream.readFrom(in);

        NodeData nodeData = InternalPCJ.getNodeData();
        Networker networker = InternalPCJ.getNetworker();

        BroadcastValueBytesMessage broadcastValueBytesMessage
                = new BroadcastValueBytesMessage(groupId, requestNum, requesterThreadId, sharedEnumClassName, variableName, indices, clonedData);

        InternalCommonGroup commonGroup = nodeData.getCommonGroupById(groupId);
        commonGroup.getChildrenNodes()
                .stream()
                .map(nodeData.getSocketChannelByPhysicalId()::get)
                .forEach(socket -> networker.send(socket, broadcastValueBytesMessage));

        BroadcastStates states = commonGroup.getBroadcastStates();
        BroadcastStates.State state = states.getOrCreate(requestNum, requesterThreadId, commonGroup.getChildrenNodes().size());
        state.downProcessNode(commonGroup, clonedData, sharedEnumClassName, variableName, indices);
    }
}