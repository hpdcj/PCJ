/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import org.pcj.internal.network.CloneInputStreamData;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pcj.internal.InternalCommonGroup;
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
final public class MessageValueBroadcastRequest extends Message {

    private int requestNum;
    private int groupId;
    private int requesterThreadId;
    private String storageName;
    private String name;
    private Object newValue;

    public MessageValueBroadcastRequest() {
        super(MessageType.VALUE_BROADCAST_REQUEST);
    }

    public MessageValueBroadcastRequest(int requestNum, int groupId, int requesterThreadId,
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
        requestNum = in.readInt();
        groupId = in.readInt();
        requesterThreadId = in.readInt();
        storageName = in.readString();
        name = in.readString();

        CloneInputStreamData cloneInputStreamData = new CloneInputStreamData(in);
        cloneInputStreamData.cloneFully();

        NodeData nodeData = InternalPCJ.getNodeData();
        InternalCommonGroup group = nodeData.getGroupById(groupId);

        List<Integer> children = group.getChildrenNodes();

        MessageValueBroadcastBytes message
                = new MessageValueBroadcastBytes(requestNum,
                        groupId, requesterThreadId, storageName, name, cloneInputStreamData);

        children.stream().map(nodeData.getSocketChannelByPhysicalId()::get)
                .forEach(socket -> InternalPCJ.getNetworker().send(socket, message));

        int[] threadsId = group.getLocalThreadsId();
        for (int i = 0; i < threadsId.length; ++i) {
            int threadId = threadsId[i];
            try {
                int globalThreadId = group.getGlobalThreadId(threadId);
                PcjThread pcjThread = nodeData.getPcjThreads().get(globalThreadId);
                InternalStorage storage = (InternalStorage) pcjThread.getThreadData().getStorage();

                cloneInputStreamData.reset();
                newValue = new ObjectInputStream(cloneInputStreamData).readObject();

                storage.put0(storageName, name, newValue);
            } catch (ClassNotFoundException ex) {
                LOGGER.log(Level.SEVERE, "ClassCastException...", ex);
            }
        }
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
