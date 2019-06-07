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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.pcj.PcjRuntimeException;
import org.pcj.internal.message.Message;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class LoopbackMessageBytes implements MessageInputBytes {

    private final BlockingQueue<LoopbackMessageOutputBytes> queue;
    private final AtomicBoolean processing;
    private ByteBufferInputStream currentInputStream;

    public LoopbackMessageBytes() {
        queue = new LinkedBlockingQueue<>();
        processing = new AtomicBoolean(false);
    }

    public LoopbackMessageOutputBytes prepareForNewMessage() {
        LoopbackMessageOutputBytes loopbackMessageOutputBytes = new LoopbackMessageOutputBytes();
        queue.add(loopbackMessageOutputBytes);

        return loopbackMessageOutputBytes;
    }

    @Override
    public InputStream getInputStream() {
        if (currentInputStream == null || currentInputStream.isClosed()) {
            try {
                LoopbackMessageOutputBytes loopbackMessageOutputBytes = queue.take();
                currentInputStream = loopbackMessageOutputBytes.getByteBufferInputStream();
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

    public static class LoopbackMessageOutputBytes implements MessageOutputBytes {
        private final ByteBufferOutputStream byteBufferOutputStream;
        private final ByteBufferInputStream byteBufferInputStream;

        public LoopbackMessageOutputBytes() {
            byteBufferOutputStream = new ByteBufferOutputStream();
            byteBufferInputStream = new ByteBufferInputStream(byteBufferOutputStream.getDeque());
        }

        @Override
        public void writeMessage(Message message) throws IOException {
            try (MessageDataOutputStream messageDataOutputStream = new MessageDataOutputStream(byteBufferOutputStream)) {
                messageDataOutputStream.writeByte(message.getType().getId());
                message.write(messageDataOutputStream);
            }
        }

        public ByteBufferInputStream getByteBufferInputStream() {
            return byteBufferInputStream;
        }
    }
}
