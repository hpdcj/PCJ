/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.network;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * @author faramir
 */
public class ByteBufferPool {

    private final Queue<PooledByteBuffer> pool;
    private final int chunkSize;

    public ByteBufferPool(int size, int chunkSize) {
        this.pool = new ArrayBlockingQueue<>(size);
        this.chunkSize = chunkSize;
        for (int i = 0; i < size; ++i) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(chunkSize);
            pool.offer(new DirectPooledByteBuffer(buffer));
        }
    }

    public PooledByteBuffer take() {
        PooledByteBuffer pooledByteBuffer = pool.poll();
        if (pooledByteBuffer != null) {
            return pooledByteBuffer;
        }
        return new HeapPooledByteBuffer(chunkSize);
    }

    public static abstract class PooledByteBuffer {
        final ByteBuffer byteBuffer;

        private PooledByteBuffer(ByteBuffer byteBuffer) {
            this.byteBuffer = byteBuffer;
        }

        public ByteBuffer getByteBuffer() {
            return byteBuffer;
        }

        public abstract void returnToPool();
    }

    final private class DirectPooledByteBuffer extends PooledByteBuffer {

        private DirectPooledByteBuffer(ByteBuffer buffer) {
            super(buffer);
        }

        public void returnToPool() {
            byteBuffer.clear();
            pool.offer(this);
        }
    }

    public final static class HeapPooledByteBuffer extends PooledByteBuffer {
        public HeapPooledByteBuffer(int capacity) {
            super(ByteBuffer.allocate(capacity));
        }

        public void returnToPool() {
            // heap byte buffer, do not return to pool
        }
    }
}
