/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.join;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.MessageType;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class GroupJoinInformMessage extends Message {

    private int requestNum;
    private int groupId;
    private int globalThreadId;
    private Map<Integer, Integer> threadsMapping;

    public GroupJoinInformMessage() {
        super(MessageType.GROUP_JOIN_INFORM);
    }

    public GroupJoinInformMessage(int requestNum, int groupId, int globalThreadId, Map<Integer, Integer> threadsMapping) {
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

    @SuppressWarnings("unchecked")
    @Override
    public void onReceive(SocketChannel sender, MessageDataInputStream in) throws IOException {
        requestNum = in.readInt();
        groupId = in.readInt();
        globalThreadId = in.readInt();

        try {
            threadsMapping = (Map<Integer, Integer>) in.readObject();
        } catch (Exception ex) {
            throw new RuntimeException("Unable to read threadsMapping", ex);
        }

        NodeData nodeData = InternalPCJ.getNodeData();
        InternalCommonGroup commonGroup = nodeData.getCommonGroupById(groupId);

        commonGroup.updateThreadsMap(threadsMapping);

        int senderPhysicalId = nodeData.getPhysicalIdBySocketChannel(sender);
        commonGroup.getCommunicationTree().setParentNode(senderPhysicalId);

        List<Integer> childrenNodes = new ArrayList<>(commonGroup.getCommunicationTree().getChildrenNodes());
        // generate ChildrenNodes from current threadsMapping
//        Set<Integer> physicalIdsSet = new LinkedHashSet<>();
//        physicalIdsSet.add(commonGroup.getCommunicationTree().getMasterNode());
//        threadsMapping.keySet().stream()
//                .sorted()
//                .map(threadsMapping::get)
//                .map(nodeData::getPhysicalId)
//                .forEach(physicalIdsSet::add);
//        List<Integer> physicalIds = new ArrayList<>(physicalIdsSet);
//
        int currentPhysicalId = nodeData.getPhysicalId();
//        int currentIndex = physicalIds.indexOf(currentPhysicalId);
//
//        List<Integer> childrenNodes = new ArrayList<>();
//        if (currentIndex * 2 + 1 < physicalIds.size()) {
//            childrenNodes.add(physicalIds.get(currentIndex * 2 + 1));
//        }
//        if (currentIndex * 2 + 2 < physicalIds.size()) {
//            childrenNodes.add(physicalIds.get(currentIndex * 2 + 2));
//        }

        GroupJoinStates states = commonGroup.getGroupJoinStates();
        GroupJoinStates.State state = states.create(requestNum, globalThreadId, childrenNodes.size());

        childrenNodes.stream()
                .map(nodeData.getSocketChannelByPhysicalId()::get)
                .forEach(socket -> InternalPCJ.getNetworker().send(socket, this));

        state.processNode(currentPhysicalId, commonGroup);
    }
}
