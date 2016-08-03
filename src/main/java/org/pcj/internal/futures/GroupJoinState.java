/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
 * @author faramir
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

        this.childrenSet = new HashSet<>(childrenNodes.size() + 1, 1.0f);
        childrenSet.add(InternalPCJ.getNodeData().getPhysicalId());
        childrenNodes.forEach(childrenSet::add);
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

    public synchronized void processPhysical(int physicalId) {
//        System.out.println("grpId:" + groupId + " req:" + requestNum + " thrdId: " + globalThreadId + " " + childrenSet + " contains " + physicalId + "?");
        if (childrenSet.contains(physicalId) == false) {
            return;
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
            } else {
                int parentId = commonGroup.getParentNode();
                socket = nodeData.getSocketChannelByPhysicalId().get(parentId);

                message = new MessageGroupJoinConfirm(requestNum, groupId, globalThreadId,
                        nodeData.getPhysicalId());
            }

            InternalPCJ.getNetworker().send(socket, message);
        }
    }
}
