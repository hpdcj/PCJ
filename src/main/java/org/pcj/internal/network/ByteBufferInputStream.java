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
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingDeque;
import org.pcj.PcjRuntimeException;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class ByteBufferInputStream extends InputStream {

    private static final int HEADER_SIZE = Integer.BYTES;
    private static final int LAST_CHUNK_BIT = (1 << (Integer.SIZE - 1));
    private static final int LENGTH_MASK = ~LAST_CHUNK_BIT;
    private final ByteBuffer header;
    private final BlockingDeque<ByteBufferPool.PooledByteBuffer> deque;
    private int remainingLength;
    private boolean receivingLastChunk;
    private volatile boolean closed;
    private ByteBufferPool.PooledByteBuffer currentPooledByteBuffer;

    public ByteBufferInputStream(BlockingDeque<ByteBufferPool.PooledByteBuffer> deque) {
        this.deque = deque;
        this.header = ByteBuffer.allocate(HEADER_SIZE);

        this.remainingLength = 0;
        this.receivingLastChunk = false;
        this.closed = false;
    }

    private ByteBuffer getCurrentByteBuffer() {
        if (currentPooledByteBuffer == null || !currentPooledByteBuffer.getByteBuffer().hasRemaining()) {
            if (currentPooledByteBuffer != null) {
                currentPooledByteBuffer.returnToPool();
            }
            try {
                currentPooledByteBuffer = deque.take();
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

        while (remainingLength == 0) {
            if (receivingLastChunk) {
                return -1;
            }

            readChunkLength();
        }

        ByteBuffer byteBuffer = getCurrentByteBuffer();
        int b = byteBuffer.get() & 0xFF;
        --remainingLength;

        return b;
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
            while (remainingLength == 0) {
                if (receivingLastChunk) {
                    if (bytesRead == 0) {
                        return -1;
                    } else {
                        return bytesRead;
                    }
                }

                readChunkLength();
            }

            ByteBuffer byteBuffer = getCurrentByteBuffer();

            int len = Math.min(Math.min(byteBuffer.remaining(), length - bytesRead), remainingLength);
            byteBuffer.get(b, offset, len);

            bytesRead += len;
            offset += len;
            remainingLength -= len;

            if (bytesRead == length) {
                return bytesRead;
            }
        }

    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        skipCurrentChunk();
        while (!receivingLastChunk) {
            readChunkLength();
            skipCurrentChunk();
        }

        if (!currentPooledByteBuffer.getByteBuffer().hasRemaining()) {
            currentPooledByteBuffer.returnToPool();
            currentPooledByteBuffer = null;
        }

        if (currentPooledByteBuffer != null) {
            deque.offerFirst(currentPooledByteBuffer);
        }

        closed = true;
    }

    public boolean isClosed() {
        return closed;
    }

    private void readChunkLength() {
        if (remainingLength > 0) {
            throw new IllegalStateException("Bytes left in chunk: " + remainingLength);
        }
        if (receivingLastChunk) {
            throw new IllegalStateException("Already last chunk");
        }


        ByteBuffer byteBuffer = getCurrentByteBuffer();

        while (header.hasRemaining() && byteBuffer.hasRemaining()) {
            header.put(byteBuffer.get());
        }

        if (!header.hasRemaining()) {
            int lengthWithMarker = header.getInt(0);
            remainingLength = lengthWithMarker & LENGTH_MASK;

            if (lengthWithMarker != remainingLength) {
                receivingLastChunk = true;
            }

            header.clear();
        }
    }

    private void skipCurrentChunk() {
        while (remainingLength > 0) {
            ByteBuffer byteBuffer = getCurrentByteBuffer();
            int skip = Math.min(byteBuffer.remaining(), remainingLength);
            byteBuffer.position(byteBuffer.position() + skip);
            remainingLength -= skip;
        }
    }
}
