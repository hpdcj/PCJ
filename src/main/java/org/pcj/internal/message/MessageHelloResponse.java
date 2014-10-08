/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import org.pcj.internal.network.MessageOutputStream;
import org.pcj.internal.network.MessageInputStream;

/**
 * @see MessageTypes#HELLO_RESPONSE
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageHelloResponse extends Message {

    private int physicalId;
    private int parentPhysicalId;
    private String hostname;

    public MessageHelloResponse() {
        super(MessageTypes.HELLO_RESPONSE);
    }

    @Override
    void writeObjects(MessageOutputStream bbos) {
        bbos.writeInt(physicalId);
        bbos.writeInt(parentPhysicalId);
        bbos.writeString(hostname);
    }

    @Override
    void readObjects(MessageInputStream bbis) {
        physicalId = bbis.readInt();
        parentPhysicalId = bbis.readInt();
        hostname = bbis.readString();
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

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
}
