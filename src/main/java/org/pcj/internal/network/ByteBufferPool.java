/* 
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.network;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 *
 * @author faramir
 */
public class ByteBufferPool {

    private final ConcurrentLinkedDeque<ByteBuffer> pool;
    private final int byteBufferChunkSize;
    
    public ByteBufferPool(int maxSize, int byteBufferChunkSize) {
        this.pool = new ConcurrentLinkedDeque<>();
        this.byteBufferChunkSize = byteBufferChunkSize;
        for (int i = 0; i < maxSize; ++i) {
            pool.offer(ByteBuffer.allocateDirect(byteBufferChunkSize));
        }
    }

    public ByteBuffer take() {
        ByteBuffer byteBuffer = pool.poll();
        if (byteBuffer == null) {
            byteBuffer = ByteBuffer.allocate(byteBufferChunkSize);
        }
        return byteBuffer;
    }

    public void give(ByteBuffer byteBuffer) {
        if (byteBuffer.isDirect()) {
            byteBuffer.clear();
            pool.offer(byteBuffer);
        }
    }
}
