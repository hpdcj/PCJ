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

    private final WaitObject waitObject;
    private final int groupId;
    private final int requestNum;
    private final int globalThreadId;
    private final Set<Integer> childrenSet;
    private int groupThreadId;

    public GroupJoinState(int groupId, int requestNum, int globalThreadId, List<Integer> childrenNodes) {
        waitObject = new WaitObject();

        this.groupId = groupId;
        this.requestNum = requestNum;
        this.globalThreadId = globalThreadId;

        this.childrenSet = new HashSet<>(childrenNodes);
//        this.childrenSet = new HashSet<>(childrenNodes.size() + 1, 1.0f);
        childrenSet.add(InternalPCJ.getNodeData().getPhysicalId());
//        childrenNodes.forEach(childrenSet::add);
//        System.out.println("GJS: grpId:" + groupId + " req:" + requestNum + " thrdId: " + globalThreadId + " " + childrenSet);
    }

    public WaitObject getWaitObject() {
        return waitObject;
    }

    public int getGroupThreadId() {
        return groupThreadId;
    }

    public void setGroupThreadId(int groupThreadId) {
        this.groupThreadId = groupThreadId;
    }

    public synchronized boolean processPhysical(int physicalId) {
//        System.out.println(groupId + ": " + InternalPCJ.getNodeData().getPhysicalId() + " process physical " + physicalId + " set:" + childrenSet + " glId:" + globalThreadId);
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

                message = new MessageGroupJoinResponse(requestNum, groupId, globalThreadId, groupThreadId);
//                System.out.println(groupId + ": " + InternalPCJ.getNodeData().getPhysicalId() + " sending response to " + requesterPhysicalId + " num:" + requestNum + " glId:" + globalThreadId + " grId:" + groupThreadId);
            } else {
                int parentId = commonGroup.getParentNode();
//                if (parentId == -1) {
////                    System.out.println(groupId + ": " + InternalPCJ.getNodeData().getPhysicalId() + " no parent " + " num:" + requestNum + " glId:" + globalThreadId + " grId:" + groupThreadId);
//                    return true;
//                }
                socket = nodeData.getSocketChannelByPhysicalId().get(parentId);

                message = new MessageGroupJoinConfirm(requestNum, groupId, globalThreadId,
                        nodeData.getPhysicalId());
//                System.out.println(groupId + ": " + InternalPCJ.getNodeData().getPhysicalId() + " sending confirm to " + parentId + " num:" + requestNum + " glId:" + globalThreadId + " grId:" + groupThreadId);
            }

            InternalPCJ.getNetworker().send(socket, message);
            return true;
        }
        return false;
    }
}
