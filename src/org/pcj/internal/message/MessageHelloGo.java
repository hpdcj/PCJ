/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import org.pcj.internal.network.MessageOutputStream;
import org.pcj.internal.network.MessageInputStream;

/**
 * @see MessageTypes#HELLO_GO
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageHelloGo extends Message {

    public MessageHelloGo() {
        super(MessageTypes.HELLO_GO);
    }

    @Override
    void writeObjects(MessageOutputStream bbos) {
    }

    @Override
    void readObjects(MessageInputStream bbis) {
    }

    @Override
    public String paramsToString() {
        return "";
    }
}
