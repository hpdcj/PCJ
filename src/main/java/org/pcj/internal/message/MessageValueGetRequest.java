/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
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

    private int requestNum;
    private int groupId;
    private int requesterThreadId;
    private int threadId;
    private String name;
    private int[] indices;

    public MessageValueGetRequest() {
        super(MessageType.VALUE_GET_REQUEST);
    }

    public MessageValueGetRequest(int requestNum, int groupId, int requesterThreadId, int threadId, String name, int[] indices) {
        this();

        this.requestNum = requestNum;
        this.groupId = groupId;
        this.requesterThreadId = requesterThreadId;
        this.threadId = threadId;
        this.name = name;
        this.indices = indices;
    }

    @Override
    public void readObjects(MessageDataInputStream in) throws IOException {
        requestNum = in.readInt();
        groupId = in.readInt();
        requesterThreadId = in.readInt();
        threadId = in.readInt();
        name = in.readString();
        indices = in.readIntArray();
    }

    @Override
    public void writeObjects(MessageDataOutputStream out) throws IOException {
        out.writeInt(requestNum);
        out.writeInt(groupId);
        out.writeInt(requesterThreadId);
        out.writeInt(threadId);
        out.writeString(name);
        out.writeIntArray(indices);
    }

    @Override
    public String paramsToString() {
        return String.format("requestNum:%d,"
                + "groupId:%d,"
                + "requesterThreadId:%d,"
                + "threadId:%d,"
                + "name:%s,"
                + "indices:%s",
                requestNum, groupId, requesterThreadId, threadId, name, Arrays.toString(indices));
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        readObjects(in);

        NodeData nodeData = InternalPCJ.getNodeData();
        int globalThreadId = nodeData.getGroupById(groupId).getGlobalThreadId(threadId);
        PcjThread pcjThread = nodeData.getPcjThreads().get(globalThreadId);
        InternalStorage storage = (InternalStorage) pcjThread.getThreadData().getStorage();
        Object variableValue = storage.get0(name, indices);

        MessageValueGetResponse messageValueGetResponse = new MessageValueGetResponse(
                requestNum, groupId, requesterThreadId, variableValue);
        InternalPCJ.getNetworker().send(sender, messageValueGetResponse);
    }
}
