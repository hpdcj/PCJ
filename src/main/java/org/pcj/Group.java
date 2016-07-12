/*
 * This file is the part of the PCJ Library
 */
package org.pcj;

import org.pcj.internal.InternalGroup;

/**
 * External class that represents group for grouped communication.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class Group extends InternalGroup {

    final private int threadId;

    public Group(int threadId, InternalGroup internalGroup) {
        super(internalGroup);
        this.threadId = threadId;
    }

    @Override

    protected int getGroupId() {
        return super.getGroupId();
    }

    @Override
    public String getGroupName() {
        return super.getGroupName();
    }

    @Override
    public int threadCount() {
        return super.threadCount();
    }

    @Override
    public int myId() {
        return threadId;
    }

    /**
     * Synchronize all nodes in group
     */
    @Override
    public void barrier() {
        super.barrier(threadId);
    }
//
//    /**
//     * Synchronize with potentially subset of nodes in group
//     *
//     * @param nodes nodes group node id
//     */
//    @Override
//    public void barrier(int nodes) {
//        super.barrier(myNodeId, nodes);
//    }
//
//    /**
//     * Fully asynchronous get operation - receives variable value from other
//     * node. If nodeId is current node, data is cloned.
//     *
//     * @param nodeId   other node group node id
//     * @param variable name of variable
//     * @param indexes  indexes of variable array (not needed)
//     *
//     * @return FutureObject that will contain received object
//     */
//    public <T> PcjFuture<T> getFutureObject(int nodeId, String variable, int... indexes) {
//        return (PcjFuture<T>) super.getFutureObject(myNodeId, nodeId, variable, indexes);
//    }
//
//    public <T> T get(int nodeId, String variable, int... indexes) {
//        PcjFuture<T> futureObject = getFutureObject(nodeId, variable, indexes);
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
//     * @param indexes  indexes of variable array (not needed)
//     */
//    @Override
//    public PcjFuture<Void> put(int nodeId, String variable, Object newValue, int... indexes) {
//        return super.put(nodeId, variable, newValue, indexes);
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
