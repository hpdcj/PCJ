/* 
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.network;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class MessageDataOutputStream extends OutputStream {

    private final OutputStream output;
    private ObjectOutputStream objectOutputStream;

    public MessageDataOutputStream(OutputStream output) {
        this.output = output;
    }

    @Override
    public void write(int b) throws IOException {
        output.write(b);
    }

    @Override
    public void close() throws IOException {
        output.close();
    }

    private byte[] intToBytes(int value) {
        byte[] temp = new byte[Integer.BYTES];

        temp[0] = (byte) ((value >> 24) & 0xff);
        temp[1] = (byte) ((value >> 16) & 0xff);
        temp[2] = (byte) ((value >> 8) & 0xff);
        temp[3] = (byte) (value & 0xff);

        return temp;
    }

    private byte[] longToBytes(long value) {
        byte[] temp = new byte[Long.BYTES];

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

    public void writeBoolean(boolean value) throws IOException {
        output.write(value ? 1 : 0);
    }

    public void writeByte(byte value) throws IOException {
        output.write(value);
    }

    public void writeByteArray(byte[] array) throws IOException {
        if (array == null) {
            writeInt(-1);
        } else {
            writeInt(array.length);
            output.write(array, 0, array.length);
        }
    }

    public void writeDouble(double value) throws IOException {
        byte[] bytes = longToBytes(Double.doubleToRawLongBits(value));
        write(bytes, 0, bytes.length);
    }

    public void writeDoubleArray(double[] array) throws IOException {
        if (array == null) {
            writeInt(-1);
        } else {
            writeInt(array.length);
            for (double a : array) {
                writeDouble(a);
            }
        }
    }

    public void writeFloat(float value) throws IOException {
        byte[] bytes = intToBytes(Float.floatToRawIntBits(value));
        output.write(bytes, 0, bytes.length);
    }

    public void writeFloatArray(float[] array) throws IOException {
        if (array == null) {
            writeInt(-1);
        } else {
            writeInt(array.length);
            for (float a : array) {
                writeFloat(a);
            }
        }
    }

    public void writeInt(int value) throws IOException {
        byte[] bytes = intToBytes(value);
        output.write(bytes, 0, bytes.length);
    }

    public void writeIntArray(int[] array) throws IOException {
        if (array == null) {
            writeInt(-1);
        } else {
            writeInt(array.length);
            for (int a : array) {
                writeInt(a);
            }
        }
    }

    public void writeLong(long value) throws IOException {
        byte[] bytes = longToBytes(value);
        output.write(bytes, 0, bytes.length);
    }

    public void writeLongArray(long[] array) throws IOException {
        if (array == null) {
            writeInt(-1);
        } else {
            writeInt(array.length);
            for (long a : array) {
                writeLong(a);
            }
        }
    }

    public void writeString(String string) throws IOException {
        if (string == null) {
            writeInt(-1);
        } else {
            byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
            writeInt(bytes.length);
            output.write(bytes, 0, bytes.length);
        }
    }

    public void writeObject(Object object) throws IOException {
        if (objectOutputStream == null) {
            objectOutputStream = new ObjectOutputStream(output);
        }
        objectOutputStream.writeObject(object);
    }
}
