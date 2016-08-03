/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.InternalStorage;
import org.pcj.internal.NodeData;
import org.pcj.internal.PcjThread;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * ....
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageValueGetRequest extends Message {

    private int groupId;
    private int requestNum;
    private int requesterThreadId;
    private int threadId;
    private String storageName;
    private String name;
    private int[] indices;

    public MessageValueGetRequest() {
        super(MessageType.VALUE_GET_REQUEST);
    }

    public MessageValueGetRequest(int groupId, int requestNum, int requesterThreadId, int threadId, String storageName, String name, int[] indices) {
        this();

        this.groupId = groupId;
        this.requestNum = requestNum;
        this.requesterThreadId = requesterThreadId;
        this.threadId = threadId;
        this.storageName = storageName;
        this.name = name;
        this.indices = indices;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(groupId);
        out.writeInt(requestNum);
        out.writeInt(requesterThreadId);
        out.writeInt(threadId);
        out.writeString(storageName);
        out.writeString(name);
        out.writeIntArray(indices);
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        groupId = in.readInt();
        requestNum = in.readInt();
        requesterThreadId = in.readInt();
        threadId = in.readInt();
        storageName = in.readString();
        name = in.readString();
        indices = in.readIntArray();

        NodeData nodeData = InternalPCJ.getNodeData();
        int globalThreadId = nodeData.getGroupById(groupId).getGlobalThreadId(threadId);
        PcjThread pcjThread = nodeData.getPcjThreads().get(globalThreadId);
        InternalStorage storage = (InternalStorage) pcjThread.getThreadData().getStorage();

        MessageValueGetResponse messageValueGetResponse;
        try {
            Object variableValue = storage.get0(storageName, name, indices);
            messageValueGetResponse = new MessageValueGetResponse(
                    groupId, requestNum, requesterThreadId, variableValue);
        } catch (Exception ex) {
            messageValueGetResponse = new MessageValueGetResponse(
                    groupId, requestNum, requesterThreadId, null);
            messageValueGetResponse.setException(ex);
        }

        InternalPCJ.getNetworker().send(sender, messageValueGetResponse);
    }
}
