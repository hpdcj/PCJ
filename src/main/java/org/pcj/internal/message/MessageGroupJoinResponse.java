/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import org.pcj.internal.network.MessageOutputStream;
import org.pcj.internal.network.MessageInputStream;

/**
 * @see MessageTypes#GROUP_JOIN_RESPONSE
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageGroupJoinResponse extends Message {

    private int groupId;
    private int globalNodeId;
    private int groupNodeId;
    private int parentPhysicalId;

    public MessageGroupJoinResponse() {
        super(MessageTypes.GROUP_JOIN_RESPONSE);
    }

    @Override
    void writeObjects(MessageOutputStream bbos) {
        bbos.writeInt(groupId);
        bbos.writeInt(globalNodeId);
        bbos.writeInt(groupNodeId);
        bbos.writeInt(parentPhysicalId);
    }

    @Override
    void readObjects(MessageInputStream bbis) {
        groupId = bbis.readInt();
        globalNodeId = bbis.readInt();
        groupNodeId = bbis.readInt();
        parentPhysicalId = bbis.readInt();
    }

    @Override
    public String paramsToString() {
        return "groupId:" + groupId + ","
                + "globalNodeId:" + globalNodeId + ","
                + "groupNodeId:" + groupNodeId + ","
                + "parentPhysicalId:" + parentPhysicalId;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public int getGlobalNodeId() {
        return globalNodeId;
    }

    public void setGlobalNodeId(int globalNodeId) {
        this.globalNodeId = globalNodeId;
    }

    public int getGroupNodeId() {
        return groupNodeId;
    }

    public void setGroupNodeId(int groupNodeId) {
        this.groupNodeId = groupNodeId;
    }

    public int getParentPhysicalId() {
        return parentPhysicalId;
    }

    public void setParentPhysicalId(int parentPhysicalId) {
        this.parentPhysicalId = parentPhysicalId;
    }
}
