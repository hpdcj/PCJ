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

    private final LoopbackMessageOutputBytes loopbackMessageOutputBytes;

    private LoopbackMessageBytes() {
        loopbackMessageOutputBytes = new LoopbackMessageOutputBytes();
    }

    public static LoopbackMessageBytes prepareForNewMessage() {
        return new LoopbackMessageBytes();
    }

    @Override
    public InputStream getInputStream() {
        return loopbackMessageOutputBytes.getByteBufferInputStream();
    }

    @Override
    public boolean tryProcessing() {
        return true;
    }

    @Override
    public void finishedProcessing() {
    }

    @Override
    public boolean hasMoreData() {
        return false;
    }

    public void writeMessage(Message message) throws IOException {
        loopbackMessageOutputBytes.writeMessage(message);
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
