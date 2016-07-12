/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj.internal;

/**
 *
 * @author faramir
 */
public class LocalBarrier {

    private final int round;
    private final WaitObject waitObject;
    private final Bitmask localBarrierBitmask;
    private final Bitmask localBarrierMaskBitmask;

    public LocalBarrier(int round, Bitmask localBitmask) {
        this.round = round;
        this.waitObject = new WaitObject();
        this.localBarrierBitmask = new Bitmask(localBitmask.getSize());
        this.localBarrierMaskBitmask = new Bitmask(localBitmask);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LocalBarrier == false) {
            return false;
        }
        return ((LocalBarrier) obj).round == this.round;
    }

    @Override
    public int hashCode() {
        return round;
    }

    public int getRound() {
        return round;
    }

    public void set(int index) {
        localBarrierBitmask.set(index);
    }

    public boolean isSet() {
        return localBarrierBitmask.isSet(localBarrierMaskBitmask);
    }

    public void signalAll() {
        waitObject.signalAll();
    }

    public void await() throws InterruptedException {
        waitObject.await();
    }
}
