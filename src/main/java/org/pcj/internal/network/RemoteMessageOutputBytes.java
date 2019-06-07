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
import java.nio.ByteBuffer;
import org.pcj.internal.message.Message;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class RemoteMessageOutputBytes implements MessageOutputBytes {

    private final ByteBufferOutputStream byteBufferOutputStream;

    public RemoteMessageOutputBytes() {
        byteBufferOutputStream = new ByteBufferOutputStream();
    }

    @Override
    public void writeMessage(Message message) throws IOException {
        try (MessageDataOutputStream messageDataOutputStream = new MessageDataOutputStream(byteBufferOutputStream)) {
            messageDataOutputStream.writeByte(message.getType().getId());
            message.write(messageDataOutputStream);
        }
    }

    ByteBufferArray getByteBufferArray() {
        return new ByteBufferArray(byteBufferOutputStream);
    }

    public static class ByteBufferArray {
        private static final ByteBuffer[] EMPTY_ARRAY = new ByteBuffer[0];
        private final ByteBufferOutputStream byteBufferOutputStream;
        private ByteBuffer[] array;
        private int offset;

        public ByteBufferArray(ByteBufferOutputStream byteBufferOutputStream) {
            this.byteBufferOutputStream = byteBufferOutputStream;

            this.array = EMPTY_ARRAY;
        }

        public ByteBuffer[] getArray() {
            if (offset == array.length) {
                array = byteBufferOutputStream.getDeque()
                                .stream()
                                .map(ByteBufferPool.PooledByteBuffer::getByteBuffer)
                                .toArray(ByteBuffer[]::new);
                offset = 0;
            }
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
                byteBufferOutputStream.getDeque().remove().returnToPool();
                ++offset;
            }
        }

        public boolean hasMoreData() {
            return offset < array.length
                           || !byteBufferOutputStream.isClosed()
                           || !byteBufferOutputStream.getDeque().isEmpty();
        }
    }
}