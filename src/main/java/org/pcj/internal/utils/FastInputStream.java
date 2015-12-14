/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.utils;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * InputStream that should be faster than
 * {@link java.io.ByteArrayInputStream}.
 * <p>
 * It is a pair for {@link FastOutputStream}
 *
 * @author faramir
 */
public final class FastInputStream extends InputStream {

    private final Iterator<byte[]> iterator;
    private final int globalSize;
    private byte[] buf;
    private int globalPos;
    private int localPos;
    private byte[] temp;

    public FastInputStream(byte[] buf) {
        this(buf, buf.length);
    }

    public FastInputStream(byte[] buf, int size) {
        this(Arrays.<byte[]>asList(buf), size);
    }

    FastInputStream(List<byte[]> buffers, int size) {
        this.iterator = buffers.iterator();
        this.globalSize = size;
        this.globalPos = 0;
        this.localPos = 0;
        this.temp = new byte[32];

        if (iterator.hasNext()) {
            this.buf = this.iterator.next();
        } else {
            throw new IllegalArgumentException("List empty!");
        }
    }

    @Override
    public int available() {
        return globalSize - globalPos;
    }

    @Override
    public int read() {
        if (globalPos >= globalSize) {
            return -1;
        }
        if (localPos >= buf.length) {
            if (iterator.hasNext()) {
                buf = iterator.next();
                localPos = 0;
            } else {
                return -1;
            }
        }
        globalPos++;
        return buf[localPos++] & 0xff;
    }

    @Override
    public int read(byte[] b) {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) {
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        } else if (globalPos >= globalSize) {
            return -1;
        } else if ((globalPos + len) > globalSize) {
            len = (globalSize - globalPos);
        }

        int readLen = 0;
        int len0;
        while (len > 0) {
            len0 = Math.min(buf.length - localPos, len);
            System.arraycopy(buf, localPos, b, off, len0);
            globalPos += len0;
            localPos += len0;
            off += len0;
            readLen += len0;
            len -= len0;
            if (len > 0 && localPos >= buf.length) {
                if (iterator.hasNext()) {
                    buf = iterator.next();
                    localPos = 0;
                } else {
                    break;
                }
            }
        }
        return readLen;
    }

    @Override
    public long skip(long n) {
        long readLen = 0;
        long len0;
        while (n > 0) {
            len0 = Math.min((long) buf.length - localPos, n);
            globalPos += len0;
            localPos += len0;
            readLen += len0;
            n -= len0;
            if (n > 0 && localPos >= buf.length) {
                if (iterator.hasNext()) {
                    buf = iterator.next();
                    localPos = 0;
                } else {
                    break;
                }
            }
        }

        return readLen;
    }

    public byte readByte() {
        return (byte) (read() & 0xff);
    }

    public int readInt() {
        if (read(temp, 0, 4) < 4) {
            throw new IndexOutOfBoundsException();
        }
        return bytesToInt(temp);
    }

    public long readLong() {
        if (read(temp, 0, 8) < 8) {
            throw new IndexOutOfBoundsException();
        }
        return bytesToLong(temp);
    }

    public float readFloat() {
        if (read(temp, 0, 4) < 4) {
            throw new IndexOutOfBoundsException();
        }
        return Float.intBitsToFloat(bytesToInt(temp));
    }

    public double readDouble() {
        if (read(temp, 0, 8) < 8) {
            throw new IndexOutOfBoundsException();
        }
        return Double.longBitsToDouble(bytesToLong(temp));
    }

    private int bytesToInt(byte[] bytes) {
        return (((int) (bytes[0] & 0xff) << 24)
                | ((int) (bytes[1] & 0xff) << 16)
                | ((int) (bytes[2] & 0xff) << 8)
                | ((int) (bytes[3] & 0xff)));
    }

    private long bytesToLong(byte[] bytes) {
        return (((long) (bytes[0] & 0xff) << 56)
                | ((long) (bytes[1] & 0xff) << 48)
                | ((long) (bytes[2] & 0xff) << 40)
                | ((long) (bytes[3] & 0xff) << 32)
                | ((long) (bytes[4] & 0xff) << 24)
                | ((long) (bytes[5] & 0xff) << 16)
                | ((long) (bytes[6] & 0xff) << 8)
                | ((long) (bytes[7] & 0xff)));
    }
}
