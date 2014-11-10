/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import org.pcj.internal.network.MessageOutputStream;
import org.pcj.internal.network.MessageInputStream;

/**
 * @see MessageTypes#VALUE_PUT
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageValuePut extends Message {

    private int receiverGlobalNodeId;
    private String variableName;
    private int[] indexes;
    private byte[] variableValue;

    public MessageValuePut() {
        super(MessageTypes.VALUE_PUT);
    }

    @Override
    void writeObjects(MessageOutputStream bbos) {
        bbos.writeInt(receiverGlobalNodeId);
        bbos.writeString(variableName);
        bbos.writeIntArray(indexes);
        bbos.writeByteArray(variableValue);
    }

    @Override
    void readObjects(MessageInputStream bbis) {
        receiverGlobalNodeId = bbis.readInt();
        variableName = bbis.readString();
        indexes = bbis.readIntArray();
        variableValue = bbis.readByteArray();
    }
    
    @Override
    public String paramsToString() {
        return "";
    }

    public int getReceiverGlobalNodeId() {
        return receiverGlobalNodeId;
    }

    public void setReceiverGlobalNodeId(int receiverGlobalNodeId) {
        this.receiverGlobalNodeId = receiverGlobalNodeId;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public int[] getIndexes() {
        return indexes;
    }

    public void setIndexes(int[] indexes) {
        this.indexes = indexes;
    }

    public byte[] getVariableValue() {
        return variableValue;
    }

    public void setVariableValue(byte[] variableValue) {
        this.variableValue = variableValue;
    }
}
