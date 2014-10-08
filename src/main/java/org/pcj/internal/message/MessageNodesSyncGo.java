/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import org.pcj.internal.network.MessageOutputStream;
import org.pcj.internal.network.MessageInputStream;

/**
 * @see MessageTypes#NODES_SYNC_GO
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageNodesSyncGo extends Message {

    private int[] nodesGlobalIds;

    public MessageNodesSyncGo() {
        super(MessageTypes.NODES_SYNC_GO);
    }

    @Override
    void writeObjects(MessageOutputStream bbos) {
        bbos.writeIntArray(nodesGlobalIds);
    }

    @Override
    void readObjects(MessageInputStream bbis) {
        nodesGlobalIds = bbis.readIntArray();
    }
    
    @Override
    public String paramsToString() {
        return "";
    }

    public int[] getNodesGlobalIds() {
        return nodesGlobalIds;
    }

    public void setNodesGlobalIds(int[] nodesGlobalIds) {
        this.nodesGlobalIds = nodesGlobalIds;
    }
}
