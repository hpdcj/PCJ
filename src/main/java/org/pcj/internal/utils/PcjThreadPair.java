/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.utils;

/**
 * Class used for synchronizing two PCJ Threads.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class PcjThreadPair {

    private volatile int val;
    private final int first;
    private final int second;

    public PcjThreadPair(int first, int second) {
        this.first = first;
        this.second = second;

        val = 0;
    }

    @Override
    public int hashCode() {
        return first * second;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof PcjThreadPair == false) {
            return false;
        }

        final PcjThreadPair other = (PcjThreadPair) obj;
        return (this.first == other.first && this.second == other.second);
                //|| (this.first == other.second && this.second == other.first);
    }

    synchronized public void increase() {
        ++val;
    }

    synchronized public void decrease() {
        --val;
    }

    synchronized public void clear() {
        val = 0;
    }

    synchronized public int get() {
        return val;
    }
}
