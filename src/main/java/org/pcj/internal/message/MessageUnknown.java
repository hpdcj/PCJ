/* 
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
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
    public void write(MessageDataOutputStream out) {
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        LOGGER.severe("Unknown message received!");
        if (LOGGER.isLoggable(Level.FINEST)) {
            StringBuilder sb = new StringBuilder("Unknown message bytes: [");
            int b;
            while ((b = in.read()) != -1) {
                sb.append(String.format("%02x,", b));
            }
            sb.append("]");
            LOGGER.finest(sb.toString());
        }
    }
}
