/*
 * Copyright (c) 2011-2021, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.scatter;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Map;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.Networker;
import org.pcj.internal.NodeData;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.MessageType;
import org.pcj.internal.network.InputStreamCloner;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public final class ScatterRequestMessage extends Message {

    private int groupId;
    private int requestNum;
    private int requesterThreadId;
    private String sharedEnumClassName;
    private String variableName;
    private int[] indices;
    private Object newValueMap;

    public ScatterRequestMessage() {
        super(MessageType.SCATTER_REQUEST);
    }

    public ScatterRequestMessage(int groupId, int requestNum, int requesterThreadId, String storageName, String variableName, int[] indices, Map<Integer,Object> newValueMap) {
        this();

        this.groupId = groupId;
        this.requestNum = requestNum;
        this.requesterThreadId = requesterThreadId;
        this.sharedEnumClassName = storageName;
        this.variableName = variableName;
        this.indices = indices;
        this.newValueMap = newValueMap;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(groupId);
        out.writeInt(requestNum);
        out.writeInt(requesterThreadId);
        out.writeString(sharedEnumClassName);
        out.writeString(variableName);
        out.writeIntArray(indices);
        out.writeObject(newValueMap);
    }

    @Override
    public void onReceive(SocketChannel sender, MessageDataInputStream in) throws IOException {
        groupId = in.readInt();
        requestNum = in.readInt();
        requesterThreadId = in.readInt();

        sharedEnumClassName = in.readString();
        variableName = in.readString();
        indices = in.readIntArray();

        InputStreamCloner inputStreamCloner = InputStreamCloner.clone(in);

        NodeData nodeData = InternalPCJ.getNodeData();
        Networker networker = InternalPCJ.getNetworker();

        try {
            newValueMap = in.readObject();
            //storage.put(newValue, sharedEnumClassName, name, indices);
        } catch (Exception ex) {
            valuePutResponseMessage.setException(ex);
        }
        BroadcastBytesMessage broadcastBytesMessage
                = new BroadcastBytesMessage(groupId, requestNum, requesterThreadId, sharedEnumClassName, variableName, indices, inputStreamCloner);

        InternalCommonGroup commonGroup = nodeData.getCommonGroupById(groupId);
        int requesterPhysicalId = nodeData.getPhysicalId(commonGroup.getGlobalThreadId(requesterThreadId));
        commonGroup.getCommunicationTree().getChildrenNodes(requesterPhysicalId)
                .stream()
                .map(nodeData::getSocketChannelByPhysicalId)
                .forEach(socket -> networker.send(socket, broadcastBytesMessage));

        ScatterStates states = commonGroup.getBroadcastStates();
        ScatterStates.State state = states.getOrCreate(requestNum, requesterThreadId, commonGroup);

        state.downProcessNode(commonGroup, inputStreamCloner, sharedEnumClassName, variableName, indices);
    }
}