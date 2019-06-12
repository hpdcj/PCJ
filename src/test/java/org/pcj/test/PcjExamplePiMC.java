/*
 * Copyright (c) 2016, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.test;

import java.util.Arrays;
import java.util.Random;
import org.pcj.PCJ;
import org.pcj.RegisterStorage;
import org.pcj.StartPoint;
import org.pcj.Storage;
import org.pcj.test.PcjExamplePiMC.SharedEnum;

@RegisterStorage(SharedEnum.class)
public class PcjExamplePiMC implements StartPoint {

    @Storage(PcjExamplePiMC.class)
    enum SharedEnum {
        circleCountArray
    }

    private long[] circleCountArray = PCJ.myId() == 0 ? new long[PCJ.threadCount()] : null;

    @Override
    public void main() {
        Random random = new Random();
        long nAll = 512_000_000;
        long n = nAll / PCJ.threadCount();

        double time = System.nanoTime();
// Calculate
        long circleCount = 0;
        for (long i = 0; i < n; ++i) {
            double x = 2.0 * random.nextDouble() - 1.0;
            double y = 2.0 * random.nextDouble() - 1.0;
            if ((x * x + y * y) < 1.0) {
                circleCount++;
            }
        }
        PCJ.barrier();
// Gather results
        PCJ.put(circleCount, 0, SharedEnum.circleCountArray, PCJ.myId());

        if (PCJ.myId() == 0) {
            PCJ.waitFor(SharedEnum.circleCountArray, PCJ.threadCount());

// Calculate pi
            long c = Arrays.stream(circleCountArray).sum();
            double pi = 4.0 * (double) c / (double) nAll;
            time = System.nanoTime() - time;
// Print results
            System.out.println(pi + " " + time * 1.0E-9 + "s " + (pi - Math.PI));
        }
    }

    public static void main(String[] args) {

        String[] nodes = {"localhost", "localhost", "localhost", "localhost"};
        PCJ.executionBuilder(PcjExamplePiMC.class).addNodes(nodes).deploy();
    }
}
