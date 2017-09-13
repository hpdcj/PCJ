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
import java.io.Serializable;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.PcjThread;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * ....
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageAsyncAtRequest<T> extends Message {

    private int requestNum;
    private int groupId;
    private int requesterThreadId;
    private int threadId;
    private Callable<T> callable;

    public MessageAsyncAtRequest() {
        super(MessageType.ASYNC_AT_REQUEST);
    }

    public <I extends Callable & Serializable> MessageAsyncAtRequest(int groupId, int requestNum, int requesterThreadId, int threadId, I callable) {
        this();

        this.groupId = groupId;
        this.requestNum = requestNum;
        this.requesterThreadId = requesterThreadId;
        this.threadId = threadId;
        this.callable = callable;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(groupId);
        out.writeInt(requestNum);
        out.writeInt(requesterThreadId);
        out.writeInt(threadId);
        out.writeObject(callable);
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        groupId = in.readInt();
        requestNum = in.readInt();
        requesterThreadId = in.readInt();
        threadId = in.readInt();

        try {
            callable = (Callable<T>) in.readObject();
        } catch (Exception ex) {
            MessageAsyncAtResponse messageAsyncAtResponse = new MessageAsyncAtResponse(
                    groupId, requestNum, requesterThreadId, null);
            messageAsyncAtResponse.setException(ex);

            InternalPCJ.getNetworker().send(sender, messageAsyncAtResponse);
            return;
        }

        NodeData nodeData = InternalPCJ.getNodeData();
        int globalThreadId = nodeData.getGroupById(groupId).getGlobalThreadId(threadId);
        PcjThread pcjThread = nodeData.getPcjThread(globalThreadId);
        pcjThread.execute(() -> {
            System.err.println("executing");
            MessageAsyncAtResponse messageAsyncAtResponse;
            try {
                Object returnedValue = callable.call();
                messageAsyncAtResponse = new MessageAsyncAtResponse(
                        groupId, requestNum, requesterThreadId, returnedValue);
            } catch (Exception ex) {
                messageAsyncAtResponse = new MessageAsyncAtResponse(
                        groupId, requestNum, requesterThreadId, null);
                messageAsyncAtResponse.setException(ex);
            }
            InternalPCJ.getNetworker().send(sender, messageAsyncAtResponse);
        });
    }
}
