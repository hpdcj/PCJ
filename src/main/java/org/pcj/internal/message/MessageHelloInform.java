/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import org.pcj.internal.network.MessageOutputStream;
import org.pcj.internal.network.MessageInputStream;

/**
 * @see MessageTypes#HELLO_INFORM
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageHelloInform extends Message {

    private int physicalId;
    private int parentPhysicalId;
    private int[] nodeIds;
    private String host;
    private int port;

    public MessageHelloInform() {
        super(MessageTypes.HELLO_INFORM);
    }

    @Override
    void writeObjects(MessageOutputStream bbos) {
        bbos.writeInt(physicalId);
        bbos.writeInt(parentPhysicalId);
        bbos.writeIntArray(nodeIds);
        bbos.writeString(host);
        bbos.writeInt(port);
    }

    @Override
    void readObjects(MessageInputStream bbis) {
        physicalId = bbis.readInt();
        parentPhysicalId = bbis.readInt();
        nodeIds = bbis.readIntArray();
        host = bbis.readString();
        port = bbis.readInt();
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

    public int getParentPhysicalId() {
        return parentPhysicalId;
    }

    public void setParentPhysicalId(int parentPhysicalId) {
        this.parentPhysicalId = parentPhysicalId;
    }

    public int[] getNodeIds() {
        return nodeIds;
    }

    public void setNodeIds(int[] nodeIds) {
        this.nodeIds = nodeIds;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
