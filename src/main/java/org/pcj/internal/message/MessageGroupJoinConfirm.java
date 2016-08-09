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
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.futures.GroupJoinState;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class MessageGroupJoinConfirm extends Message {

    private int requestNum;
    private int groupId;
    private int physicalId;
    private int globalThreadId;

    public MessageGroupJoinConfirm() {
        super(MessageType.GROUP_JOIN_CONFIRM);
    }

    public MessageGroupJoinConfirm(int requestNum, int groupId, int globalThreadId, int physicalId) {
        this();

        this.requestNum = requestNum;
        this.groupId = groupId;
        this.globalThreadId = globalThreadId;
        this.physicalId = physicalId;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(requestNum);
        out.writeInt(groupId);
        out.writeInt(globalThreadId);
        out.writeInt(physicalId);
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        requestNum = in.readInt();
        groupId = in.readInt();
        globalThreadId = in.readInt();
        physicalId = in.readInt();

        InternalCommonGroup commonGroup = InternalPCJ.getNodeData().getGroupById(groupId);

        GroupJoinState groupJoinState = commonGroup.getGroupJoinState(requestNum, globalThreadId, commonGroup.getChildrenNodes());

        if (groupJoinState.processPhysical(physicalId)) {
            commonGroup.removeGroupJoinState(requestNum, globalThreadId);
        }
    }

}
