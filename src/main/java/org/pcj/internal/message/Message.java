/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import org.pcj.internal.network.MessageOutputStream;
import org.pcj.internal.network.MessageInputStream;
import java.io.Serializable;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;
import org.pcj.internal.utils.FastInputStream;

/**
 * Abstract class for storing messages, containing base
 * methods for any type of different messages.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
abstract public class Message implements Serializable {

    private static final long serialVersionUID = 1L;
    private static AtomicInteger messageCount = new AtomicInteger(0);
    private SocketChannel socket;
    private MessageTypes type;
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

    Message(MessageTypes type) {
        messageId = getNextMessageId();
        this.type = type;
    }

    private static int getNextMessageId() {
        return messageCount.incrementAndGet();
    }

    public static Message parse(FastInputStream fis) {
        MessageInputStream bbis = new MessageInputStream(fis);
        byte typeId = bbis.readByte();
        int messageId = bbis.readInt();
        int inReplyTo = bbis.readInt();

        Message message = MessageTypes.valueOf(typeId).createMessage();

        message.setMessageId(messageId);
        message.setInReplyTo(inReplyTo);
        message.readObjects(bbis);

        return message;
    }

    final public void writeToOutputStream(MessageOutputStream bbos) {
        bbos.writeByte(type.getId());
        bbos.writeInt(messageId);
        bbos.writeInt(inReplyTo);

        writeObjects(bbos);
    }

    abstract void writeObjects(MessageOutputStream bbos);

    abstract void readObjects(MessageInputStream bbis);

    /**
     * @return the type
     */
    public MessageTypes getType() {
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
    private void setMessageId(int messageId) {
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
    public void setInReplyTo(int inReplyTo) {
        this.inReplyTo = inReplyTo;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Message");
        sb.append("{Type:");
        sb.append(type);
        sb.append("; messageId:");
        sb.append(messageId);
        sb.append("; inReplyTo:");
        sb.append(inReplyTo);

        sb.append("; objs:[");
        sb.append(paramsToString());
        sb.append("]");

        sb.append("}");

        return sb.toString();
    }

    public abstract String paramsToString();

    /**
     * @return the socket
     */
    public SocketChannel getSocket() {
        return socket;
    }

    /**
     * @param socket the socket to set
     */
    public void setSocket(SocketChannel socket) {
        this.socket = socket;
    }
}
