/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import org.pcj.internal.network.MessageOutputStream;
import org.pcj.internal.network.MessageInputStream;

/**
 * @see MessageTypes#LOG
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageLog extends Message {

    private int groupId;
    private int groupNodeId;
    private String logText;

    public MessageLog() {
        super(MessageTypes.LOG);
    }

    @Override
    void writeObjects(MessageOutputStream bbos) {
        bbos.writeInt(groupId);
        bbos.writeInt(groupNodeId);
        bbos.writeString(logText);
    }

    @Override
    void readObjects(MessageInputStream bbis) {
        groupId = bbis.readInt();
        groupNodeId = bbis.readInt();
        logText = bbis.readString();
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

    public int getGroupNodeId() {
        return groupNodeId;
    }

    public void setGroupNodeId(int groupNodeId) {
        this.groupNodeId = groupNodeId;
    }

    public String getLogText() {
        return logText;
    }

    public void setLogText(String logText) {
        this.logText = logText;
    }
}
