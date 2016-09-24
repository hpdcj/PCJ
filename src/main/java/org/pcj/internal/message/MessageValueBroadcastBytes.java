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
import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.InternalStorages;
import org.pcj.internal.NodeData;
import org.pcj.internal.PcjThread;
import org.pcj.internal.futures.BroadcastState;
import org.pcj.internal.network.CloneInputStream;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * ....
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageValueBroadcastBytes extends Message {

    private int requestNum;
    private int groupId;
    private int requesterThreadId;
    private String sharedEnumClassName;
    private String name;
    private CloneInputStream clonedData;

    public MessageValueBroadcastBytes() {
        super(MessageType.VALUE_BROADCAST_BYTES);
    }

    public MessageValueBroadcastBytes(int groupId, int requestNum, int requesterThreadId, String storageName, String name, CloneInputStream clonedData) {
        this();

        this.groupId = groupId;
        this.requestNum = requestNum;
        this.requesterThreadId = requesterThreadId;
        this.sharedEnumClassName = storageName;
        this.name = name;

        this.clonedData = clonedData;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(groupId);
        out.writeInt(requestNum);
        out.writeInt(requesterThreadId);
        out.writeString(sharedEnumClassName);
        out.writeString(name);

        clonedData.writeInto(out);
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        groupId = in.readInt();
        requestNum = in.readInt();
        requesterThreadId = in.readInt();

        sharedEnumClassName = in.readString();
        name = in.readString();

        clonedData = CloneInputStream.readFrom(in);

        NodeData nodeData = InternalPCJ.getNodeData();
        InternalCommonGroup group = nodeData.getGroupById(groupId);

        List<Integer> children = group.getChildrenNodes();

        MessageValueBroadcastBytes messageBytes
                = new MessageValueBroadcastBytes(groupId, requestNum, requesterThreadId,
                        sharedEnumClassName, name, clonedData);

        children.stream().map(nodeData.getSocketChannelByPhysicalId()::get)
                .forEach(socket -> InternalPCJ.getNetworker().send(socket, messageBytes));

        Queue<Exception> exceptionsQueue = new LinkedList<>();
        int[] threadsId = group.getLocalThreadsId();
        for (int i = 0; i < threadsId.length; ++i) {
            int threadId = threadsId[i];
            try {
                int globalThreadId = group.getGlobalThreadId(threadId);
                PcjThread pcjThread = nodeData.getPcjThread(globalThreadId);
                InternalStorages storage = (InternalStorages) pcjThread.getThreadData().getStorages();

                clonedData.reset();
                Object newValue = new ObjectInputStream(clonedData).readObject();

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
