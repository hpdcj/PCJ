/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;
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
    private static final AtomicInteger MESSAGE_COUNTER = new AtomicInteger(0);
    private MessageType type;
    // FIXME: w zasadzie messageId przydaje się tylko wówczas, gdy potrzeba inReplyTo
    // może można by messageId przesyłać wówczas, gdy trzeba inReplyTo?
    // może być problem, że jeden węzeł czeka na messageId = N,
    // a inny węzeł będzie miał wiadomość N, która rozgłaszana jest bez zmiany messageId
    // można ewentualnie zmieniać messageId przy rozgłaszaniu, ale to trudne
    private int messageId;
    private int inReplyTo;

    /**
     * Prevent from creating object
     */
    private Message() {
    }

    Message(MessageType type) {
        this.type = type;

        messageId = getNextMessageId();
    }

    private static int getNextMessageId() {
        return MESSAGE_COUNTER.incrementAndGet();
    }

    /**
     * @return the type
     */
    public MessageType getType() {
        return type;
    }

    /**
     * @return the messageId
     */
    public int getMessageId() {
        return messageId;
    }

    /**
     * @param messageId the messageId to set
     */
    protected void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    /**
     * @return the inReplyTo
     */
    public int getInReplyTo() {
        return inReplyTo;
    }

    /**
     * @param inReplyTo the inReplyTo to set
     */
    protected void setInReplyTo(int inReplyTo) {
        this.inReplyTo = inReplyTo;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Message");

        sb.append("{Type:");
        sb.append(type);

        sb.append(", messageId:");
        sb.append(messageId);

        sb.append(", inReplyTo:");
        sb.append(inReplyTo);

        sb.append(", objs:{");
        sb.append(paramsToString());
        sb.append("}");

        sb.append("}");

        return sb.toString();
    }

    public abstract void writeObjects(MessageDataOutputStream out) throws IOException;

    public abstract void readObjects(MessageDataInputStream in) throws IOException;

    public abstract String paramsToString();

    public abstract void execute(SocketChannel sender, MessageDataInputStream in);

}
