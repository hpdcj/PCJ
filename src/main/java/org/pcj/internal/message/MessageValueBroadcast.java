/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import org.pcj.internal.network.MessageOutputStream;
import org.pcj.internal.network.MessageInputStream;

/**
 * @see MessageTypes#VALUE_BROADCAST
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageValueBroadcast extends Message {

    private int groupId;
    private String variableName;
    private byte[] variableValue;

    public MessageValueBroadcast() {
        super(MessageTypes.VALUE_BROADCAST);
    }

    @Override
    void writeObjects(MessageOutputStream bbos) {
        bbos.writeInt(groupId);
        bbos.writeString(variableName);
        bbos.writeByteArray(variableValue);
    }

    @Override
    void readObjects(MessageInputStream bbis) {
        groupId = bbis.readInt();
        variableName = bbis.readString();
        variableValue = bbis.readByteArray();
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

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public byte[] getVariableValue() {
        return variableValue;
    }

    public void setVariableValue(byte[] variableValue) {
        this.variableValue = variableValue;
    }
}
