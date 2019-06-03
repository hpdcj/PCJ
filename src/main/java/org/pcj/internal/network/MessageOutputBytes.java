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
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import org.pcj.internal.Configuration;
import org.pcj.internal.message.Message;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class MessageOutputBytes {

    private static final ByteBufferPool BYTE_BUFFER_POOL = new ByteBufferPool(Configuration.BUFFER_POOL_SIZE, Configuration.BUFFER_CHUNK_SIZE);
    private static final ByteBufferPool.PooledByteBuffer[] EMPTY_BYTE_BUFFER_ARRAY = new ByteBufferPool.PooledByteBuffer[0];
    private final Message message;
    private final MessageOutputStream messageOutputStream;
    private ByteBufferArray byteBufferArray;

    public MessageOutputBytes(Message message) {
        this.message = message;

        this.messageOutputStream = new MessageOutputStream();
    }

    public void writeMessage() throws IOException {
        MessageDataOutputStream messageDataOutputStream = new MessageDataOutputStream(this.messageOutputStream);
        messageDataOutputStream.writeByte(message.getType().getId());
        message.write(messageDataOutputStream);
    }

    public void close() {
        messageOutputStream.close();
        byteBufferArray = new ByteBufferArray(messageOutputStream.queue.toArray(EMPTY_BYTE_BUFFER_ARRAY));

    }

    public ByteBufferArray getByteBufferArray() {
        return byteBufferArray;
    }

    private static class MessageOutputStream extends OutputStream {

        private static final int HEADER_SIZE = Integer.BYTES;

        private static final int LAST_CHUNK_BIT = (1 << (Integer.SIZE - 1));
        private final Queue<ByteBufferPool.PooledByteBuffer> queue;
        private boolean closed;
        private ByteBufferPool.PooledByteBuffer currentPooledByteBuffer;

        public MessageOutputStream() {
            this.queue = new ConcurrentLinkedQueue<>();
            this.currentPooledByteBuffer = null;
        }

        @Override
        public void close() {
            closed = true;
            offerCurrentByteBuffer();
        }

        @Override
        public void write(int b) {
            ByteBuffer currentByteBuffer = getCurrentByteBuffer();
            if (!currentByteBuffer.hasRemaining()) {
                offerCurrentByteBuffer();
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

                offerCurrentByteBuffer();
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

        private void offerCurrentByteBuffer() {
            ByteBuffer currentByteBuffer = currentPooledByteBuffer.getByteBuffer();
            int length = (currentByteBuffer.position() - HEADER_SIZE);
            if (closed) {
                length = (length | LAST_CHUNK_BIT);
            }
            currentByteBuffer.putInt(0, length);
            currentByteBuffer.flip();

            queue.offer(currentPooledByteBuffer);
        }
    }

    public static class ByteBufferArray {

        private final ByteBufferPool.PooledByteBuffer[] pooledArray;
        private final ByteBuffer[] array;
        private int offset;

        private ByteBufferArray(ByteBufferPool.PooledByteBuffer[] pooledArray) {
            this.pooledArray = pooledArray;
            this.array = Arrays.stream(pooledArray).map(ByteBufferPool.PooledByteBuffer::getByteBuffer).toArray(ByteBuffer[]::new);
            offset = 0;
        }

        public ByteBuffer[] getArray() {
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
                pooledArray[offset].returnToPool();
                ++offset;
            }
        }

        @Override
        public String toString() {
            return Arrays.stream(array).map(Object::toString).collect(Collectors.joining(",", "ByteBufferArray[", "]"));
        }
    }
}





















