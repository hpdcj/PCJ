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
final public class MessageValueBroadcastInform extends Message {

    private int requestNum;
    private int groupId;
    private int requesterThreadId;
    private String storageName;
    private String name;
    private Object newValue;

    public MessageValueBroadcastInform() {
        super(MessageType.VALUE_BROADCAST_INFORM);
    }

    public MessageValueBroadcastInform(int requestNum, int groupId, int requesterThreadId, 
            String storageName, String name, Object newValue) {
        this();

        this.requestNum = requestNum;
        this.groupId = groupId;
        this.requesterThreadId = requesterThreadId;
        this.storageName = storageName;
        this.name = name;
        this.newValue = newValue;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(requestNum);
        out.writeInt(groupId);
        out.writeInt(requesterThreadId);
        out.writeString(storageName);
        out.writeString(name);
        out.writeObject(newValue);
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
//        requestNum = in.readInt();
//        groupId = in.readInt();
//        requesterThreadId = in.readInt();
//        threadId = in.readInt();
//        storageName = in.readString();
//        name = in.readString();
//        indices = in.readIntArray();
//
//        NodeData nodeData = InternalPCJ.getNodeData();
//        int globalThreadId = nodeData.getGroupById(groupId).getGlobalThreadId(threadId);
//        PcjThread pcjThread = nodeData.getPcjThreads().get(globalThreadId);
//        InternalStorage storage = (InternalStorage) pcjThread.getThreadData().getStorage();
//
//        MessageValuePutResponse messageValuePutResponse = new MessageValuePutResponse(
//                requestNum, groupId, requesterThreadId);
//        try {
//            newValue = in.readObject();
//            storage.put0(storageName, name, newValue, indices);
//        } catch (Exception ex) {
//            messageValuePutResponse.setException(ex);
//        }
//
//        InternalPCJ.getNetworker().send(sender, messageValuePutResponse);
    }
}
