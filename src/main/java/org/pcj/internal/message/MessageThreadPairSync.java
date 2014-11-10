/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import org.pcj.internal.network.MessageOutputStream;
import org.pcj.internal.network.MessageInputStream;

/**
 * @see MessageTypes#THREAD_PAIR_SYNC
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageThreadPairSync extends Message {

    private int senderGlobalNodeId;
    private int receiverGlobalNodeId;

    public MessageThreadPairSync() {
        super(MessageTypes.THREAD_PAIR_SYNC);
    }

    @Override
    void writeObjects(MessageOutputStream bbos) {
        bbos.writeInt(senderGlobalNodeId);
        bbos.writeInt(receiverGlobalNodeId);
    }

    @Override
    void readObjects(MessageInputStream bbis) {
        senderGlobalNodeId = bbis.readInt();
        receiverGlobalNodeId = bbis.readInt();
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
}
