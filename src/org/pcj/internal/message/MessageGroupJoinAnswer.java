/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import org.pcj.internal.network.MessageOutputStream;
import org.pcj.internal.network.MessageInputStream;

/**
 * @see MessageTypes#GROUP_JOIN_ANSWER
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageGroupJoinAnswer extends Message {

    private String groupName;
    private int groupId;
    private int masterPhysicalId;

    public MessageGroupJoinAnswer() {
        super(MessageTypes.GROUP_JOIN_ANSWER);
    }

    @Override
    void writeObjects(MessageOutputStream bbos) {
        bbos.writeString(groupName);
        bbos.writeInt(groupId);
        bbos.writeInt(masterPhysicalId);
    }

    @Override
    void readObjects(MessageInputStream bbis) {
        groupName = bbis.readString();
        groupId = bbis.readInt();
        masterPhysicalId = bbis.readInt();
    }

    @Override
    public String paramsToString() {
        return "groupName:" + groupName + ","
                + "groupId:" + groupId + ","
                + "masterPhysicalId:" + masterPhysicalId;
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

    public int getMasterPhysicalId() {
        return masterPhysicalId;
    }

    public void setMasterPhysicalId(int masterPhysicalId) {
        this.masterPhysicalId = masterPhysicalId;
    }
}
