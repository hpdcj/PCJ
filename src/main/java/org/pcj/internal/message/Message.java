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
import java.io.Serializable;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * Abstract class for storing messages, containing base
 * methods for any type of different messages.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
abstract public class Message implements Serializable {

    protected static final Logger LOGGER = Logger.getLogger(Message.class.getName());
    private static final long serialVersionUID = 1L;
    private MessageType type;

    /**
     * Prevent from creating object
     */
    private Message() {
    }

    Message(MessageType type) {
        this.type = type;
    }

    /**
     * @return the type
     */
    public MessageType getType() {
        return type;
    }

    @Override
    public String toString() {
        return type.toString();
    }

    public abstract void write(MessageDataOutputStream out) throws IOException;

    public abstract void execute(SocketChannel sender, MessageDataInputStream in) throws IOException;
}
