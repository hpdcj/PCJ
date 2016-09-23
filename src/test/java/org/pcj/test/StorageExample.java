/*
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.test;

import java.util.Arrays;
import org.pcj.NodesDescription;
import org.pcj.PCJ;
import org.pcj.StartPoint;
import org.pcj.Storage;
import org.pcj.test.StorageExample.SharedEnum;
import org.pcj.RegisterStorage;

/**
 *
 * @author faramir
 */
@RegisterStorage(SharedEnum.class)
public class StorageExample implements StartPoint {

    @Storage(StorageExample.class)
    enum SharedEnum {
        x, avg;
    }
    int[] x; // Shared bo w Enum
    final static double[] STATIC_TEST = new double[PCJ.threadCount()];
    double avg; // Shared bo w Enum
    float obiekt_nie_w_Enum_wiec_nie_Shared;

    @Override
    public void main() throws Throwable {
        System.out.println("size of STATIC_TEST: " + STATIC_TEST.length);
        if (PCJ.myId() == 0) {
            x = new int[PCJ.threadCount()];
//            PCJ.putLocal(SharedEnum.x, x);
        }
        PCJ.barrier();
        PCJ.put(PCJ.myId() + 1, 0, SharedEnum.x, PCJ.myId());
        PCJ.barrier();
        if (PCJ.myId() == 0) {
//            avg = Arrays.stream(PCJ.<int[]>getLocal(SharedEnum.x)).average().orElse(Double.NaN);
            avg = Arrays.stream(x).average().orElse(Double.NaN);
            PCJ.broadcast(avg, SharedEnum.avg);
        }
        PCJ.waitFor(SharedEnum.avg);
//        System.out.println(PCJ.myId() + ": avg = " + PCJ.<Double>getLocal(SharedEnum.avg));
        System.out.println(PCJ.myId() + ": avg = " + avg);
    }
}
