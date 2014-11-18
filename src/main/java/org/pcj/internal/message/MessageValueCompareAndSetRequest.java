/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import org.pcj.internal.network.MessageOutputStream;
import org.pcj.internal.network.MessageInputStream;

/**
 * @see MessageTypes#VALUE_COMPARE_AND_SET_REQUEST
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageValueCompareAndSetRequest extends Message {

    private int senderGlobalNodeId;
    private int receiverGlobalNodeId;
    private String variableName;
    private int[] indexes;
    private byte[] expectedValue;
    private byte[] newValue;

    public MessageValueCompareAndSetRequest() {
        super(MessageTypes.VALUE_COMPARE_AND_SET_REQUEST);
    }

    @Override
    void writeObjects(MessageOutputStream bbos) {
        bbos.writeInt(senderGlobalNodeId);
        bbos.writeInt(receiverGlobalNodeId);
        bbos.writeIntArray(indexes);
        bbos.writeString(variableName);
        bbos.writeByteArray(getExpectedValue());
        bbos.writeByteArray(getNewValue());
    }

    @Override
    void readObjects(MessageInputStream bbis) {
        senderGlobalNodeId = bbis.readInt();
        receiverGlobalNodeId = bbis.readInt();
        indexes = bbis.readIntArray();
        variableName = bbis.readString();
        expectedValue = bbis.readByteArray();
        newValue = bbis.readByteArray();
    }

    @Override
    public String paramsToString() {
        return "";
    }

    public int getSenderGlobalNodeId() {
        return senderGlobalNodeId;
    }

    public void setSenderGlobalNodeId(int senderGlobalNodeId) {
        this.senderGlobalNodeId = senderGlobalNodeId;
    }

    public int getReceiverGlobalNodeId() {
        return receiverGlobalNodeId;
    }

    public void setReceiverGlobalNodeId(int receiverGlobalNodeId) {
        this.receiverGlobalNodeId = receiverGlobalNodeId;
    }

    public int[] getIndexes() {
        return indexes;
    }

    public void setIndexes(int[] indexes) {
        this.indexes = indexes;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public byte[] getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(byte[] expectedValue) {
        this.expectedValue = expectedValue;
    }

    public byte[] getNewValue() {
        return newValue;
    }

    public void setNewValue(byte[] newValue) {
        this.newValue = newValue;
    }
}
