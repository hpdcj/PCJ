/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj.internal.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.pcj.internal.Configuration;

/**
 *
 * @author faramir
 */
public class CloneInputStreamData extends InputStream implements Serializable {

    private final InputStream in;
    private final int chunkSize;
    private List<byte[]> bytesList;
    private Iterator<byte[]> iterator;
    private byte[] bytes;
    private int index;
    private long length;
    private boolean cloned;

    public CloneInputStreamData(InputStream in) {
        this(in, Configuration.CHUNK_SIZE);
    }

    public CloneInputStreamData(InputStream in, int chunkSize) {
        this.in = in;
        this.chunkSize = chunkSize;

        cloned = false;

        length = 0;

        bytesList = new ArrayList<>();
        bytes = new byte[0];
        index = 0;
    }

    @Override
    public synchronized void reset() throws IOException {
        index = 0;
        iterator = bytesList.iterator();
        if (iterator.hasNext()) {
            bytes = iterator.next();
        } else {
            bytes = new byte[0];
        }
    }

    @Override
    public int read() throws IOException {
        if (cloned == false) {
            return readAndClone();
        } else {
            if (index < bytes.length) {
                return bytes[index++];
            }
            while (iterator.hasNext()) {
                bytes = iterator.next();
                index = 0;
                if (index < bytes.length) {
                    return bytes[index++];
                }
            }
            return -1;
        }
    }

    public void cloneFully() throws IOException {
        int b;
        while ((b = in.read()) != -1) {
            addByte(b);
        }
        cloneCompleted();
        reset();
    }

    private int readAndClone() throws IOException {
        int b = in.read();
        if (b == -1) {
            cloneCompleted();
        } else {
            addByte(b);
        }
        return b;
    }

    private void addByte(int b) {
        if (index == bytes.length) {
            bytes = new byte[chunkSize];
            bytesList.add(bytes);
            index = 0;
        }
        bytes[index++] = (byte) b;
        length++;
    }

    public void cloneCompleted() {
        cloned = true;
    }

    public long getLength() {
        return length;
    }

    private void readObject(ObjectInputStream in) throws IOException {
        length = in.readLong();
        bytesList = new ArrayList<>();
        for (long left = length; left > 0; left -= bytes.length) {
            int len = (int) Math.min((long) Integer.MAX_VALUE, left);
            bytes = new byte[len];
            in.readFully(bytes, 0, len);
            bytesList.add(bytes);
        }
        cloned = true;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeLong(length);
        long written = 0;
        for (byte[] bytesArray : bytesList) {
            int len = (int) Math.min(bytesArray.length, length - written);
            out.write(bytesArray, 0, len);
        }
    }

}
