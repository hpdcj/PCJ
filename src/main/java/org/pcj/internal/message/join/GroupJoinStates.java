/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.join;

import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.pcj.PcjFuture;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.message.Message;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class GroupJoinStates {

    private final AtomicInteger counter;
    private final ConcurrentMap<List<Integer>, State> stateMap;
    private final ConcurrentMap<List<Integer>, Notification> notificationMap;

    public GroupJoinStates() {
        counter = new AtomicInteger(0);
        stateMap = new ConcurrentHashMap<>();
        notificationMap = new ConcurrentHashMap<>();
    }

    public Notification createNotification(int threadId) {
        int requestNum = counter.incrementAndGet();

        GroupJoinFuture future = new GroupJoinFuture();
        Notification notification = new Notification(requestNum, future);

        notificationMap.put(Arrays.asList(requestNum, threadId), notification);

        return notification;
    }

    public Notification removeNotification(int requestNum, int threadId) {
        return notificationMap.remove(Arrays.asList(requestNum, threadId));
    }

    public State get(int requestNum, int requesterThreadId) {
        return stateMap.get(Arrays.asList(requestNum, requesterThreadId));
    }

    public State create(int requestNum, int requesterThreadId, int childrenCount) {
        return stateMap.computeIfAbsent(Arrays.asList(requestNum, requesterThreadId),
                key -> new State(requestNum, requesterThreadId, childrenCount));
    }

    public State remove(int requestNum, int threadId) {
        return stateMap.remove(Arrays.asList(requestNum, threadId));
    }

    public class Notification {
        private final int requestNum;
        private final GroupJoinFuture future;

        public Notification(int requestNum, GroupJoinFuture future) {
            this.requestNum = requestNum;
            this.future = future;
        }

        public int getRequestNum() {
            return requestNum;
        }

        public PcjFuture<InternalGroup> getFuture() {
            return future;
        }

        public void signal(InternalGroup internalGroup) {
            future.signalDone(internalGroup);
        }
    }

    public class State {
        private final int requestNum;
        private final int joinerGlobalThreadId;
        private final AtomicInteger notificationCount;

        private State(int requestNum, int joinerGlobalThreadId, int childrenCount) {
            this.requestNum = requestNum;
            this.joinerGlobalThreadId = joinerGlobalThreadId;

            this.notificationCount = new AtomicInteger(childrenCount + 1);
        }

        public void processNode(InternalCommonGroup commonGroup) {
            NodeData nodeData = InternalPCJ.getNodeData();

            int leftPhysical = notificationCount.decrementAndGet();
            if (leftPhysical == 0) {
                GroupJoinStates.this.remove(requestNum, joinerGlobalThreadId);

                SocketChannel socket;
                Message message;
                int groupId = commonGroup.getGroupId();

                if (nodeData.getPhysicalId() == commonGroup.getCommunicationTree().getMasterNode()) {
                    int requesterPhysicalId = nodeData.getPhysicalId(joinerGlobalThreadId);
                    socket = nodeData.getSocketChannelByPhysicalId().get(requesterPhysicalId);

                    message = new GroupJoinResponseMessage(requestNum, groupId, joinerGlobalThreadId, commonGroup.getGroupThreadId(joinerGlobalThreadId));
                } else {
                    int parentId = commonGroup.getCommunicationTree().getParentNode();
                    socket = nodeData.getSocketChannelByPhysicalId().get(parentId);

                    message = new GroupJoinConfirmMessage(requestNum, groupId, joinerGlobalThreadId, nodeData.getPhysicalId());
                }

                InternalPCJ.getNetworker().send(socket, message);
            }
        }

        public int getRequestNum() {
            return requestNum;
        }
    }

}
