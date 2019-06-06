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
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.pcj.internal.Configuration;
import org.pcj.internal.message.Message;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class RemoteMessageOutputBytes {

    private static final ByteBufferPool BYTE_BUFFER_POOL = new ByteBufferPool(Configuration.BUFFER_POOL_SIZE, Configuration.BUFFER_CHUNK_SIZE);
    private static final ByteBufferPool.PooledByteBuffer[] EMPTY_BYTE_BUFFER_ARRAY = new ByteBufferPool.PooledByteBuffer[0];
    private final MessageOutputStream messageOutputStream;

    public RemoteMessageOutputBytes() {
        messageOutputStream = new MessageOutputStream();
    }

    public void writeMessage(Message message) throws IOException {
        try (MessageDataOutputStream messageDataOutputStream = new MessageDataOutputStream(messageOutputStream)) {
            messageDataOutputStream.writeByte(message.getType().getId());
            message.write(messageDataOutputStream);
        }
    }

    ByteBufferArray getByteBufferArray() {
        return new ByteBufferArray(messageOutputStream);
    }

    private static class MessageOutputStream extends OutputStream {

        private static final int HEADER_SIZE = Integer.BYTES;
        private static final int LAST_CHUNK_BIT = (1 << (Integer.SIZE - 1));
        private final Queue<ByteBufferPool.PooledByteBuffer> queue;
        private ByteBufferPool.PooledByteBuffer currentPooledByteBuffer;
        private boolean closed;

        public MessageOutputStream() {
            this.queue = new ConcurrentLinkedQueue<>();
            this.currentPooledByteBuffer = null;
        }

        @Override
        public void close() {
            offerCurrentByteBuffer(true);
            closed = true;
        }

        @Override
        public void write(int b) {
            ByteBuffer currentByteBuffer = getCurrentByteBuffer();
            if (!currentByteBuffer.hasRemaining()) {
                offerCurrentByteBuffer(false);
                currentByteBuffer = getNextByteBuffer();
            }

            currentByteBuffer.put((byte) b);
        }

        @Override
        public void write(byte[] b) {
            write(b, 0, b.length);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            ByteBuffer currentByteBuffer = getCurrentByteBuffer();

            int remaining = currentByteBuffer.remaining();
            while (remaining < len) {
                currentByteBuffer.put(b, off, remaining);

                offerCurrentByteBuffer(false);
                currentByteBuffer = getNextByteBuffer();

                len -= remaining;
                off += remaining;

                remaining = currentByteBuffer.remaining();
            }
            currentByteBuffer.put(b, off, len);
        }

        private ByteBuffer getCurrentByteBuffer() {
            if (currentPooledByteBuffer != null) {
                return currentPooledByteBuffer.getByteBuffer();
            }

            return getNextByteBuffer();
        }

        private ByteBuffer getNextByteBuffer() {
            currentPooledByteBuffer = BYTE_BUFFER_POOL.take();
            currentPooledByteBuffer.getByteBuffer().position(HEADER_SIZE);
            return currentPooledByteBuffer.getByteBuffer();
        }

        private void offerCurrentByteBuffer(boolean lastChunk) {
            ByteBuffer currentByteBuffer = currentPooledByteBuffer.getByteBuffer();
            int length = (currentByteBuffer.position() - HEADER_SIZE);
            if (lastChunk) {
                length = (length | LAST_CHUNK_BIT);
            }
            currentByteBuffer.putInt(0, length);
            currentByteBuffer.flip();

            queue.offer(currentPooledByteBuffer);
        }
    }

    public static class ByteBufferArray {
        private final MessageOutputStream messageOutputStream;
        private ByteBuffer[] array;
        private int offset;

        public ByteBufferArray(MessageOutputStream messageOutputStream) {
            this.messageOutputStream = messageOutputStream;

            this.array = new ByteBuffer[0];
        }

        public ByteBuffer[] getArray() {
            if (offset == array.length) {
                array = messageOutputStream.queue.stream().map(ByteBufferPool.PooledByteBuffer::getByteBuffer).toArray(ByteBuffer[]::new);
                offset = 0;
            }
            return array;
        }

        public int getOffset() {
            return offset;
        }

        public int getRemainingLength() {
            return array.length - offset;
        }

        public void revalidate() {
            while (offset < array.length && !array[offset].hasRemaining()) {
                messageOutputStream.queue.remove().returnToPool();
                ++offset;
            }
        }

        public boolean hasMoreData() {
            return offset < array.length || !messageOutputStream.closed || !messageOutputStream.queue.isEmpty();
        }
    }
}