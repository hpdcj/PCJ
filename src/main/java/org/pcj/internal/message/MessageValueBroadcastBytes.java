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
import java.io.ObjectInputStream;
import java.nio.channels.SocketChannel;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.InternalStorages;
import org.pcj.internal.Networker;
import org.pcj.internal.NodeData;
import org.pcj.internal.PcjThread;
import org.pcj.internal.futures.BroadcastStates;
import org.pcj.internal.network.CloneInputStream;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * ....
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageValueBroadcastBytes extends Message {

    private int groupId;
    private int requestNum;
    private int requesterThreadId;
    private String sharedEnumClassName;
    private String name;
    private int[] indices;
    private CloneInputStream clonedData;

    public MessageValueBroadcastBytes() {
        super(MessageType.VALUE_BROADCAST_BYTES);
    }

    public MessageValueBroadcastBytes(int groupId, int requestNum, int requesterThreadId, String storageName, String name, int[] indices, CloneInputStream clonedData) {
        this();

        this.groupId = groupId;
        this.requestNum = requestNum;
        this.requesterThreadId = requesterThreadId;
        this.sharedEnumClassName = storageName;
        this.name = name;
        this.indices = indices;

        this.clonedData = clonedData;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(groupId);
        out.writeInt(requestNum);
        out.writeInt(requesterThreadId);
        out.writeString(sharedEnumClassName);
        out.writeString(name);
        out.writeIntArray(indices);

        clonedData.writeInto(out);
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        groupId = in.readInt();
        requestNum = in.readInt();
        requesterThreadId = in.readInt();

        sharedEnumClassName = in.readString();
        name = in.readString();
        indices = in.readIntArray();

        clonedData = CloneInputStream.readFrom(in);

        NodeData nodeData = InternalPCJ.getNodeData();
        Networker networker = InternalPCJ.getNetworker();

        MessageValueBroadcastBytes messageValueBroadcastBytes
                = new MessageValueBroadcastBytes(groupId, requestNum, requesterThreadId, sharedEnumClassName, name, indices, clonedData);

        InternalCommonGroup group = nodeData.getGroupById(groupId);
        group.getChildrenNodes()
                .stream()
                .map(nodeData.getSocketChannelByPhysicalId()::get)
                .forEach(socket -> networker.send(socket, messageValueBroadcastBytes));

        BroadcastStates states = group.getBroadcastStates();
        BroadcastStates.State state = states.getOrCreate(requestNum, requesterThreadId, group.getChildrenNodes().size());
        int[] threadsId = group.getLocalThreadsId();
        for (int threadId : threadsId) {
            try {
                int globalThreadId = group.getGlobalThreadId(threadId);
                PcjThread pcjThread = nodeData.getPcjThread(globalThreadId);
                InternalStorages storage = pcjThread.getThreadData().getStorages();

                clonedData.reset();
                Object newValue = new ObjectInputStream(clonedData).readObject();

                storage.put(newValue, sharedEnumClassName, name, indices);
            } catch (Exception ex) {
                state.addException(ex);
            }
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
