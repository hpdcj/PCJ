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
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.pcj.internal.Configuration;
import org.pcj.internal.message.Message;

/**
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class LoopbackMessageBytesStream implements AutoCloseable {

    private static final ByteBufferPool BYTE_BUFFER_POOL = new ByteBufferPool(Configuration.BUFFER_POOL_SIZE, Configuration.BUFFER_CHUNK_SIZE);
    private final Message message;
    private final Queue<ByteBufferPool.PooledByteBuffer> queue;
    private final MessageDataOutputStream messageDataOutputStream;

    public LoopbackMessageBytesStream(Message message) {
        this(message, Configuration.BUFFER_CHUNK_SIZE);
    }

    public LoopbackMessageBytesStream(Message message, int chunkSize) {
        this.message = message;

        this.queue = new ConcurrentLinkedQueue<>();
        this.messageDataOutputStream = new MessageDataOutputStream(new LoopbackOutputStream(queue, chunkSize));
    }

    public void writeMessage() throws IOException {
        message.write(messageDataOutputStream);
    }

    @Override
    public void close() throws IOException {
        messageDataOutputStream.close();
    }

    public MessageDataInputStream getMessageDataInputStream() {
        return new MessageDataInputStream(new LoopbackInputStream(queue));
    }

    private static class LoopbackOutputStream extends OutputStream {

        private final int chunkSize;
        private final Queue<ByteBufferPool.PooledByteBuffer> queue;
        private ByteBufferPool.PooledByteBuffer currentPooledByteBuffer;

        public LoopbackOutputStream(Queue<ByteBufferPool.PooledByteBuffer> queue, int chunkSize) {
            this.chunkSize = chunkSize;
            this.queue = queue;

            allocateBuffer(chunkSize);
        }

        private void allocateBuffer(int size) {
            if (size <= chunkSize) {
                currentPooledByteBuffer = BYTE_BUFFER_POOL.take();
            } else {
                currentPooledByteBuffer = new ByteBufferPool.HeapPooledByteBuffer(size);
            }
        }

        private void flush(int size) {
            ByteBuffer currentByteBuffer = currentPooledByteBuffer.getByteBuffer();
            if (currentByteBuffer.position() <= 0) {
                return;
            }
            currentByteBuffer.flip();
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
        public void close() throws IOException {
            if (currentPooledByteBuffer.getByteBuffer().position() > 0) {
                flush(0);
            }
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
            if (remaining < len) {
                currentByteBuffer.put(b, off, remaining);
                len -= remaining;
                off += remaining;
                flush(Math.max(chunkSize, len));
                currentByteBuffer = currentPooledByteBuffer.getByteBuffer();
            }
            currentByteBuffer.put(b, off, len);
        }
    }

    private static class LoopbackInputStream extends InputStream {

        private final Queue<ByteBufferPool.PooledByteBuffer> queue;

        public LoopbackInputStream(Queue<ByteBufferPool.PooledByteBuffer> queue) {
            this.queue = queue;
        }

        @Override
        public void close() {
            ByteBufferPool.PooledByteBuffer bb;
            while ((bb = queue.poll()) != null) {
                bb.returnToPool();
            }
        }

        @Override
        public int read() {
            while (true) {
                ByteBufferPool.PooledByteBuffer pooledByteBuffer = queue.peek();
                if (pooledByteBuffer== null) {
                    return -1;
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
                    if (bytesRead == 0) {
                        return -1;
                    } else {
                        return bytesRead;
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
    }
}
