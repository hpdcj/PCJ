/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.barrier;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.MessageType;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public final class GroupBarrierWaitingMessage extends Message {

    private int groupId;
    private int round;

    public GroupBarrierWaitingMessage() {
        super(MessageType.GROUP_BARRIER_WAITING);
    }

    public GroupBarrierWaitingMessage(int groupId, int round) {
        this();

        this.groupId = groupId;
        this.round = round;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(groupId);
        out.writeInt(round);
    }

    @Override
    public void onReceive(SocketChannel sender, MessageDataInputStream in) throws IOException {
        groupId = in.readInt();
        round = in.readInt();

        InternalCommonGroup commonGroup = InternalPCJ.getNodeData().getCommonGroupById(groupId);
        System.out.println(InternalPCJ.getNodeData().getCurrentNodePhysicalId()+" groupId:"+groupId+" "+commonGroup);

        BarrierStates states = commonGroup.getBarrierStates();
        BarrierStates.State state = states.getOrCreate(round, commonGroup);
        state.processPhysical(commonGroup);
    }
}
