/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import org.pcj.internal.network.MessageOutputStream;
import org.pcj.internal.network.MessageInputStream;

/**
 * @see MessageTypes#HELLO_BONJOUR
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageHelloBonjour extends Message {

    private int physicalId;
    private int[] nodeIds;

    public MessageHelloBonjour() {
        super(MessageTypes.HELLO_BONJOUR);
    }

    @Override
    void writeObjects(MessageOutputStream bbos) {
        bbos.writeInt(physicalId);
        bbos.writeIntArray(nodeIds);
    }

    @Override
    void readObjects(MessageInputStream bbis) {
        physicalId = bbis.readInt();
        nodeIds = bbis.readIntArray();
    }

    @Override
    public String paramsToString() {
        return "";
    }

    public int getPhysicalId() {
        return physicalId;
    }

    public void setPhysicalId(int physicalId) {
        this.physicalId = physicalId;
    }

    public int[] getNodeIds() {
        return nodeIds;
    }

    public void setNodeIds(int[] nodeIds) {
        this.nodeIds = nodeIds;
    }
}
