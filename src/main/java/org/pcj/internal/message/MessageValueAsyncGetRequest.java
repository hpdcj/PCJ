/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import org.pcj.internal.network.MessageOutputStream;
import org.pcj.internal.network.MessageInputStream;

/**
 * @see MessageTypes#VALUE_ASYNC_GET_REQUEST
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageValueAsyncGetRequest extends Message {

    private int senderGlobalNodeId;
    private int receiverGlobalNodeId;
    private String variableName;

    public MessageValueAsyncGetRequest() {
        super(MessageTypes.VALUE_ASYNC_GET_REQUEST);
    }

    @Override
    void writeObjects(MessageOutputStream bbos) {
        bbos.writeInt(senderGlobalNodeId);
        bbos.writeInt(receiverGlobalNodeId);
        bbos.writeString(variableName);
    }

    @Override
    void readObjects(MessageInputStream bbis) {
        senderGlobalNodeId = bbis.readInt();
        receiverGlobalNodeId = bbis.readInt();
        variableName = bbis.readString();
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

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }
}
