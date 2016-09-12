/* 
 * Copyright (c) 2016, Marek Nowicki
 * All rights reserved.
 * 
 * Licensed under New BSD License (3-clause license).
 * 
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.test;

import java.util.Random;
import org.pcj.NodesDescription;
import org.pcj.PCJ;
import org.pcj.PcjFuture;
import org.pcj.StartPoint;
import org.pcj.Storage;
import org.pcj.test.PcjExamplePiMC.SharedEnum;
import org.pcj.RegisterStorage;

@RegisterStorage(SharedEnum.class)
public class PcjExamplePiMC implements StartPoint {

    @Storage(PcjExamplePiMC.class)
    enum SharedEnum {
        circleCount
    }

    long circleCount;

    @Override
    public void main() {
        Random random = new Random();
        long nAll = 512_000_000;
        long n = nAll / PCJ.threadCount();

        double time = System.nanoTime();
// Calculate  
        for (long i = 0; i < n; ++i) {
            double x = 2.0 * random.nextDouble() - 1.0;
            double y = 2.0 * random.nextDouble() - 1.0;
            if ((x * x + y * y) < 1.0) {
                circleCount++;
            }
        }
        PCJ.barrier();
// Gather results 
        long c = 0;
        PcjFuture cL[] = new PcjFuture[PCJ.threadCount()];

        if (PCJ.myId() == 0) {
            for (int p = 0; p < PCJ.threadCount(); p++) {
                cL[p] = PCJ.asyncGet(p, SharedEnum.circleCount);
            }
            for (int p = 0; p < PCJ.threadCount(); p++) {
                c = c + (long) cL[p].get();
            }
        }
// Calculate pi 
        double pi = 4.0 * (double) c / (double) nAll;
        time = System.nanoTime() - time;
// Print results         
        if (PCJ.myId() == 0) {
            System.out.println(pi + " " + time * 1.0E-9 + "s " + (pi - Math.PI));
        }
    }

    public static void main(String[] args) {

        PCJ.deploy(PcjExamplePiMC.class,
                new NodesDescription(
                        new String[]{"localhost", "localhost", "localhost", "localhost"}
                )
        );
    }
}
