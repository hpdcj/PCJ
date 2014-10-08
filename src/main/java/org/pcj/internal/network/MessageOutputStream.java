/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.network;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.pcj.internal.utils.FastOutputStream;

/**
 * Output stream to save Messages as bytes.
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class MessageOutputStream extends OutputStream {

    private FastOutputStream fos;

    public MessageOutputStream() {
        this(1024);
    }

    public MessageOutputStream(int capacity) {
        fos = new FastOutputStream(capacity);
    }

    public ByteBuffer getByteBuffer() {
        return ByteBuffer.wrap(fos.toByteArray());
    }

    @Override
    public void write(byte[] b) {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        fos.write(b, off, len);
    }

    @Override
    public void write(int b) {
        fos.write((byte) (b & 0xFF));
    }

//    public byte[] toByteArray() {
//        return fos.array();
//    }
    public void writeInt(int v) {
        fos.writeInt(v);
    }

    public void writeIntArray(int[] array) {
        fos.writeInt(array.length);
        for (int a : array) {
            fos.writeInt(a);
        }
    }

    public void writeByte(byte b) {
        fos.write(b);
    }

    public void writeString(String string) {
        // FIXME: check if string is null, and read data in MessageInputStream
//        if (string == null) {
//            fos.writeInt(-1);
//        } else {
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        fos.writeInt(bytes.length);
        write(bytes);
//        }
    }

//    public void writeObject(Object object) throws IOException {
//        new ObjectOutputStream(this).writeObject(object);
//    }
    public void writeByteArray(byte[] array) {
        fos.writeInt(array.length);
        fos.write(array);
    }
}
