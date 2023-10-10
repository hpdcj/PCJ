/*
 * Copyright (c) 2011-2022, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.test;

import java.util.Arrays;
import org.pcj.PCJ;
import org.pcj.RegisterStorage;
import org.pcj.StartPoint;
import org.pcj.Storage;
import org.pcj.test.StorageExample.ShareableEnum;

/**
 * @author faramir
 */
@RegisterStorage(ShareableEnum.class)
public class StorageExample implements StartPoint {

    @Storage(StorageExample.class)
    enum ShareableEnum {
        x, avg;
    }

    int[] x; // Shareable bo w Enum
    final static double[] STATIC_TEST = new double[PCJ.threadCount()];
    double avg; // Shareable bo w Enum
    float obiekt_nie_w_Enum_wiec_nie_Shareable;

    @Override
    public void main() throws Throwable {
        System.out.println("size of STATIC_TEST: " + STATIC_TEST.length);
        if (PCJ.myId() == 0) {
            x = new int[PCJ.threadCount()];
//            PCJ.localPut(SharedEnum.x, x);
        }
        PCJ.barrier();
        PCJ.put(PCJ.myId() + 1, 0, ShareableEnum.x, PCJ.myId());
        PCJ.barrier();
        if (PCJ.myId() == 0) {
//            avg = Arrays.stream(PCJ.<int[]>localGet(SharedEnum.x)).average().orElse(Double.NaN);
            avg = Arrays.stream(x).average().orElse(Double.NaN);
            PCJ.broadcast(avg, ShareableEnum.avg);
        }
        PCJ.waitFor(ShareableEnum.avg);
//        System.out.println(PCJ.myId() + ": avg = " + PCJ.<Double>localGet(SharedEnum.avg));
        System.out.println(PCJ.myId() + ": avg = " + avg);
    }
}
