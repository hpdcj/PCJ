/*
 * This file is the part of the PCJ Library
 */
package org.pcj;

import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.pcj.internal.InternalGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.futures.GetVariable;
import org.pcj.internal.message.MessageGroupBarrierWaiting;
import org.pcj.internal.message.MessageType;
import org.pcj.internal.message.MessageValueGetRequest;

/**
 * External class that represents group for grouped communication.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class Group extends InternalGroup {

    private final int myThreadId;
    private final AtomicInteger barrierRoundCounter;
    private final AtomicInteger getVariableCounter;
    private final ConcurrentMap<Integer, GetVariable> getVariableMap;

    public Group(int threadId, InternalGroup internalGroup) {
        super(internalGroup);
        this.myThreadId = threadId;
        barrierRoundCounter = new AtomicInteger(0);
        getVariableCounter = new AtomicInteger(0);
        getVariableMap = new ConcurrentHashMap<>();
    }

    @Override
    public int myId() {
        return myThreadId;
    }

    @Override
    public PcjFuture<Void> asyncBarrier() {
        return super.barrier(myThreadId, barrierRoundCounter.incrementAndGet());
    }

    /**
     * Fully asynchronous get operation - receives variable value from other
     * thread. If threadId is current thread, data is cloned.
     *
     * @param threadId other node group thread id
     * @param variable name of variable
     * @param indices  indices of variable array (not needed)
     *
     * @return FutureObject that will contain received object
     */
    @Override
    public <T> PcjFuture<T> asyncGet(int threadId, String variable, int... indices) {
        int requestNum = getVariableCounter.incrementAndGet();
        GetVariable<T> getVariable = new GetVariable<>();
        getVariableMap.put(requestNum, getVariable);

        int globalThreadId = super.getGlobalThreadId(threadId);
        int physicalId = InternalPCJ.getNodeData().getPhysicalIdByThreadId().get(globalThreadId);
        SocketChannel socket = InternalPCJ.getNodeData()
                .getSocketChannelByPhysicalId().get(physicalId);

        MessageValueGetRequest message = new MessageValueGetRequest(
                requestNum, super.getGroupId(), myThreadId, threadId, variable, indices);

        InternalPCJ.getNetworker().send(socket, message);

        return getVariable;
    }

    public ConcurrentMap<Integer, GetVariable> getGetVariableMap() {
        return getVariableMap;
    }

//    public <T> T get(int nodeId, String variable, int... indices) {
//        PcjFuture<T> futureObject = getFutureObject(nodeId, variable, indices);
//
//        return futureObject.get();
//    }
//
//    /**
//     * Puts variable value to other node Storage space. If nodeId is current
//     * node, data is cloned.
//     *
//     * @param nodeId   other node group node id
//     * @param variable name of variable
//     * @param newValue value to put
//     * @param indices  indices of variable array (not needed)
//     */
//    @Override
//    public PcjFuture<Void> put(int nodeId, String variable, Object newValue, int... indices) {
//        return super.put(nodeId, variable, newValue, indices);
//    }
//
//    /**
//     * Broadcasts new variable value to all nodes in group and stores it in
//     * Storage space
//     *
//     * @param variable name of variable
//     * @param newValue value to put
//     */
//    @Override
//    public PcjFuture<Void> broadcast(String variable, Object newValue) {
//        return super.broadcast(variable, newValue);
//    }
}
