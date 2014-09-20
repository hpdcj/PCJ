/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import org.pcj.internal.network.MessageOutputStream;
import org.pcj.internal.network.MessageInputStream;

/**
 * @see MessageTypes#SYNC_WAIT
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageSyncWait extends Message {

    private int groupId;

    public MessageSyncWait() {
        super(MessageTypes.SYNC_WAIT);
    }

    @Override
    void writeObjects(MessageOutputStream bbos) {
        bbos.writeInt(groupId);
    }

    @Override
    void readObjects(MessageInputStream bbis) {
        groupId=bbis.readInt();
    }
    
    @Override
    public String paramsToString() {
        return "";
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }
}
