/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import org.pcj.internal.network.MessageOutputStream;
import org.pcj.internal.network.MessageInputStream;

/**
 * @see MessageTypes#VALUE_COMPARE_AND_SET_RESPONSE
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageValueCompareAndSetResponse extends Message {

    private int receiverGlobalNodeId;
    private byte[] variableValue;

    public MessageValueCompareAndSetResponse() {
        super(MessageTypes.VALUE_COMPARE_AND_SET_RESPONSE);
    }

    @Override
    void writeObjects(MessageOutputStream bbos) {
        bbos.writeInt(receiverGlobalNodeId);
        bbos.writeByteArray(variableValue);
    }

    @Override
    void readObjects(MessageInputStream bbis) {
        receiverGlobalNodeId=bbis.readInt();
        variableValue=bbis.readByteArray();
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

    public byte[] getVariableValue() {
        return variableValue;
    }

    public void setVariableValue(byte[] variableValue) {
        this.variableValue = variableValue;
    }
}
