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
    private ByteBufferPool.PooledByteBuffer currentPooledByteBuffer;

    public MessageBytesInputStream() {
        this.header = ByteBuffer.allocate(HEADER_SIZE);
        prepareForNewMessage();
    }

    final public void prepareForNewMessage() {
        currentPooledByteBuffer = null;
        /* necessary new MessageInputStream as previous one still has data
        and will be processed by Worker */
        messageInputStream = new MessageInputStream();
    }

    private void allocateBuffer(int size) {
        if (size <= Configuration.BUFFER_CHUNK_SIZE) {
            currentPooledByteBuffer = BYTE_BUFFER_POOL.take();
            currentPooledByteBuffer.getByteBuffer().limit(size);
        } else {
            currentPooledByteBuffer = new ByteBufferPool.HeapPooledByteBuffer(size);
        }
    }

    public void offerNextBytes(ByteBuffer sourceByteBuffer) {
        while (sourceByteBuffer.hasRemaining()) {
            if (currentPooledByteBuffer == null) {
                readBuffer(sourceByteBuffer, header);

                if (!header.hasRemaining()) {
                    int lengthWithMarker = header.getInt(0);
                    int length = lengthWithMarker & ~LAST_CHUNK_BIT;

                    header.clear();

                    if ((lengthWithMarker & LAST_CHUNK_BIT) != 0) {
                        messageInputStream.setReceivingLastChunk();
                    }

                    if (length > 0) {
                        allocateBuffer(length);
                    }
                }
            }

            /* currentPooledByteBuffer can be changed in allocateBuffer() */
            if (currentPooledByteBuffer != null) {
                ByteBuffer currentByteBuffer = currentPooledByteBuffer.getByteBuffer();
                readBuffer(sourceByteBuffer, currentByteBuffer);

                if (!currentByteBuffer.hasRemaining()) {
                    currentByteBuffer.flip();
                    messageInputStream.offerByteBuffer(currentPooledByteBuffer);

                    currentPooledByteBuffer = null;
                }
            }

            if (messageInputStream.isReceivingLastChunk()) {
                return;
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
        return messageInputStream.isReceivingLastChunk() && currentPooledByteBuffer == null;
    }

    public MessageDataInputStream getMessageDataInputStream() {
        return new MessageDataInputStream(messageInputStream);
    }

    private static class MessageInputStream extends InputStream {

        private final Queue<ByteBufferPool.PooledByteBuffer> queue;
        private boolean closed;
        private boolean receivingLastChunk;

        public MessageInputStream() {
            this.queue = new LinkedList<>();
            this.closed = false;
            this.receivingLastChunk = false;
        }

        private boolean offerByteBuffer(ByteBufferPool.PooledByteBuffer byteBuffer) {
            return queue.offer(byteBuffer);
        }

        @Override
        public void close() {
            ByteBufferPool.PooledByteBuffer bb;
            while ((bb = queue.poll()) != null) {
                bb.returnToPool();
            }

            closed = true;
        }

        public boolean isClosed() {
            return closed;
        }

        @Override
        public int read() {
            while (true) {
                ByteBufferPool.PooledByteBuffer pooledByteBuffer = queue.peek();
                if (pooledByteBuffer == null) {
                    if (closed) {
                        return -1;
                    } else {
                        throw new IllegalStateException("Stream not closed, but no more data available.");
                    }
                } else {
                    ByteBuffer byteBuffer = pooledByteBuffer.getByteBuffer();
                    if (!byteBuffer.hasRemaining()) {
                        pooledByteBuffer.returnToPool();
                        queue.poll();
                    } else {
                        return byteBuffer.get() & 0xFF;
                    }
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
                ByteBufferPool.PooledByteBuffer pooledByteBuffer = queue.peek();
                if (pooledByteBuffer == null) {
                    if (receivingLastChunk) {
                        if (bytesRead == 0) {
                            return -1;
                        } else {
                            return bytesRead;
                        }
                    } else {
                        throw new IllegalStateException("Stream not closed, but no more data available.");
                    }
                } else {
                    ByteBuffer byteBuffer = pooledByteBuffer.getByteBuffer();
                    int len = Math.min(byteBuffer.remaining(), length - bytesRead);

                    byteBuffer.get(b, offset, len);

                    bytesRead += len;
                    offset += len;

                    if (!byteBuffer.hasRemaining()) {
                        pooledByteBuffer.returnToPool();
                        queue.poll();
                    }

                    if (bytesRead == length) {
                        return length;
                    }
                }
            }
        }

        private void setReceivingLastChunk() {
            receivingLastChunk = true;
        }

        private boolean isReceivingLastChunk() {
            return receivingLastChunk;
        }
    }
}
