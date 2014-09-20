/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import org.pcj.internal.network.MessageOutputStream;
import org.pcj.internal.network.MessageInputStream;

/**
 * @see MessageTypes#GROUP_JOIN_REQUEST
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageGroupJoinRequest extends Message {

    private String groupName;
    private int groupId;
    private int globalNodeId;

    public MessageGroupJoinRequest() {
        super(MessageTypes.GROUP_JOIN_REQUEST);
    }

    @Override
    void writeObjects(MessageOutputStream bbos) {
        bbos.writeString(groupName);
        bbos.writeInt(groupId);
        bbos.writeInt(globalNodeId);
    }

    @Override
    void readObjects(MessageInputStream bbis) {
        groupName = bbis.readString();
        groupId = bbis.readInt();
        globalNodeId = bbis.readInt();
    }

    @Override
    public String paramsToString() {
        return "groupName:" + groupName + ","
                + "groupId:" + groupId + ","
                + "globalNodeId:" + globalNodeId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
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

    public void setGlobaNodelId(int globalNodeId) {
        this.globalNodeId = globalNodeId;
    }
}
