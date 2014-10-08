/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import org.pcj.internal.network.MessageOutputStream;
import org.pcj.internal.network.MessageInputStream;

/**
 * @see MessageTypes#FINISH_COMPLETED
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageFinishCompleted extends Message {

    public MessageFinishCompleted() {
        super(MessageTypes.FINISH_COMPLETED);
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
