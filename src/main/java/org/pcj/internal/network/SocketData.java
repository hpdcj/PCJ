/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.network;

import static java.lang.Math.min;
import java.nio.ByteBuffer;
import org.pcj.internal.message.Message;
import org.pcj.internal.utils.FastOutputStream;

/**
 * Class for parsing data (as {@link java.nio.ByteBuffer})
 * into Message object.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class SocketData {

    private int messageLength;
    private int remaining;
    private FastOutputStream body;
    private final ByteBuffer header = ByteBuffer.allocateDirect(4); // (Integer.SIZE / Byte.SIZE)

    /**
     * Used by Worker to transform incoming ByteBuffer into
     * Message.
     *
     * @param buffer incoming data
     * @return Message or null if full message was not read
     */
    public Message parse(ByteBuffer buffer) {
        while (buffer.hasRemaining()) {
            if (body == null) {
                int oldLimit = buffer.limit();
                buffer.limit(min(oldLimit, buffer.position() + header.remaining()));
                header.put(buffer);
                buffer.limit(oldLimit);

                if (header.hasRemaining() == false) {
                    header.flip();
                    messageLength = header.getInt();
                    header.clear();
                    body = new FastOutputStream(messageLength);
                    remaining = 0;
                } else {
                    return null;
                }
            }
            if (body != null) {
                int limit = min(remaining + buffer.remaining(), messageLength);
                for (; remaining < limit; ++remaining) {
                    body.write(buffer.get());
                }

                if (remaining == messageLength) {
                    Message msg = Message.parse(body.getFastInputStream());
                    body = null;
                    return msg;
                }
            }
        }

        return null;
    }
}
