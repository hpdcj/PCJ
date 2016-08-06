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
import org.pcj.internal.futures.GroupBarrierState;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * ....
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageGroupBarrierWaiting extends Message {

    private int physicalId;
    private int groupId;
    private int barrierRound;

    public MessageGroupBarrierWaiting() {
        super(MessageType.GROUP_BARRIER_WAITING);
    }

    public MessageGroupBarrierWaiting(int groupId, int barrierRound, int physicalId) {
        this();

        this.groupId = groupId;
        this.barrierRound = barrierRound;
        this.physicalId = physicalId;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(groupId);
        out.writeInt(barrierRound);
        out.writeInt(physicalId);

    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        groupId = in.readInt();
        barrierRound = in.readInt();
        physicalId = in.readInt();

        InternalCommonGroup group = InternalPCJ.getNodeData().getGroupById(groupId);

        GroupBarrierState barrierState = group.getBarrierState(barrierRound);
        barrierState.processPhysical(physicalId);
    }
}
