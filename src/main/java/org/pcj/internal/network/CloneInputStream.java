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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.pcj.internal.Configuration;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class CloneInputStream extends InputStream {

    private static final int CHUNK_SIZE = Configuration.BUFFER_CHUNK_SIZE;
    private static final byte[] EMPTY = new byte[0];

    private final List<byte[]> bytesList;
    private Iterator<byte[]> iterator;
    private byte[] currentByteArray;
    private int currentIndex;
    private long length;

    private CloneInputStream() {
        bytesList = new LinkedList<>();
        length = 0L;
    }

    @Override
    public void reset() {
        iterator = bytesList.iterator();
        currentByteArray = EMPTY;
        currentIndex = 0;
    }

    @Override
    public int read() {
        while (currentIndex == currentByteArray.length) {
            if (iterator.hasNext()) {
                currentByteArray = iterator.next();
                currentIndex = 0;
            } else {
                return -1;
            }
        }
        return currentByteArray[currentIndex++] & 0xFF;
    }

    @Override
    public int read(byte[] b) {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int offset, int length) {
        if (length == 0) {
            return 0;
        }
        int r = 0;
        do {
            int len = Math.min(length, currentByteArray.length - currentIndex);
            System.arraycopy(currentByteArray, currentIndex, b, offset, len);
            length -= len;
            offset += len;
            r += len;
            currentIndex += len;
            if (currentIndex == currentByteArray.length) {
                if (iterator.hasNext()) {
                    currentByteArray = iterator.next();
                    currentIndex = 0;
                } else {
                    break;
                }
            }
        } while (length > 0);

        if (r == 0) {
            return -1;
        }
        return r;
    }

    public long getLength() {
        return length;
    }

    private void addByteArray(byte[] byteArray) {
        bytesList.add(byteArray);
        length += byteArray.length;
    }

    public static CloneInputStream clone(InputStream in) throws IOException {
        CloneInputStream cloneInputStream = new CloneInputStream();

        byte[] bytes = new byte[CHUNK_SIZE];
        int r;
        int offset = 0;
        while ((r = in.read(bytes, offset, bytes.length - offset)) != -1) {
            offset += r;
            if (offset == bytes.length) {
                cloneInputStream.addByteArray(bytes);

                bytes = new byte[CHUNK_SIZE];
                offset = 0;
            }
        }

        if (offset > 0) {
            byte[] dest = new byte[offset];
            System.arraycopy(bytes, 0, dest, 0, offset);
            cloneInputStream.addByteArray(dest);
        }

        return cloneInputStream;
    }

    public static CloneInputStream readFrom(MessageDataInputStream in) throws IOException {
        CloneInputStream cloneInputStream = new CloneInputStream();
        long length = in.readLong();

        byte[] bytes;
        for (long left = length; left > 0; left -= bytes.length) {
            int len = (int) Math.min((long) CHUNK_SIZE, left);
            bytes = new byte[len];
            in.readFully(bytes);
            cloneInputStream.addByteArray(bytes);
        }
        return cloneInputStream;
    }

    public void writeInto(MessageDataOutputStream out) throws IOException {
        out.writeLong(length);

        for (byte[] bytesArray : bytesList) {
            out.write(bytesArray, 0, bytesArray.length);
        }
    }
}
