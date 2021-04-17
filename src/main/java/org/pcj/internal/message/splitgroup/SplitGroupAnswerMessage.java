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
import org.pcj.PcjRuntimeException;
import org.pcj.internal.InternalCommonGroup;
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
public final class SplitGroupAnswerMessage extends Message {

    private int groupId;
    private int round;
    private int[] groupIds;

    public SplitGroupAnswerMessage() {
        super(MessageType.SPLIT_GROUP_ANSWER);
    }

    public SplitGroupAnswerMessage(int groupId, int round, int[] groupIds) {
        this();

        this.groupId = groupId;
        this.round = round;
        this.groupIds = groupIds;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(groupId);
        out.writeInt(round);
        out.writeObject(groupIds);
    }

    @Override
    public void onReceive(SocketChannel sender, MessageDataInputStream in) throws IOException {
        groupId = in.readInt();
        round = in.readInt();
        groupIds = in.readIntArray();

        InternalCommonGroup commonGroup = InternalPCJ.getNodeData().getCommonGroupById(groupId);

        SplitGroupStates states = commonGroup.getSplitGroupStates();
        SplitGroupStates.State state = states.getOrCreate(round, commonGroup);
        state.upProcessNode(commonGroup, groupIds);
    }
}
