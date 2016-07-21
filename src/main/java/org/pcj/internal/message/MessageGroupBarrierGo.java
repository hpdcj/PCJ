/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.List;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.futures.LocalBarrier;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * ....
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageGroupBarrierGo extends Message {
    
    private int groupId;
    private int round;
    
    public MessageGroupBarrierGo() {
        super(MessageType.GROUP_BARRIER_GO);
    }
    
    public MessageGroupBarrierGo(int groupId, int round) {
        this();
        
        this.groupId = groupId;
        this.round = round;
    }
    
    @Override
    public void writeObjects(MessageDataOutputStream out) throws IOException {
        out.writeInt(groupId);
        out.writeInt(round);
    }
    
    @Override
    public void readObjects(MessageDataInputStream in) throws IOException {
        groupId = in.readInt();
        round = in.readInt();
    }
    
    @Override
    public String paramsToString() {
        return String.format("groupId:%d,round:%d", groupId, round);
    }
    
    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        readObjects(in);
        
        NodeData nodeData = InternalPCJ.getNodeData();
        
        InternalCommonGroup group = nodeData.getGroupById(groupId);
        List<Integer> children = group.getChildrenNodes();
        
        children.stream().map(nodeData.getSocketChannelByPhysicalId()::get)
                .forEach(socket -> InternalPCJ.getNetworker().send(socket, this));

        LocalBarrier barrier = group.getLocalBarrierMap().remove(round);
        barrier.signalAll();
    }
}
