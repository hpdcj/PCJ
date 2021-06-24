/*
 * Copyright (c) 2011-2021, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.collect;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.stream.Collector;
import org.pcj.PcjRuntimeException;
import org.pcj.SerializableSupplier;
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
public final class CollectRequestMessage<T, R> extends Message {

    private int groupId;
    private int requestNum;
    private int requesterThreadId;
    private String sharedEnumClassName;
    private String variableName;
    private int[] indices;
    private SerializableSupplier<Collector<T, ?, R>> collectorSupplier;

    public CollectRequestMessage() {
        super(MessageType.COLLECT_REQUEST);
    }

    public CollectRequestMessage(int groupId, int requestNum, int requesterThreadId, String storageName, String variableName, int[] indices,
                                 SerializableSupplier<Collector<T, ?, R>> collectorSupplier) {
        this();

        this.groupId = groupId;
        this.requestNum = requestNum;
        this.requesterThreadId = requesterThreadId;
        this.sharedEnumClassName = storageName;
        this.variableName = variableName;
        this.indices = indices;
        this.collectorSupplier = collectorSupplier;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(groupId);
        out.writeInt(requestNum);
        out.writeInt(requesterThreadId);
        out.writeString(sharedEnumClassName);
        out.writeString(variableName);
        out.writeIntArray(indices);
        out.writeObject(collectorSupplier);
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
            collectorSupplier = (SerializableSupplier<Collector<T, ?, R>>) in.readObject();
        } catch (ClassNotFoundException e) {
            throw new PcjRuntimeException(e);
        }

        NodeData nodeData = InternalPCJ.getNodeData();
        InternalCommonGroup commonGroup = nodeData.getCommonGroupById(groupId);

        CollectStates states = commonGroup.getCollectStates();
        CollectStates.State<T, R> state = states.getOrCreate(requestNum, requesterThreadId, commonGroup);

        state.downProcessNode(commonGroup, sharedEnumClassName, variableName, indices, collectorSupplier);
    }
}