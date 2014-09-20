/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.utils;

import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * OutputStream that should be faster than
 * {@link java.io.ByteArrayOutputStream}.
 * <p>
 * It is a pair for {@link FastInputStream}. Additionally, it
 * can very quickly become a FastInputStream.
 *
 * @author faramir
 */
public final class FastOutputStream extends OutputStream {

    private final List<byte[]> buffers;
    private byte[] buf;
    private int enlarge;
    private int globalSize;
    private int localPos;
    private byte[] temp;

    public FastOutputStream() {
        this(1024);
    }

    public FastOutputStream(int capacity) {
        this.buffers = new LinkedList<>();
        this.buf = new byte[capacity];
        this.globalSize = 0;
        this.localPos = 0;
        this.enlarge = capacity;
        this.temp = new byte[32];

        this.buffers.add(buf);
    }

    public byte[] toByteArray() {
        byte[] bytes = new byte[globalSize];
        int destPos = 0;
        int len;

        for (byte[] b : buffers) {
            len = Math.min(b.length, globalSize - destPos);
            System.arraycopy(b, 0, bytes, destPos, len);
            destPos += len;
        }

        return bytes;
    }

    @Override
    public void write(byte b[]) {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte b[], int off, int len) {
        if (localPos + len > buf.length) {
            int len0 = buf.length - localPos;
            System.arraycopy(b, off, buf, localPos, len0);
            off += len0;
            len -= len0;
            globalSize += len0;

            buf = new byte[Math.max(enlarge, len)];
            buffers.add(buf);
            localPos = 0;
        }
        System.arraycopy(b, off, buf, localPos, len);
        localPos += len;
        globalSize += len;
    }

    @Override
    public void write(int b) {
        if (localPos >= buf.length) {
            buf = new byte[enlarge];
            buffers.add(buf);
            localPos = 0;
        }
        buf[localPos++] = (byte) (b & 0xff);
        globalSize++;
    }

    public FastInputStream getFastInputStream() {
        return new FastInputStream(buffers, globalSize);
    }

    public void writeByte(byte value) {
        write(value);
    }

    public void writeInt(int value) {
        byte[] bytes = intToBytes(value);
        write(bytes, 0, 4);
    }

    public void writeLong(long value) {
        byte[] bytes = longToBytes(value);
        write(bytes, 0, 8);
    }

    public void writeFloat(float value) {
        byte[] bytes = intToBytes(Float.floatToRawIntBits(value));
        write(bytes, 0, 4);
    }

    public void writeDouble(double value) {
        byte[] bytes = longToBytes(Double.doubleToRawLongBits(value));
        write(bytes, 0, 8);
    }

    private byte[] intToBytes(int value) {
        temp[0] = (byte) ((value >> 24) & 0xff);
        temp[1] = (byte) ((value >> 16) & 0xff);
        temp[2] = (byte) ((value >> 8) & 0xff);
        temp[3] = (byte) (value & 0xff);

        return temp;
    }

    private byte[] longToBytes(long value) {
        temp[0] = (byte) ((value >> 56) & 0xff);
        temp[1] = (byte) ((value >> 48) & 0xff);
        temp[2] = (byte) ((value >> 40) & 0xff);
        temp[3] = (byte) ((value >> 32) & 0xff);
        temp[4] = (byte) ((value >> 24) & 0xff);
        temp[5] = (byte) ((value >> 16) & 0xff);
        temp[6] = (byte) ((value >> 8) & 0xff);
        temp[7] = (byte) (value & 0xff);

        return temp;
    }
}
