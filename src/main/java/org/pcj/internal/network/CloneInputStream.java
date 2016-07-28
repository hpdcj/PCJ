/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj.internal.network;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.pcj.internal.Configuration;

/**
 *
 * @author faramir
 */
public class CloneInputStream extends InputStream {

    private static final int BYTES_CHUNK_SIZE = Configuration.CHUNK_SIZE;
    private static final ByteArray EMPTY = new ByteArray(0);
    private final List<ByteArray> bytesList;
    private Iterator<ByteArray> iterator;
    private ByteArray currentByteArray;
    private long length;

    public CloneInputStream() {
        bytesList = new ArrayList<>();
        length = 0;
    }

    public void clone(InputStream in) throws IOException {
        ByteArray bytes = new ByteArray(BYTES_CHUNK_SIZE);
        int b;
        while ((b = in.read()) != -1) {
            bytes.put((byte) b);
            length++;
            if (bytes.hasRemaining() == false) {
                bytesList.add(bytes);

                bytes = new ByteArray(BYTES_CHUNK_SIZE);
            }
        }

        if (bytes.isEmpty() == false) {
            bytes.truncate();
            bytesList.add(bytes);
        }

        reset();
    }

    @Override
    public void reset() {
        iterator = bytesList.iterator();
        currentByteArray = EMPTY;
    }

    @Override
    public int read() {
        while (currentByteArray.hasRemaining() == false) {
            if (iterator.hasNext()) {
                currentByteArray = iterator.next();
                currentByteArray.reset();
            } else {
                return -1;
            }
        }
        return currentByteArray.get() & 0xFF;
    }
//
//    public static CloneInputStream readFrom(MessageDataInputStream in) throws IOException {
//        CloneInputStream cloneInputStream= new CloneInputStream();
//         long length = in.readLong();
//        
//        byte[] bytes;
//        for (long left = length; left > 0; left -= bytes.length) {
//            int len = (int) Math.min((long) Integer.MAX_VALUE, left);
//            bytes = new byte[len];
//            int offset = 0;
//            while (offset < len) {
//                offset += input.read(bytes, offset, len - offset);
//            }
//            clonedData.addBytes(bytes);
//        }
//    }
    
    public void writeInto(MessageDataOutputStream out) throws IOException {
//        out.writeLong(length);

        long written = 0;
        for (ByteArray byteArray : bytesList) {
            byte[] bytesArray = byteArray.getBytes();
            int len = (int) Math.min(bytesArray.length, length - written);
            out.write(bytesArray, 0, len);
        }
    }

    private static class ByteArray {

        private byte[] bytes;
        private int index;

        public ByteArray(byte[] bytes) {
            this.bytes = bytes;
            index = 0;
        }

        public ByteArray(int length) {
            bytes = new byte[length];
            index = 0;
        }

        public boolean hasRemaining() {
            return index < bytes.length;
        }

        public boolean isEmpty() {
            return index == 0;
        }

        public byte get() {
            return bytes[index++];
        }

        public void put(byte b) {
            bytes[index++] = b;
        }

        public void reset() {
            index = 0;
        }

        public int getLength() {
            return index;
        }

        private void truncate() {
            byte[] dest = new byte[index];
            System.arraycopy(bytes, 0, dest, 0, index);
            bytes = dest;
        }

        public byte[] getBytes() {
            return bytes;
        }
    }
}
