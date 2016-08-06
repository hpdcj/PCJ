/* 
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.MessageType;

/**
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class MessageBytesInputStream {

    private static final int HEADER_SIZE = Short.BYTES;
    private final ByteBuffer header;
    private final MessageInputStream messageInputStream;
    private final MessageDataInputStream messageDataInputStream;
    private ByteBuffer currentByteBuffer;

    public MessageBytesInputStream() {
        this.header = ByteBuffer.allocateDirect(HEADER_SIZE);
        this.messageInputStream = new MessageInputStream();
        this.messageDataInputStream = new MessageDataInputStream(messageInputStream);
    }

    public void offerNextBytes(ByteBuffer byteBuffer) {
        while (byteBuffer.hasRemaining()) {
            if (currentByteBuffer == null) {
                while (header.hasRemaining() && byteBuffer.hasRemaining()) {
                    header.put(byteBuffer.get());
                }
                if (header.hasRemaining() == false) {
                    short length = header.getShort(0);

                    if ((length & 0x8000) != 0) {
                        messageInputStream.close();
                    }

                    currentByteBuffer = ByteBuffer.allocate(length & 0x7FFF);
                    header.clear();
                }
            } else {
                while (currentByteBuffer.hasRemaining() && byteBuffer.hasRemaining()) {
                    currentByteBuffer.put(byteBuffer.get());
                }

                if (currentByteBuffer.hasRemaining() == false) {
                    currentByteBuffer.flip();
                    messageInputStream.offerByteBuffer(currentByteBuffer);
                    currentByteBuffer = null;
                    if (messageInputStream.isClosed()) {
                        return;
                    }
                }
            }
        }
    }

    public Message readMessage() {
        try {
            byte messageType = messageDataInputStream.readByte();

            Message message = MessageType.valueOf(messageType).create();

            return message;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public boolean isClosed() {
        return messageInputStream.isClosed() && currentByteBuffer == null;
    }

    public MessageDataInputStream getMessageData() {
        return messageDataInputStream;
    }

    private static class MessageInputStream extends InputStream {

        private final Queue<ByteBuffer> queue;
        volatile private boolean closed;

        public MessageInputStream() {
            this.queue = new LinkedList<>();
            this.closed = false;
        }

        private boolean offerByteBuffer(ByteBuffer byteBuffer) {
            return queue.offer(byteBuffer);
        }

        @Override
        public void close() {
            closed = true;
        }

        public boolean isClosed() {
            return closed;
        }

        @Override
        public int read() {
            while (true) {
                ByteBuffer byteBuffer = queue.peek();
                if (byteBuffer == null) {
                    if (closed) {
                        return -1;
                    } else {
                        throw new IllegalStateException("Stream not closed, but no more data available.");
                    }
                } else if (byteBuffer.hasRemaining() == false) {
                    queue.poll();
                } else {
                    return byteBuffer.get() & 0xFF;
                }
            }
        }

        @Override
        public int read(byte[] b) {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte[] b, int off, int len) {
            if (len == 0) {
                return 0;
            }

            int i = 0;
            while (true) {
                ByteBuffer byteBuffer = queue.peek();
                if (byteBuffer == null) {
                    if (closed) {
                        if (i == 0) {
                            return -1;
                        } else {
                            return i;
                        }
                    } else {
                        throw new IllegalStateException("Stream not closed, but no more data available.");
                    }
                } else if (byteBuffer.hasRemaining() == false) {
                    queue.poll();
                } else {
                    while (byteBuffer.hasRemaining()) {
                        b[off + i] = byteBuffer.get();
                        if (++i == len) {
                            return len;
                        }
                    }
                }
            }
        }
    }
}
