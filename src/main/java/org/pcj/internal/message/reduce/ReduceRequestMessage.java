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
import org.pcj.PcjRuntimeException;
import org.pcj.ReduceOperation;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.Networker;
import org.pcj.internal.NodeData;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.MessageType;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public final class ReduceRequestMessage<T> extends Message {

    private int groupId;
    private int requestNum;
    private int requesterThreadId;
    private String sharedEnumClassName;
    private String variableName;
    private int[] indices;
    private ReduceOperation<T> function;

    public ReduceRequestMessage() {
        super(MessageType.REDUCE_REQUEST);
    }

    public ReduceRequestMessage(int groupId, int requestNum, int requesterThreadId, String storageName, String variableName, int[] indices, ReduceOperation<T> function) {
        this();

        this.groupId = groupId;
        this.requestNum = requestNum;
        this.requesterThreadId = requesterThreadId;
        this.sharedEnumClassName = storageName;
        this.variableName = variableName;
        this.indices = indices;
        this.function = function;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(groupId);
        out.writeInt(requestNum);
        out.writeInt(requesterThreadId);
        out.writeString(sharedEnumClassName);
        out.writeString(variableName);
        out.writeIntArray(indices);
        out.writeObject(function);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onReceive(SocketChannel sender, MessageDataInputStream in) throws IOException {
        groupId = in.readInt();
        requestNum = in.readInt();
        requesterThreadId = in.readInt();

        sharedEnumClassName = in.readString();
        variableName = in.readString();
        indices = in.readIntArray();
        try {
            function = (ReduceOperation<T>) in.readObject();
        } catch (ClassNotFoundException e) {
            throw new PcjRuntimeException(e);
        }

        NodeData nodeData = InternalPCJ.getNodeData();
        InternalCommonGroup commonGroup = nodeData.getCommonGroupById(groupId);

        ReduceStates states = commonGroup.getReduceStates();
        ReduceStates.State<T> state = states.getOrCreate(requestNum, requesterThreadId, commonGroup);

        Networker networker = InternalPCJ.getNetworker();
        commonGroup.getCommunicationTree().getChildrenNodes()
                .stream()
                .map(nodeData::getSocketChannelByPhysicalId)
                .forEach(socket -> networker.send(socket, this));

        state.downProcessNode(commonGroup, sharedEnumClassName, variableName, indices, function);
    }
}