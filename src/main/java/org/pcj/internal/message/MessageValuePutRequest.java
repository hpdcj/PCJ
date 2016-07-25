/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pcj.PcjRuntimeException;
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
final public class MessageValuePutRequest extends Message {

    private int requestNum;
    private int groupId;
    private int requesterThreadId;
    private int threadId;
    private String storageName;
    private String name;
    private int[] indices;
    private Object newValue;

    public MessageValuePutRequest() {
        super(MessageType.VALUE_PUT_REQUEST);
    }

    public MessageValuePutRequest(int requestNum, int groupId, int requesterThreadId, int threadId,
            String storageName, String name, int[] indices, Object newValue) {
        this();

        this.requestNum = requestNum;
        this.groupId = groupId;
        this.requesterThreadId = requesterThreadId;
        this.threadId = threadId;
        this.storageName = storageName;
        this.name = name;
        this.indices = indices;
        this.newValue = newValue;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(requestNum);
        out.writeInt(groupId);
        out.writeInt(requesterThreadId);
        out.writeInt(threadId);
        out.writeString(storageName);
        out.writeString(name);
        out.writeIntArray(indices);
        out.writeObject(newValue);
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        requestNum = in.readInt();
        groupId = in.readInt();
        requesterThreadId = in.readInt();
        threadId = in.readInt();
        storageName = in.readString();
        name = in.readString();
        indices = in.readIntArray();

        NodeData nodeData = InternalPCJ.getNodeData();
        int globalThreadId = nodeData.getGroupById(groupId).getGlobalThreadId(threadId);
        PcjThread pcjThread = nodeData.getPcjThreads().get(globalThreadId);
        InternalStorage storage = (InternalStorage) pcjThread.getThreadData().getStorage();

        MessageValuePutResponse messageValuePutResponse = new MessageValuePutResponse(
                requestNum, groupId, requesterThreadId);
        try {
            newValue = in.readObject();
            storage.put0(storageName, name, newValue, indices);
        } catch (Exception ex) {
            messageValuePutResponse.setException(ex);
        }

        InternalPCJ.getNetworker().send(sender, messageValuePutResponse);
    }
}
