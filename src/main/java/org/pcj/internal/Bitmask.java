/* 
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal;

/**
 * Class to manipulate large set of bits. This class is not <i>Thread-safe</i>.
 * External synchronization is required.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class Bitmask {

    private int length = 0;
//    AtomicLongArray ala=new AtomicLongArray(...);
    private long[] value = new long[length];
    private long[] all = new long[length];
    private static final long MASK = 0xffff_ffff_ffff_ffffL;
    private static final long UPPER_BIT = 1L << (Long.SIZE - 1);
    private static final int MASK_SIZE = Long.SIZE;

    public Bitmask() {
        this(0);
    }

    public Bitmask(int length) {
        ensureLength(length);
    }

    private static int calcArrayLength(int length) {
        return (length + MASK_SIZE - 1) / MASK_SIZE;
    }

    public Bitmask(Bitmask localBitmask) {
        this(localBitmask.getSize());
        System.arraycopy(localBitmask.value, 0, this.value, 0, localBitmask.value.length);
        System.arraycopy(localBitmask.all, 0, this.all, 0, localBitmask.all.length);
    }

    private void ensureLength(int length) {
        if (this.length == length) {
            return;
        }
        int newArrayLength = calcArrayLength(length);
        all = new long[newArrayLength];
        for (int i = 0; i < all.length; ++i) {
            all[i] = MASK;
        }
        if (length % MASK_SIZE != 0) {
            all[all.length - 1] &= (1L << (length % MASK_SIZE)) - 1;
        }

        long[] oldValue = value;
        value = new long[newArrayLength];

        int min = Math.min(oldValue.length, value.length);
        for (int i = 0; i < min; ++i) {
            value[i] = all[i] & oldValue[i];
        }
        this.length = length;
    }

    synchronized public int getSize() {
        return length;
    }

    synchronized public void setSize(int length) {
        ensureLength(length);
    }

    synchronized public void enlarge(int length) {
        if (this.length < length) {
            ensureLength(length);
        }
    }

    /**
     *
     * @param position place to set (0..length-1)
     */
    synchronized public void set(int position) {
        if (position >= length || position < 0) {
            throw new IllegalArgumentException("Position " + position + " is out of bound (size: " + length + ").");
        }
        value[position / MASK_SIZE] |= 1L << (position % MASK_SIZE);
    }

    synchronized public void clear(int position) {
        if (position > length || position < 0) {
            throw new IllegalArgumentException("Position " + position + " is out of bound.");
        }
        value[position / MASK_SIZE] &= MASK ^ (1L << (position % MASK_SIZE));
    }

    synchronized public void clear() {
        for (int i = 0; i < value.length; ++i) {
            value[i] = 0;
        }
    }

    /**
     * Checks if every bit of BitMask is set
     *
     * @return true if every bit of BitMask is set
     */
    synchronized public boolean isSet() {
        for (int i = 0; i < value.length; ++i) {
            if (value[i] != all[i]) {
                return false;
            }
        }
        return true;
    }

    synchronized public boolean isSet(int position) {
        return (value[position / MASK_SIZE] & (1L << (position % MASK_SIZE))) != 0;
    }

    /**
     * Checks if this BitMask has set the same bits as other BitMask. In other
     * words: <i>this & other = other</i>
     *
     * @param other
     *
     * @return false if size of BitMasks are different or there is difference in
     *         bits.
     */
    synchronized public boolean isSet(Bitmask other) {
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
}
