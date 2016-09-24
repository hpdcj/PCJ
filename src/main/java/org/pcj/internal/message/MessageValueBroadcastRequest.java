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
import java.io.ObjectInputStream;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.InternalStorages;
import org.pcj.internal.NodeData;
import org.pcj.internal.PcjThread;
import org.pcj.internal.network.CloneInputStream;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * ....
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageValueBroadcastRequest extends Message {

    private int groupId;
    private int requestNum;
    private int requesterThreadId;
    private String sharedEnumClassName;
    private String name;
    private Object newValue;

    public MessageValueBroadcastRequest() {
        super(MessageType.VALUE_BROADCAST_REQUEST);
    }

    public MessageValueBroadcastRequest(int groupId, int requestNum, int requesterThreadId, String storageName, String name, Object newValue) {
        this();

        this.groupId = groupId;
        this.requestNum = requestNum;
        this.requesterThreadId = requesterThreadId;
        this.sharedEnumClassName = storageName;
        this.name = name;
        this.newValue = newValue;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(groupId);
        out.writeInt(requestNum);
        out.writeInt(requesterThreadId);
        out.writeString(sharedEnumClassName);
        out.writeString(name);
        out.writeObject(newValue);
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        groupId = in.readInt();
        requestNum = in.readInt();
        requesterThreadId = in.readInt();

        sharedEnumClassName = in.readString();
        name = in.readString();

        CloneInputStream clonedData = CloneInputStream.clone(in);

        NodeData nodeData = InternalPCJ.getNodeData();
        InternalCommonGroup group = nodeData.getGroupById(groupId);

        MessageValueBroadcastBytes message
                = new MessageValueBroadcastBytes(groupId, requestNum, requesterThreadId, sharedEnumClassName, name, clonedData);

        group.getChildrenNodes().stream().map(nodeData.getSocketChannelByPhysicalId()::get)
                .forEach(socket -> InternalPCJ.getNetworker().send(socket, message));

        Queue<Exception> exceptionsQueue = new LinkedList<>();

        int[] threadsId = group.getLocalThreadsId();
        for (int i = 0; i < threadsId.length; ++i) {
            int threadId = threadsId[i];
            try {
                int globalThreadId = group.getGlobalThreadId(threadId);
                PcjThread pcjThread = nodeData.getPcjThread(globalThreadId);
                InternalStorages storage = (InternalStorages) pcjThread.getThreadData().getStorages();

                clonedData.reset();
                newValue = new ObjectInputStream(clonedData).readObject();

                storage.put(sharedEnumClassName, name, newValue);
            } catch (Exception ex) {
                exceptionsQueue.add(ex);
            }
        }

        int globalThreadId = group.getGlobalThreadId(requesterThreadId);
        int requesterPhysicalId = nodeData.getPhysicalId(globalThreadId);
        SocketChannel socket = InternalPCJ.getNodeData().getSocketChannelByPhysicalId().get(requesterPhysicalId);

        MessageValueBroadcastInform messageInform = new MessageValueBroadcastInform(groupId, requestNum, requesterThreadId,
                nodeData.getPhysicalId(), exceptionsQueue);
        InternalPCJ.getNetworker().send(socket, messageInform);
    }

}
