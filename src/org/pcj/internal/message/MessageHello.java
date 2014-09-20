/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import org.pcj.internal.network.MessageOutputStream;
import org.pcj.internal.network.MessageInputStream;

/**
 * @see MessageTypes#HELLO
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageHello extends Message {

    private int port;
    private int[] nodeIds;

    public MessageHello() {
        super(MessageTypes.HELLO);
    }

    @Override
    void writeObjects(MessageOutputStream bbos) {
        bbos.writeInt(port);
        bbos.writeIntArray(nodeIds);
    }

    @Override
    void readObjects(MessageInputStream bbis) {
        port = bbis.readInt();
        nodeIds = bbis.readIntArray();
    }

    @Override
    public String paramsToString() {
        return "";
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int[] getNodeIds() {
        return nodeIds;
    }

    public void setNodeIds(int[] nodeIds) {
        this.nodeIds = nodeIds;
    }
}
