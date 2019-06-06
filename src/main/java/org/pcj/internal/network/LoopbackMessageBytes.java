/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.pcj.PcjRuntimeException;
import org.pcj.internal.Configuration;
import org.pcj.internal.message.Message;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class LoopbackMessageBytes implements MessageInputBytes {

    private final BlockingQueue<LoopbackOutputStream> queue;
    private final AtomicBoolean processing;
    private LoopbackInputStream currentInputStream;

    public LoopbackMessageBytes() {
        queue = new LinkedBlockingQueue<>();
        processing = new AtomicBoolean(false);
    }

    public LoopbackOutputStream prepareForNewMessage() {
        LoopbackOutputStream loopbackOutputStream = new LoopbackOutputStream();
        queue.add(loopbackOutputStream);

        return loopbackOutputStream;
    }

    @Override
    public InputStream getInputStream() {
        if (currentInputStream == null || currentInputStream.isClosed()) {
            try {
                LoopbackOutputStream loopbackOutputStream = queue.take();
                currentInputStream = loopbackOutputStream.getInputStream();
            } catch (InterruptedException e) {
                throw new PcjRuntimeException(e);
            }
        }
        return currentInputStream;
    }

    @Override
    public boolean tryProcessing() {
        return processing.compareAndSet(false, true);
    }

    @Override
    public void finishedProcessing() {
        processing.set(false);
    }

    @Override
    public boolean hasMoreData() {
        return !queue.isEmpty();
    }

    public static class LoopbackOutputStream extends OutputStream implements MessageOutputBytes {

        private static final ByteBufferPool BYTE_BUFFER_POOL = new ByteBufferPool(Configuration.BUFFER_POOL_SIZE, Configuration.BUFFER_CHUNK_SIZE);
        private final BlockingQueue<ByteBufferPool.PooledByteBuffer> queue;
        private final LoopbackInputStream loopbackInputStream;
        private ByteBufferPool.PooledByteBuffer currentPooledByteBuffer;

        private LoopbackOutputStream() {
            this.queue = new LinkedBlockingQueue<>();
            this.loopbackInputStream = new LoopbackInputStream(queue);
            this.currentPooledByteBuffer = null;
        }

        @Override
        public void writeMessage(Message message) throws IOException {
            try (MessageDataOutputStream messageDataOutputStream = new MessageDataOutputStream(this)) {
                messageDataOutputStream.writeByte(message.getType().getId());
                message.write(messageDataOutputStream);
            }
        }

        @Override
        public void close() {
            offerCurrentByteBuffer();

            loopbackInputStream.setAllDataArrived();
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
            return currentPooledByteBuffer.getByteBuffer();
        }

        private void offerCurrentByteBuffer() {
            ByteBuffer currentByteBuffer = currentPooledByteBuffer.getByteBuffer();
            currentByteBuffer.flip();

            queue.offer(currentPooledByteBuffer);
        }

        private LoopbackInputStream getInputStream() {
            return loopbackInputStream;
        }
    }

    private static class LoopbackInputStream extends InputStream {
        private final BlockingQueue<ByteBufferPool.PooledByteBuffer> queue;
        private ByteBufferPool.PooledByteBuffer currentPooledByteBuffer;
        private volatile boolean closed;
        boolean allDataArrived;

        private LoopbackInputStream(BlockingQueue<ByteBufferPool.PooledByteBuffer> queue) {
            this.queue = queue;
            this.closed = false;
            this.allDataArrived = false;
        }

        private void setAllDataArrived() {
            this.allDataArrived = true;
        }

        private boolean isClosed() {
            return closed;
        }

        private ByteBuffer getCurrentByteBuffer() {
            if (currentPooledByteBuffer == null || !currentPooledByteBuffer.getByteBuffer().hasRemaining()) {
                if (currentPooledByteBuffer != null) {
                    currentPooledByteBuffer.returnToPool();
                }
                try {
                    currentPooledByteBuffer = queue.take();
                } catch (InterruptedException e) {
                    throw new PcjRuntimeException(e);
                }
            }
            return currentPooledByteBuffer.getByteBuffer();
        }

        @Override
        public int read() throws IOException {
            if (closed) {
                throw new IOException("Stream Closed");
            }

            if (isEndOfData()) {
                return -1;
            }


            ByteBuffer byteBuffer = getCurrentByteBuffer();

            return byteBuffer.get() & 0xFF;
        }

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte[] b, int offset, int length) throws IOException {
            if (closed) {
                throw new IOException("Stream Closed");
            }

            if (b == null) {
                throw new NullPointerException();
            } else if (offset < 0 || length < 0 || length > b.length - offset) {
                throw new IndexOutOfBoundsException();
            } else if (length == 0) {
                return 0;
            }

            int bytesRead = 0;
            while (true) {
                if (isEndOfData()) {
                    if (bytesRead == 0) {
                        return -1;
                    } else {
                        return bytesRead;
                    }
                }

                ByteBuffer byteBuffer = getCurrentByteBuffer();

                int len = Math.min(byteBuffer.remaining(), length - bytesRead);
                byteBuffer.get(b, offset, len);

                bytesRead += len;
                offset += len;

                if (bytesRead == length) {
                    return bytesRead;
                }
            }

        }

        private boolean isEndOfData() {
            return allDataArrived
                           && (currentPooledByteBuffer == null || !currentPooledByteBuffer.getByteBuffer().hasRemaining())
                           && queue.isEmpty();
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;

            ByteBufferPool.PooledByteBuffer bb;
            while ((bb = queue.poll()) != null) {
                bb.returnToPool();
            }

            currentPooledByteBuffer.returnToPool();
            currentPooledByteBuffer = null;
        }
    }
}
