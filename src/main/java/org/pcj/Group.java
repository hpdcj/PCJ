/*
 * This file is the part of the PCJ Library
 */
package org.pcj;

import org.pcj.internal.InternalGroup;

/**
 * External class that represents group for grouped
 * communication.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class Group extends org.pcj.internal.InternalGroup {

    final private int myNodeId;

    protected Group(int nodeId, InternalGroup group) {
        super(group);
        myNodeId = nodeId;
    }

    /**
     * Returns group name
     *
     * @return group name
     */
    @Override
    public String getGroupName() {
        return super.getGroupName();
    }

    /**
     * Returns number of nodes in group
     *
     * @return number of nodes in group
     */
    @Override
    public int threadCount() {
        return super.threadCount();
    }

    /**
     * Returns current thread group node id
     *
     * @return current thread group node id
     */
    @Override
    public int myId() {
        return myNodeId;
    }

    /**
     * Synchronize all nodes in group
     */
    @Override
    public void barrier() {
        super.barrier(myNodeId);
    }

    /**
     * Synchronize with potentially subset of nodes in group
     *
     * @param nodes nodes group node id
     */
    @Override
    public void barrier(int nodes) {
        super.barrier(myNodeId, nodes);
    }

    /**
     * Fully asynchronous get operation - receives variable
     * value from other node. If nodeId is current node, data
     * is cloned.
     *
     * @param nodeId other node group node id
     * @param variable name of variable
     * @param indexes indexes of variable array (not needed)
     * @return FutureObject that will contain received object
     */
    public <T> FutureObject<T> getFutureObject(int nodeId, String variable, int... indexes) {
        FutureObject<T> futureObject = new FutureObject<>();

        return (FutureObject<T>) super.getFutureObject(futureObject, myNodeId, nodeId, variable, indexes);
    }

    public <T> T get(int nodeId, String variable, int... indexes) {
        FutureObject<T> futureObject = getFutureObject(nodeId, variable, indexes);

        return futureObject.get();
    }

    /**
     * Puts variable value to other node Storage space. If
     * nodeId is current node, data is cloned.
     *
     * @param nodeId other node group node id
     * @param variable name of variable
     * @param newValue value to put
     * @param indexes indexes of variable array (not needed)
     */
    @Override
    public void put(int nodeId, String variable, Object newValue, int... indexes) {
        super.put(nodeId, variable, newValue, indexes);
    }

    /**
     * Broadcasts new variable value to all nodes in group and
     * stores it in Storage space
     *
     * @param variable name of variable
     * @param newValue value to put
     */
    @Override
    public void broadcast(String variable, Object newValue) {
        super.broadcast(variable, newValue);
    }

    /**
     * Sends message with log message
     *
     * @param text text to send
     */
    public void log(String text) {
        super.log(myNodeId, text);
    }
}
