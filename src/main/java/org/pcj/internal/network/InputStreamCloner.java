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
import org.pcj.internal.InternalPCJ;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class InputStreamCloner {

    private static final int CHUNK_SIZE = InternalPCJ.getConfiguration().getBufferChunkSize();
    private static final byte[] EMPTY_ARRAY = new byte[0];
    private final List<byte[]> bytesList;
    private long length;

    private InputStreamCloner() {
        bytesList = new LinkedList<>();
        length = 0L;
    }

    public long getLength() {
        return length;
    }

    private void addByteArray(byte[] byteArray) {
        bytesList.add(byteArray);
        length += byteArray.length;
    }

    public static InputStreamCloner clone(InputStream in) throws IOException {
        InputStreamCloner inputStreamCloner = new InputStreamCloner();

        byte[] bytes = new byte[CHUNK_SIZE];
        int r;
        int offset = 0;
        while ((r = in.read(bytes, offset, bytes.length - offset)) != -1) {
            offset += r;
            if (offset == bytes.length) {
                inputStreamCloner.addByteArray(bytes);

                bytes = new byte[CHUNK_SIZE];
                offset = 0;
            }
        }

        if (offset > 0) {
            byte[] dest = new byte[offset];
            System.arraycopy(bytes, 0, dest, 0, offset);
            inputStreamCloner.addByteArray(dest);
        }

        return inputStreamCloner;
    }

    public static InputStreamCloner readFrom(MessageDataInputStream in) throws IOException {
        InputStreamCloner inputStreamCloner = new InputStreamCloner();
        long length = in.readLong();

        byte[] bytes;
        for (long left = length; left > 0; left -= bytes.length) {
            int len = (int) Math.min((long) CHUNK_SIZE, left);
            bytes = new byte[len];
            in.readFully(bytes);
            inputStreamCloner.addByteArray(bytes);
        }
        return inputStreamCloner;
    }

    public void writeInto(MessageDataOutputStream out) throws IOException {
        out.writeLong(length);

        for (byte[] bytesArray : bytesList) {
            out.write(bytesArray, 0, bytesArray.length);
        }
    }

    public ClonedInputStream newInputStream() {
        return new ClonedInputStream(bytesList);
    }

    public static class ClonedInputStream extends InputStream {
        private Iterator<byte[]> iterator;
        private byte[] currentByteArray;
        private int currentIndex;

        private ClonedInputStream(List<byte[]> bytesList) {
            iterator = bytesList.iterator();
            currentByteArray = EMPTY_ARRAY;
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
    }
}
