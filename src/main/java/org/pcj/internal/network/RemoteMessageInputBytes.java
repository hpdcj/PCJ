/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.network;

import java.io.InputStream;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class RemoteMessageInputBytes implements MessageInputBytes {

    private final AtomicBoolean processing;
    private ByteBufferInputStream inputStream;
    private final BlockingDeque<ByteBufferPool.PooledByteBuffer> queue;

    public RemoteMessageInputBytes() {
        this.queue = new LinkedBlockingDeque<>();
        this.processing = new AtomicBoolean(false);
        this.inputStream = new ByteBufferInputStream(queue);
    }

    public void offer(ByteBufferPool.PooledByteBuffer byteBuffer) {
        queue.offer(byteBuffer);
    }

    @Override
    public InputStream getInputStream() {
        if (inputStream.isClosed()) {
            inputStream = new ByteBufferInputStream(queue);
        }
        return inputStream;
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
        return !inputStream.isClosed() || !queue.isEmpty();
    }

}
