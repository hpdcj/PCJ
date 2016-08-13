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

/**
 *
 * @author faramir
 */
@Storage(SharedEnum.class)
public class StorageExample implements StartPoint {

    enum SharedEnum {
        x, avg;
    }
    int[] x; // Shared bo w Enum
    double avg; // Shared bo w Enum
    float obiekt_nie_w_Enum_wiec_nie_Shared;

    @Override
    public void main() throws Throwable {
        if (PCJ.myId() == 0) {
            x = new int[PCJ.threadCount()];
//            PCJ.putLocal(SharedEnum.x, x);
        }
        PCJ.barrier();
        PCJ.put(0, SharedEnum.x, PCJ.myId() + 1, PCJ.myId());
        PCJ.barrier();
        if (PCJ.myId() == 0) {
//            avg = Arrays.stream(PCJ.<int[]>getLocal(SharedEnum.x)).average().orElse(Double.NaN);
            avg = Arrays.stream(x).average().orElse(Double.NaN);
            PCJ.broadcast(SharedEnum.avg, avg);
        }
        PCJ.waitFor(SharedEnum.avg);
//        System.out.println(PCJ.myId() + ": avg = " + PCJ.<Double>getLocal(SharedEnum.avg));
        System.out.println(PCJ.myId() + ": avg = " + avg);
    }

    public static void main(String[] args) {
        PCJ.deploy(StorageExample.class,
                new NodesDescription(new String[]{"localhost", "localhost", "localhost"})
        );
    }
}
