/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.SocketChannel;
import org.pcj.internal.Bitmask;
import org.pcj.internal.InternalGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * ....
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageGroupBarrierWaiting extends Message {

    private int groupId;
    private int physicalId;
    private int round;

    public MessageGroupBarrierWaiting() {
        super(MessageType.GROUP_BARRIER_WAITING);
    }

    public MessageGroupBarrierWaiting(int groupId, int physicalId, int round) {
        this();

        this.groupId = groupId;
        this.physicalId = physicalId;
        this.round = round;
    }

    @Override
    public void writeObjects(MessageDataOutputStream out) throws IOException {
        out.writeInt(groupId);
        out.writeInt(physicalId);
        out.writeInt(round);
    }

    @Override
    public void readObjects(MessageDataInputStream in) throws IOException {
        groupId = in.readInt();
        physicalId = in.readInt();
        round = in.readInt();
    }

    @Override
    public String paramsToString() {
        return String.format("groupId:%d,physicalId:%d,round:%d", groupId, physicalId, round);
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        readObjects(in);

        InternalGroup group = InternalPCJ.getNodeData().getGroupById(groupId);
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
