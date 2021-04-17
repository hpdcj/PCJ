/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.splitgroup;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.MessageType;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * ....
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public final class SplitGroupQueryMessage extends Message {

    private int groupId;
    private int round;
    private int splitCount;

    public SplitGroupQueryMessage() {
        super(MessageType.SPLIT_GROUP_QUERY);
    }

    public SplitGroupQueryMessage(int groupId, int round, int splitCount) {
        this();

        this.groupId = groupId;
        this.round = round;
        this.splitCount = splitCount;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(groupId);
        out.writeInt(round);
        out.writeInt(splitCount);
    }

    @Override
    public void onReceive(SocketChannel sender, MessageDataInputStream in) throws IOException {
        groupId = in.readInt();
        round = in.readInt();
        splitCount = in.readInt();

        NodeData nodeData = InternalPCJ.getNodeData();
        NodeData.Node0Data node0Data = nodeData.getNode0Data();

        int[] groupIds = new int[splitCount];
        for (int i = 0; i < splitCount; i++) {
            groupIds[i] = node0Data.reserveGroupId();
        }

        Message message = new SplitGroupAnswerMessage(groupId, round, groupIds);
        InternalPCJ.getNetworker().send(sender, message);
    }
}
