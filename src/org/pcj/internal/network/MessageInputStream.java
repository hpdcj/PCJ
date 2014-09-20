/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.network;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.pcj.internal.utils.FastInputStream;

/**
 * Input stream for reading messages from bytes.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class MessageInputStream extends InputStream {

    private FastInputStream fis;

    public MessageInputStream(FastInputStream buf) {
        this.fis = buf;
    }

    public MessageInputStream(byte[] b) {
        this(new FastInputStream(b));
    }

    @Override
    public int read() {
        return fis.read() & 0xff;
    }

    public int readInt() {
        return fis.readInt();
    }

    public int[] readIntArray() {
        int len = fis.readInt();
        int[] array = new int[len];
        for (int i = 0; i < len; ++i) {
            array[i] = fis.readInt();
        }
        return array;
    }

    public byte readByte() {
        return fis.readByte();
    }

    public byte[] readByteArray() {
        byte[] b = new byte[fis.readInt()];
        fis.read(b);
        return b;
    }

    public String readString() {
        return new String(readByteArray(), StandardCharsets.UTF_8);
    }
}
