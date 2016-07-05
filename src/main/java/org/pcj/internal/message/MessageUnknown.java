/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import java.nio.channels.SocketChannel;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * Unknown MessageType - to be logged and skipped.
 *
 * @param <i>unknown</i> <i>unknown parameters</i>
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageUnknown extends Message {

    public MessageUnknown() {
        super(MessageType.UNKNOWN);
    }

    @Override
    public void writeObjects(MessageDataOutputStream out) {
    }

    @Override
    public void readObjects(MessageDataInputStream in) {
    }

    @Override
    public String paramsToString() {
        return "";
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) {
        LOGGER.severe("Unknown message received!");
    }
}
