package org.pcj.internal.message.barrier;

import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.pcj.PcjFuture;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.message.Message;

public class BarrierStates {
    private final ConcurrentMap<Integer, AtomicInteger> counterMap;
    private final ConcurrentMap<Integer, State> stateMap;

    public BarrierStates() {
        counterMap = new ConcurrentHashMap<>();
        stateMap = new ConcurrentHashMap<>();
    }

    public int getNextRound(int threadId) {
        AtomicInteger roundCounter = counterMap.computeIfAbsent(threadId, key -> new AtomicInteger(0));
        return roundCounter.incrementAndGet();
    }

    public State getOrCreate(int round, InternalCommonGroup commonGroup) {
        return stateMap.computeIfAbsent(round,
                _round -> new State(_round, commonGroup.getLocalThreadsId().length, commonGroup.getChildrenNodes().size(), new BarrierFuture()));
    }

    public State remove(int round) {
        return stateMap.remove(round);
    }

    public static class State {
        private final int round;
        private final AtomicReference<NotificationCount> notificationCount;
        private final BarrierFuture future;

        private State(int round, int localCount, int physicalCount, BarrierFuture future) {
            this.round = round;
            this.future = future;

            notificationCount = new AtomicReference<>(new NotificationCount(localCount, physicalCount));
        }

        public PcjFuture<Void> getFuture() {
            return future;
        }

        public void processLocal(InternalCommonGroup group) {
            NotificationCount count = notificationCount.updateAndGet(
                    old -> new NotificationCount(old.local - 1, old.physical));

            if (count.isDone()) {
                nodeProcessed(group);
            }
        }

        public void processPhysical(InternalCommonGroup group) {
            NotificationCount count = notificationCount.updateAndGet(
                    old -> new NotificationCount(old.local, old.physical - 1));

            if (count.isDone()) {
                nodeProcessed(group);
            }
        }

        private void nodeProcessed(InternalCommonGroup group) {
            Message message;
            SocketChannel socket;
            NodeData nodeData = InternalPCJ.getNodeData();

            int physicalId = nodeData.getPhysicalId();
            if (physicalId == group.getGroupMasterNode()) {
                socket = nodeData.getSocketChannelByPhysicalId().get(physicalId);

                message = new GroupBarrierGoMessage(group.getGroupId(), round);
            } else {
                int parentId = group.getParentNode();
                socket = nodeData.getSocketChannelByPhysicalId().get(parentId);

                message = new GroupBarrierWaitingMessage(group.getGroupId(), round);
            }

            InternalPCJ.getNetworker().send(socket, message);
        }

        public void signalDone() {
            future.signalDone();
        }

        private static class NotificationCount {

            private final int local;
            private final int physical;

            public NotificationCount(int local, int physical) {
                this.local = local;
                this.physical = physical;
            }

            boolean isDone() {
                return local == 0 && physical == 0;
            }
        }
    }
}
