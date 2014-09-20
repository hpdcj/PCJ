/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import org.pcj.internal.network.MessageOutputStream;
import org.pcj.internal.network.MessageInputStream;

/**
 * @see MessageTypes#UNKNOWN
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageUnknown extends Message {

    public MessageUnknown() {
        super(MessageTypes.UNKNOWN);
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
