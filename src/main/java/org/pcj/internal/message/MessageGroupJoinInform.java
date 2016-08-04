/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj.internal.message;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.futures.GroupJoinState;
import static org.pcj.internal.message.Message.LOGGER;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 *
 * @author faramir
 */
public class MessageGroupJoinInform extends Message {

    private int requestNum;
    private int groupId;
    private int globalThreadId;
    private Map<Integer, Integer> threadsMapping;

    public MessageGroupJoinInform() {
        super(MessageType.GROUP_JOIN_INFORM);
    }

    public MessageGroupJoinInform(int requestNum, int groupId, int globalThreadId,
            Map<Integer, Integer> threadsMapping) {
        this();

        this.requestNum = requestNum;
        this.groupId = groupId;
        this.globalThreadId = globalThreadId;
        this.threadsMapping = threadsMapping;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(requestNum);
        out.writeInt(groupId);
        out.writeInt(globalThreadId);
        out.writeObject(threadsMapping);
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        requestNum = in.readInt();
        groupId = in.readInt();
        globalThreadId = in.readInt();

        try {
            Object obj = in.readObject();
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<Integer, Integer> map = (Map<Integer, Integer>) obj;
                threadsMapping = map;
            }
        } catch (ClassNotFoundException ex) {
            LOGGER.log(Level.SEVERE, "Unable to read threadsMapping", ex);
            throw new RuntimeException(ex);
        }

        NodeData nodeData = InternalPCJ.getNodeData();
        InternalCommonGroup commonGroup = nodeData.getGroupById(groupId);

        List<Integer> keys = new ArrayList<>(threadsMapping.keySet());
        keys.sort(Integer::compare);
        for (int groupThreadId : keys) {
            int globalId = threadsMapping.get(groupThreadId);

            commonGroup.addThread(globalId, groupThreadId);
        }

        GroupJoinState groupJoinState = commonGroup.getGroupJoinState(requestNum, globalThreadId, commonGroup.getChildrenNodes());

        commonGroup.getChildrenNodes().stream()
                .map(nodeData.getSocketChannelByPhysicalId()::get)
                .forEach(socket -> InternalPCJ.getNetworker().send(socket, this));

        groupJoinState.processPhysical(nodeData.getPhysicalId());
    }
}
