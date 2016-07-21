/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Objects;
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
    public void readObjects(MessageDataInputStream in) throws IOException, ClassNotFoundException {
        requestNum = in.readInt();
        groupId = in.readInt();
        requesterThreadId = in.readInt();
        threadId = in.readInt();
        storageName = in.readString();
        name = in.readString();
        indices = in.readIntArray();
        newValue = in.readObject();
    }

    @Override
    public void writeObjects(MessageDataOutputStream out) throws IOException {
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
    public String paramsToString() {
        return String.format("requestNum:%d,"
                + "groupId:%d,"
                + "requesterThreadId:%d,"
                + "threadId:%d,"
                + "storageName:%s,"
                + "name:%s,"
                + "indices:%s,"
                + "newValue:%s",
                requestNum, groupId, requesterThreadId, threadId, storageName, name,
                Arrays.toString(indices), Objects.toString(newValue));
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        try {
            readObjects(in);
        } catch (ClassNotFoundException ex) {
            // TODO: wyjatek przerzucic do wywolujacego
            throw new PcjRuntimeException(ex);
        }

        NodeData nodeData = InternalPCJ.getNodeData();
        int globalThreadId = nodeData.getGroupById(groupId).getGlobalThreadId(threadId);
        PcjThread pcjThread = nodeData.getPcjThreads().get(globalThreadId);
        InternalStorage storage = (InternalStorage) pcjThread.getThreadData().getStorage();
        storage.put0(storageName, name, newValue, indices);

        MessageValuePutResponse messageValuePutResponse = new MessageValuePutResponse(
                requestNum, groupId, requesterThreadId);
        InternalPCJ.getNetworker().send(sender, messageValuePutResponse);
    }
}
