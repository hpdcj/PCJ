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
import org.pcj.internal.Configuration;

/**
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class MessageBytesInputStream {

    private static final ByteBufferPool BYTE_BUFFER_POOL = new ByteBufferPool(Configuration.BUFFER_POOL_SIZE, Configuration.BUFFER_CHUNK_SIZE);
    private static final int HEADER_SIZE = Integer.BYTES;
    private static final int LAST_CHUNK_BIT = (int) (1 << (Integer.SIZE - 1));
    private final ByteBuffer header;
    private MessageInputStream messageInputStream;
    private ByteBuffer currentByteBuffer;
    private boolean hasAllData;

    public MessageBytesInputStream() {
        this.header = ByteBuffer.allocate(HEADER_SIZE);
        prepareForNewMessage();
    }

    final public void prepareForNewMessage() {
        currentByteBuffer = null;
        hasAllData = false;
        /* necessary new MessageInputStream as previous one still has data
        and will be processed by Worker */
        messageInputStream = new MessageInputStream();
    }

    private void allocateBuffer(int size) {
        if (size <= Configuration.BUFFER_CHUNK_SIZE) {
            currentByteBuffer = BYTE_BUFFER_POOL.take();
            currentByteBuffer.limit(size);
        } else {
            currentByteBuffer = ByteBuffer.allocate(size);
        }
    }

    public void offerNextBytes(ByteBuffer sourceByteBuffer) {
        while (sourceByteBuffer.hasRemaining()) {
            if (currentByteBuffer == null) {
                readBuffer(sourceByteBuffer, header);

                if (header.hasRemaining() == false) {
                    int lengthWithMarker = header.getInt(0);
                    int length = lengthWithMarker & ~LAST_CHUNK_BIT;

                    header.clear();

                    if ((lengthWithMarker & LAST_CHUNK_BIT) != 0) {
                        hasAllData = true;
                        if (length == 0) {
                            return;
                        }
                    }

                    allocateBuffer(length);
                }
            } else {
                readBuffer(sourceByteBuffer, currentByteBuffer);

                if (currentByteBuffer.hasRemaining() == false) {
                    currentByteBuffer.flip();
                    messageInputStream.offerByteBuffer(currentByteBuffer);

                    currentByteBuffer = null;
                    if (hasAllData) {
                        return;
                    }
                }
            }
        }
    }

    private ByteBuffer readBuffer(ByteBuffer src, ByteBuffer dest) {
        int remaining = Math.min(dest.remaining(), src.remaining());

        ByteBuffer sliceBuffer = src.slice();

        sliceBuffer.limit(remaining);
        dest.put(sliceBuffer);
        src.position(src.position() + remaining);

        return dest;
    }

    public boolean hasAllData() {
        return hasAllData && currentByteBuffer == null;
    }

    public MessageDataInputStream getMessageDataInputStream() {
        return new MessageDataInputStream(messageInputStream);
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
            ByteBuffer bb;
            while ((bb = queue.poll()) != null) {
                BYTE_BUFFER_POOL.give(bb);
            }

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
                    BYTE_BUFFER_POOL.give(byteBuffer);
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
        public int read(byte[] b, int offset, int length) {
            if (length == 0) {
                return 0;
            }

            int bytesRead = 0;
            while (true) {
                ByteBuffer byteBuffer = queue.peek();
                if (byteBuffer == null) {
                    if (closed) {
                        if (bytesRead == 0) {
                            return -1;
                        } else {
                            return bytesRead;
                        }
                    } else {
                        throw new IllegalStateException("Stream not closed, but no more data available.");
                    }
                } else {
                    int len = Math.min(byteBuffer.remaining(), length - bytesRead);

                    byteBuffer.get(b, offset, len);

                    bytesRead += len;
                    offset += len;

                    if (byteBuffer.hasRemaining() == false) {
                        BYTE_BUFFER_POOL.give(byteBuffer);
                        queue.poll();
                    }

                    if (bytesRead == length) {
                        return length;
                    }
                }
            }
        }
    }
}
