/* 
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.futures;

import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.MessageGroupJoinConfirm;
import org.pcj.internal.message.MessageGroupJoinResponse;

/**
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class GroupJoinState {

    private final int groupId;
    private final int requestNum;
    private final int globalThreadId;
    private final Set<Integer> childrenSet;

    public GroupJoinState(int groupId, int requestNum, int globalThreadId, List<Integer> childrenNodes) {
        this.groupId = groupId;
        this.requestNum = requestNum;
        this.globalThreadId = globalThreadId;

        this.childrenSet = new HashSet<>(childrenNodes.size() + 1, 1.0f);
        childrenSet.add(InternalPCJ.getNodeData().getPhysicalId());
        childrenSet.addAll(childrenNodes);
    }

    public synchronized boolean processPhysical(int physicalId) {
        if (childrenSet.contains(physicalId) == false) {
            return false;
        }

        childrenSet.remove(physicalId);

        if (childrenSet.isEmpty()) {
            NodeData nodeData = InternalPCJ.getNodeData();
            InternalCommonGroup commonGroup = nodeData.getGroupById(groupId);

            SocketChannel socket;
            Message message;

            if (nodeData.getPhysicalId() == commonGroup.getGroupMasterNode()) {
                int requesterPhysicalId = nodeData.getPhysicalId(globalThreadId);
                socket = nodeData.getSocketChannelByPhysicalId().get(requesterPhysicalId);

                message = new MessageGroupJoinResponse(requestNum, groupId, globalThreadId,
                        commonGroup.getGroupThreadId(globalThreadId));
            } else {
                int parentId = commonGroup.getParentNode();

                socket = nodeData.getSocketChannelByPhysicalId().get(parentId);

                message = new MessageGroupJoinConfirm(requestNum, groupId, globalThreadId,
                        nodeData.getPhysicalId());
            }

            InternalPCJ.getNetworker().send(socket, message);
            return true;
        }
        return false;
    }
}
