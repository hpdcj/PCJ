/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import java.util.Arrays;
import org.pcj.internal.network.MessageOutputStream;
import org.pcj.internal.network.MessageInputStream;

/**
 * @see MessageTypes#GROUP_JOIN_BONJOUR
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageGroupJoinBonjour extends Message {

    private int groupId;
    private int newNodeId;
    private int[] globalNodeIds;
    private int[] groupNodeIds;

    public MessageGroupJoinBonjour() {
        super(MessageTypes.GROUP_JOIN_BONJOUR);
    }

    @Override
    void writeObjects(MessageOutputStream bbos) {
        bbos.writeInt(groupId);
        bbos.writeInt(newNodeId);
        bbos.writeIntArray(globalNodeIds);
        bbos.writeIntArray(groupNodeIds);
    }

    @Override
    void readObjects(MessageInputStream bbis) {
        groupId = bbis.readInt();
        newNodeId = bbis.readInt();
        globalNodeIds = bbis.readIntArray();
        groupNodeIds = bbis.readIntArray();
    }

    @Override
    public String paramsToString() {
        return "groupId:" + groupId + ","
                + "newNodeId:" + newNodeId + ","
                + "globalNodeIds:" + Arrays.toString(globalNodeIds) + ","
                + "groupNodeIds:" + Arrays.toString(groupNodeIds);
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public int getNewNodeId() {
        return newNodeId;
    }

    public void setNewNodeId(int newNodeId) {
        this.newNodeId = newNodeId;
    }

    public int[] getGlobalNodeIds() {
        return globalNodeIds;
    }

    public void setGlobalNodeIds(int[] globalNodeIds) {
        this.globalNodeIds = globalNodeIds;
    }

    public int[] getGroupNodeIds() {
        return groupNodeIds;
    }

    public void setGroupNodeIds(int[] groupNodeIds) {
        this.groupNodeIds = groupNodeIds;
    }
}
