/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import org.pcj.internal.Bitmask;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
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
    private int round;

    public MessageGroupBarrierWaiting() {
        super(MessageType.GROUP_BARRIER_WAITING);
    }

    public MessageGroupBarrierWaiting(int groupId, int physicalId, int round) {
        this();

        this.physicalId = physicalId;
        this.groupId = groupId;
        this.round = round;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(physicalId);
        out.writeInt(groupId);
        out.writeInt(round);
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        physicalId = in.readInt();
        groupId = in.readInt();
        round = in.readInt();

        InternalCommonGroup group = InternalPCJ.getNodeData().getGroupById(groupId);
        int index = group.getPhysicalIdIndex(physicalId);

        Bitmask bitmask = group.getPhysicalBitmask(round);
        synchronized (bitmask) {
            bitmask.set(index);

            if (bitmask.isSet()) {
                bitmask.clear();

                MessageGroupBarrierGo messageGroupBarrierGo = new MessageGroupBarrierGo(groupId, round);

                int groupMasterId = group.getGroupMasterNode();
                SocketChannel groupMasterSocket = InternalPCJ.getNodeData()
                        .getSocketChannelByPhysicalId().get(groupMasterId);

                InternalPCJ.getNetworker().send(groupMasterSocket, messageGroupBarrierGo);
            }
        }
    }
}
