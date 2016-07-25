/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.futures.BarrierState;
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

    public MessageGroupBarrierWaiting(int groupId, int physicalId, int barrierRound) {
        this();

        this.physicalId = physicalId;
        this.groupId = groupId;
        this.barrierRound = barrierRound;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(physicalId);
        out.writeInt(groupId);
        out.writeInt(barrierRound);
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        physicalId = in.readInt();
        groupId = in.readInt();
        barrierRound = in.readInt();

        InternalCommonGroup group = InternalPCJ.getNodeData().getGroupById(groupId);

        BarrierState barrierState = group.getBarrierState(barrierRound);

        synchronized (barrierState) {
            barrierState.setPhysical(physicalId);

            if (barrierState.isLocalSet() && barrierState.isPhysicalSet()) {
                if (physicalId == group.getGroupMasterNode()) {
                    MessageGroupBarrierGo messageGroupBarrierGo = new MessageGroupBarrierGo(groupId, barrierRound);

                    int groupMasterId = group.getGroupMasterNode();
                    SocketChannel groupMasterSocket = InternalPCJ.getNodeData()
                            .getSocketChannelByPhysicalId().get(groupMasterId);

                    InternalPCJ.getNetworker().send(groupMasterSocket, messageGroupBarrierGo);
                } else {
                    int parentId = group.getParentNode();
                    SocketChannel parentSocket = InternalPCJ.getNodeData()
                            .getSocketChannelByPhysicalId().get(parentId);

                    MessageGroupBarrierWaiting message = new MessageGroupBarrierWaiting(
                            groupId, InternalPCJ.getNodeData().getPhysicalId(), barrierRound);

                    InternalPCJ.getNetworker().send(parentSocket, message);
                }
            }
        }
    }
}
