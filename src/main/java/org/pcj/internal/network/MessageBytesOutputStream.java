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
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.message.Message;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class MessageBytesOutputStream implements AutoCloseable {

    private static final ByteBufferPool BYTE_BUFFER_POOL = new ByteBufferPool(Configuration.BUFFER_POOL_SIZE, Configuration.BUFFER_CHUNK_SIZE);
    private static final ByteBufferPool.PooledByteBuffer[] EMPTY_BYTE_BUFFER_ARRAY = new ByteBufferPool.PooledByteBuffer[0];
    private final Message message;
    private final MessageOutputStream messageOutputStream;
    private final MessageDataOutputStream messageDataOutputStream;
    private ByteBufferArray byteBufferArray;

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

    public MessageBytesOutputStream(Message message) {
        this.message = message;

        this.messageOutputStream = new MessageOutputStream(Configuration.BUFFER_CHUNK_SIZE);
        this.messageDataOutputStream = new MessageDataOutputStream(this.messageOutputStream);
    }

    public void writeMessage() throws IOException {
        messageDataOutputStream.writeByte(message.getType().getId());

        message.write(messageDataOutputStream);
    }

    public Message getMessage() {
        return message;
    }

    public boolean isClosed() {
        return messageOutputStream.isClosed();
    }

    @Override
    public void close() throws IOException {
        messageOutputStream.close();
        byteBufferArray = new ByteBufferArray(messageOutputStream.queue.toArray(EMPTY_BYTE_BUFFER_ARRAY));

    }

    public ByteBufferArray getByteBufferArray() {
        return byteBufferArray;
    }

    private static class MessageOutputStream extends OutputStream {

        private static final int HEADER_SIZE = Integer.BYTES;
        private static final int LAST_CHUNK_BIT = (1 << (Integer.SIZE - 1));
        private final int chunkSize;
        private final Queue<ByteBufferPool.PooledByteBuffer> queue;
        volatile private boolean closed;
        volatile private ByteBufferPool.PooledByteBuffer currentPooledByteBuffer;

        public MessageOutputStream(int chunkSize) {
            this.chunkSize = chunkSize;
            this.queue = new ConcurrentLinkedQueue<>();

            allocateBuffer(chunkSize);
        }

        private void allocateBuffer(int size) {
            if (size <= chunkSize) {
                currentPooledByteBuffer = BYTE_BUFFER_POOL.take();
            } else {
                currentPooledByteBuffer = new ByteBufferPool.HeapPooledByteBuffer(size);
            }
            currentPooledByteBuffer.getByteBuffer().position(HEADER_SIZE);
        }

        private void flush(int size) {
            ByteBuffer currentByteBuffer = currentPooledByteBuffer.getByteBuffer();
            int length = (currentByteBuffer.position() - HEADER_SIZE);
            if (closed) {
                length = (length | LAST_CHUNK_BIT);
            }
            currentByteBuffer.putInt(0, length);
            currentByteBuffer.flip();
//            System.err.println(InternalPCJ.getNetworker().getCurrentHostName() + " offer "+currentPooledByteBuffer+" "+currentPooledByteBuffer.getByteBuffer()+" flush:"+size);
            queue.offer(currentPooledByteBuffer);

            if (size > 0) {
                allocateBuffer(size);
            }
        }

        @Override
        public void flush() {
            flush(chunkSize);
        }

        @Override
        public void close() {
            closed = true;
            flush(0);
            currentPooledByteBuffer = null;
        }

        public boolean isClosed() {
            return currentPooledByteBuffer == null && queue.isEmpty();
        }

        @Override
        public void write(int b) {
            ByteBuffer currentByteBuffer = currentPooledByteBuffer.getByteBuffer();
            if (!currentByteBuffer.hasRemaining()) {
                flush();
            }
            currentByteBuffer.put((byte) b);
        }

        @Override
        public void write(byte[] b) {
            write(b, 0, b.length);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            ByteBuffer currentByteBuffer = currentPooledByteBuffer.getByteBuffer();
            int remaining = currentByteBuffer.remaining();
            while (remaining < len) {
                currentByteBuffer.put(b, off, remaining);
                len -= remaining;
                off += remaining;
                flush(Math.max(chunkSize, len));

                currentByteBuffer = currentPooledByteBuffer.getByteBuffer();
                remaining = currentByteBuffer.remaining();
            }
            currentByteBuffer.put(b, off, len);
        }
    }
}
