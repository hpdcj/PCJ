/* 
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.network;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

/**
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class MessageBytesInputStream {

    private static final int HEADER_SIZE = Integer.BYTES;
    private static final int LAST_CHUNK_BIT = (int) (1 << (Integer.SIZE - 1));
    private final ByteBuffer header;
    private MessageInputStream messageInputStream;
    private ByteBuffer currentByteBuffer;

    public MessageBytesInputStream() {
        this.header = ByteBuffer.allocate(HEADER_SIZE);
        reset();
    }

    public void offerNextBytes(ByteBuffer byteBuffer) {
        while (byteBuffer.hasRemaining()) {
            if (currentByteBuffer == null) {
                while (header.hasRemaining() && byteBuffer.hasRemaining()) {
                    header.put(byteBuffer.get());
                }
                if (header.hasRemaining() == false) {
                    int lengthWithMarker = header.getInt(0);
                    int length = lengthWithMarker & ~LAST_CHUNK_BIT;

                    header.clear();

                    if ((lengthWithMarker & LAST_CHUNK_BIT) != 0) {
                        messageInputStream.close();
                        if (length == 0) {
                            return;
                        }
                    }

                    currentByteBuffer = ByteBuffer.allocate(length);
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

    public boolean isClosed() {
        return messageInputStream.isClosed() && currentByteBuffer == null;
    }

    public MessageDataInputStream getMessageDataInputStream() {
        return new MessageDataInputStream(messageInputStream);
    }

    final public void reset() {
        currentByteBuffer = null;
        messageInputStream = new MessageInputStream();
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
