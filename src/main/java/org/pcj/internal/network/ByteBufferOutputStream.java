/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.network;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import org.pcj.internal.InternalPCJ;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class ByteBufferOutputStream extends OutputStream {

    private static final ByteBufferPool BYTE_BUFFER_POOL = new ByteBufferPool(
            InternalPCJ.getConfiguration().BUFFER_POOL_SIZE,
            InternalPCJ.getConfiguration().BUFFER_CHUNK_SIZE);
    private static final int HEADER_SIZE = Integer.BYTES;
    private static final int LAST_CHUNK_BIT = (1 << (Integer.SIZE - 1));
    private final BlockingDeque<ByteBufferPool.PooledByteBuffer> queue;
    private ByteBufferPool.PooledByteBuffer currentPooledByteBuffer;
    private boolean closed;

    public ByteBufferOutputStream() {
        this(new LinkedBlockingDeque<>());
    }

    public ByteBufferOutputStream(BlockingDeque<ByteBufferPool.PooledByteBuffer> queue) {
        this.queue = queue;

        this.currentPooledByteBuffer = null;
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

    @Override
    public void close() {
        offerCurrentByteBuffer(true);
        closed = true;
    }

    public boolean isClosed() {
        return closed;
    }

    public BlockingDeque<ByteBufferPool.PooledByteBuffer> getDeque() {
        return queue;
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
        currentByteBuffer.flip();

        int length = (currentByteBuffer.limit() - HEADER_SIZE);
        if (lastChunk) {
            length = (length | LAST_CHUNK_BIT);
        }
        currentByteBuffer.putInt(0, length);

        queue.offer(currentPooledByteBuffer);
    }
}
