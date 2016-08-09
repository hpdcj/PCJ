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
import java.nio.channels.SocketChannel;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.futures.GroupJoinQuery;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class MessageGroupJoinResponse extends Message {

    private int requestNum;
    private int groupId;
    private int globalThreadId;
    private int groupThreadId;

    public MessageGroupJoinResponse() {
        super(MessageType.GROUP_JOIN_RESPONSE);
    }

    public MessageGroupJoinResponse(int requestNum, int groupId, int globalThreadId, int groupThreadId) {
        this();

        this.requestNum = requestNum;
        this.groupId = groupId;
        this.globalThreadId = globalThreadId;
        this.groupThreadId = groupThreadId;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(requestNum);
        out.writeInt(groupId);
        out.writeInt(globalThreadId);
        out.writeInt(groupThreadId);
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        requestNum = in.readInt();
        groupId = in.readInt();
        globalThreadId = in.readInt();
        groupThreadId = in.readInt();

        GroupJoinQuery groupJoinQuery = InternalPCJ.getNodeData().removeGroupJoinQuery(requestNum);
        groupJoinQuery.setGroupThreadId(groupThreadId);

        groupJoinQuery.getWaitObject().signalAll();
    }
}
