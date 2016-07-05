/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj.internal.network;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.pcj.Configuration;
import org.pcj.internal.message.Message;

/**
 *
 * @author faramir
 */
public class MessageBytesOutputStream implements AutoCloseable {

    private final Message message;
    private final MessageOutputStream messageOutputStream;
    private final MessageDataOutputStream messageDataOutputStream;

    public MessageBytesOutputStream(Message message) throws IOException {
        this(message, Configuration.CHUNK_SIZE);
    }

    public MessageBytesOutputStream(Message message, short chunkSize) {
        this.message = message;

        this.messageOutputStream = new MessageOutputStream(chunkSize);
        this.messageDataOutputStream = new MessageDataOutputStream(this.messageOutputStream);
    }

    public void writeMessage() throws IOException {
        messageDataOutputStream.writeByte(message.getType().getId());
        messageDataOutputStream.writeInt(message.getMessageId());
        messageDataOutputStream.writeInt(message.getInReplyTo());

        message.writeObjects(messageDataOutputStream);
    }

    public Message getMessage() {
        return message;
    }

    public boolean isClosed() {
        return messageOutputStream.isClosed();
    }

    @Override
    public void close() throws IOException {
        messageOutputStream.close();
    }

    public ByteBuffer getNextBytes() {
        while (true) {
            ByteBuffer bb = messageOutputStream.peekByteBuffer();
            if (bb == null) {
                return null;
            }
            if (bb.hasRemaining()) {
                return bb;
            } else {
                messageOutputStream.pollByteBuffer();
            }
        }
    }

    private static class MessageOutputStream extends OutputStream {

        private static final int HEADER_SIZE = Short.BYTES;
        private static final short LAST_CHUNK_BIT = (short) (1 << (Short.SIZE - 1));
        private final short chunkSize;
        private final Queue<ByteBuffer> queue;
        volatile private ByteBuffer currentByteBuffer;

        public MessageOutputStream(short chunkSize) {
            this.chunkSize = chunkSize;
            this.queue = new ConcurrentLinkedQueue<>();

            allocateBuffer();
        }

        private void allocateBuffer() {
            currentByteBuffer = ByteBuffer.allocate(chunkSize);
            currentByteBuffer.position(HEADER_SIZE);
        }

        private void flush(boolean closed) {
            short length = (short) (currentByteBuffer.position() - HEADER_SIZE);
            if (closed) {
                length = (short) (length | LAST_CHUNK_BIT);
            }
            currentByteBuffer.putShort(0, (short) length);
            currentByteBuffer.flip();
            queue.offer(currentByteBuffer);
        }

        @Override
        public void flush() {
            flush(false);
            allocateBuffer();
        }

        @Override
        public void close() throws IOException {
            flush(true);
            currentByteBuffer = null;
        }

        public boolean isClosed() {
            return currentByteBuffer == null && queue.isEmpty();
        }

        private ByteBuffer peekByteBuffer() {
            return queue.peek();
        }

        private ByteBuffer pollByteBuffer() {
            return queue.poll();
        }

        @Override
        public void write(int b) {
            currentByteBuffer.put((byte) b);
            if (currentByteBuffer.hasRemaining() == false) {
                flush();
            }
        }

        @Override
        public void write(byte[] b) {
            write(b, 0, b.length);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            for (int i = 0; i < len; i++) {
                write(b[off + i]);
            }
        }
    }
}
