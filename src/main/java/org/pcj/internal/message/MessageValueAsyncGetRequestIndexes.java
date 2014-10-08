/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import org.pcj.internal.network.MessageOutputStream;
import org.pcj.internal.network.MessageInputStream;

/**
 * @see MessageTypes#VALUE_ASYNC_GET_REQUEST_INDEXES
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageValueAsyncGetRequestIndexes extends Message {

    private int senderGlobalNodeId;
    private int receiverGlobalNodeId;
    private int[] indexes;
    private String variableName;

    public MessageValueAsyncGetRequestIndexes() {
        super(MessageTypes.VALUE_ASYNC_GET_REQUEST_INDEXES);
    }

    @Override
    void writeObjects(MessageOutputStream bbos) {
        bbos.writeInt(senderGlobalNodeId);
        bbos.writeInt(receiverGlobalNodeId);
        bbos.writeIntArray(indexes);
        bbos.writeString(variableName);
    }

    @Override
    void readObjects(MessageInputStream bbis) {
        senderGlobalNodeId=bbis.readInt();
        receiverGlobalNodeId=bbis.readInt();
        indexes = bbis.readIntArray();
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
}
