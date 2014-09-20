/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.utils;

/**
 * Class to manipulate large set of bits. This class is not <i>Thread-safe</i>.
 * External synchronization is required.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class BitMask {

    private int length = 0;
    //AtomicLongArray ala=new AtomicLongArray(...);
    private long[] value = new long[length];
    private long[] all = new long[length];
    private static final long MASK = 0xffffffffffffffffL;
    private static final long UPPER_BIT = 1L << 63;
    private static final int MASK_SIZE = Long.SIZE;

    public BitMask() {
        this(0);
    }

    public BitMask(int length) {
        ensureLength(length);
    }

    private static int calcLength(int length) {
        return (length + MASK_SIZE - 1) / MASK_SIZE;
    }

    private void ensureLength(int length) {
        all = new long[calcLength(length)];
        for (int i = 0; i < all.length; ++i) {
            all[i] = MASK;
        }
        if (length % MASK_SIZE != 0) {
            all[all.length - 1] &= (1L << (length % MASK_SIZE)) - 1;
        }

        long[] oldValue = value;
        value = new long[calcLength(length)];

        int min = Math.min(oldValue.length, value.length);
        for (int i = 0; i < min; ++i) {
            value[i] = all[i] & oldValue[i];
        }
        this.length = length;
    }

    public int getSize() {
        return length;
    }

    public void setSize(int length) {
        ensureLength(length);
    }

    /**
     *
     * @param position place to set (0..length-1)
     */
    public void set(int position) {
        if (position >= length || position < 0) {
            throw new IllegalArgumentException("Position " + position + " is out of bound (size: " + length + ").");
        }
        value[position / MASK_SIZE] |= 1L << (position % MASK_SIZE);
    }

    public void clear(int position) {
        if (position > length || position < 0) {
            throw new IllegalArgumentException("Position " + position + " is out of bound.");
        }
        value[position / MASK_SIZE] &= all[position / MASK_SIZE] ^ (1L << (position % MASK_SIZE));
    }

    public void clear() {
        for (int i = 0; i < value.length; ++i) {
            value[i] = 0;
        }
    }

    /**
     * Checks if every bit of BitMask is set
     *
     * @return true if every bit of BitMask is set
     */
    public boolean isSet() {
        for (int i = 0; i < value.length; ++i) {
            if (value[i] != all[i]) {
                return false;
            }
        }
        return true;
    }

    public boolean isSet(int position) {
        return (value[position / MASK_SIZE] & (1L << (position % MASK_SIZE))) != 0;
    }

    /**
     * Checks if this BitMask has set the same bits as other BitMask. In other
     * words: <i>this & other = other</i>
     *
     * @param other
     * @return false if size of BitMasks are different or there is difference in
     * bits.
     */
    public boolean isSet(BitMask other) {
        if (other.length != length) {
            return false;
        }

        for (int i = 0; i < value.length; ++i) {
            if ((value[i] & other.value[i]) != other.value[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(length);
        long l = 0;
        for (int i = 0; i < length; ++i) {
            if (i % MASK_SIZE == 0) {
                l = 1;
                if (i > 0) {
                    sb.append(' ');
                }
            } else {
                l <<= 1;
            }
            sb.append((value[i / MASK_SIZE] & l) == 0 ? '0' : '1');
        }
        return sb.toString();
    }

    public void insert(int position, int bit) {
        long v;
        if (bit == 0) {
            v = 0L;
        } else {
            v = 1L;
        }

        if (position < length) {
            ensureLength(length + 1);
            int index = position / MASK_SIZE;
            long carry = (value[index] & UPPER_BIT) >>> (MASK_SIZE - 1);
            int shift = (position % MASK_SIZE);
            if (shift + 1 == MASK_SIZE) {
                value[index] = (value[index] & (UPPER_BIT - 1)) | (v << shift);
            } else {
                long lo = value[index] & ((1L << shift) - 1);
                value[index] = ((value[index] ^ lo) << 1) | (v << shift) | lo;
            }
            for (++index; index < value.length; ++index) {
                long temp = (value[index] & UPPER_BIT) >>> (MASK_SIZE - 1);
                value[index] = (value[index] << 1) | carry;
                carry = temp;
            }
        } else {
            ensureLength(length + 1);
            if (v == 1L) {
                set(position);
            }
        }
    }
}
