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
import java.util.concurrent.ThreadLocalRandom;
import org.pcj.PCJ;
import org.pcj.RegisterStorage;
import org.pcj.StartPoint;
import org.pcj.Storage;

/**
 * @author faramir
 */
@RegisterStorage
public class StorageSimpleExample implements StartPoint {

    @Storage
    enum ShareableEnum {
        array, avg;
    }

    int[] array = PCJ.myId() == 0 ? new int[PCJ.threadCount()] : null;
    double avg;

    @Override
    public void main() throws Throwable {
        PCJ.put(PCJ.myId() + 42, 0, ShareableEnum.array, PCJ.myId());

        if (PCJ.myId() == 0) {
            PCJ.waitFor(ShareableEnum.array, PCJ.threadCount());
            System.out.println(Arrays.toString(array));
            avg = Arrays.stream(array).average().orElse(Double.NaN);
            PCJ.broadcast(avg, ShareableEnum.avg);
        }

        PCJ.waitFor(ShareableEnum.avg);
        System.out.println(PCJ.myId() + ": avg = " + avg);
    }

    public static void main(String[] args) {
        PCJ.executionBuilder(StorageSimpleExample.class)
                .start();
    }
}
