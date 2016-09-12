/* 
 * Copyright (c) 2016, Marek Nowicki
 * All rights reserved.
 * 
 * Licensed under New BSD License (3-clause license).
 * 
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.test;

import org.pcj.NodesDescription;
import org.pcj.PCJ;
import org.pcj.PcjFuture;
import org.pcj.StartPoint;
import org.pcj.Storage;
import org.pcj.test.PcjExamplePiInt.SharedEnum;
import org.pcj.RegisterStorage;

/**
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
@RegisterStorage(SharedEnum.class)
public class PcjExamplePiInt implements StartPoint {

    @Storage(PcjExamplePiInt.class)
    enum SharedEnum {
        sum
    }

    double sum;

    private double f(double x) {
        return (4.0 / (1.0 + x * x));
    }

    @Override
    public void main() throws Throwable {
        double pi = 0.0;
        long time = System.currentTimeMillis();
        for (int i = 1; i < 1000; ++i) {
            pi = calc(1000000);
        }
        time = System.currentTimeMillis() - time;
        if (PCJ.myId() == 0) {
            double err = pi - Math.PI;
            System.out.format("time %d\tsum = %7.5f, err = %10e\n", time, pi, err);
        }
    }

    private double calc(int N) {
        double w;

        w = 1.0 / (double) N;
        for (int i = PCJ.myId() + 1; i <= N; i += PCJ.threadCount()) {
            sum = sum + f(((double) i - 0.5) * w);
        }
        sum = sum * w;

        PcjFuture<Void> barrier = PCJ.asyncBarrier();
        if (PCJ.myId() == 0) {
            barrier.get();
            PcjFuture[] data = new PcjFuture[PCJ.threadCount()];
            for (int i = 1; i < PCJ.threadCount(); ++i) {
                data[i] = PCJ.asyncGet(i, SharedEnum.sum);
            }
            for (int i = 1; i < PCJ.threadCount(); ++i) {
                sum = sum + (double) data[i].get();
            }

            return sum;
        } else {
            return Double.NaN;
        }
    }

    public static void main(String[] args) {
        PCJ.deploy(PcjExamplePiInt.class,
                new NodesDescription(
                        new String[]{
                            "localhost:8091",
                            "localhost:8092",
                            "localhost:8092",
                            "localhost:8093",}));
    }
}
