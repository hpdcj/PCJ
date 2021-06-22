/*
 * Copyright (c) 2011-2021, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.test;

import java.io.Serializable;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import org.pcj.PCJ;
import org.pcj.RegisterStorage;
import org.pcj.StartPoint;
import org.pcj.Storage;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
@RegisterStorage(ScatterTest.Communicable.class)
public class ScatterTest implements StartPoint {

    @Storage(ScatterTest.class)
    enum Communicable {
        intArray,
        intValue,
        nullObject
    }

    private int[] intArray = new int[1];
    private int intValue;
    private Serializable nullObject;

    public static void main(String[] args) {
        Level level = Level.INFO;
//        Level level = Level.FINEST;
        Logger logger = Logger.getLogger("");
        Arrays.stream(logger.getHandlers()).forEach(handler -> handler.setLevel(level));
        logger.setLevel(level);

        String[] nodes = {
                "localhost:8091",
                "localhost:8002",
                "localhost:8003",
                "localhost:8004",
                "localhost:8005",
                "localhost:8006",
                "localhost:8007",
                "localhost:8008",
        };

        PCJ.executionBuilder(ScatterTest.class)
                .addNodes(nodes)
                .deploy();
    }

    @Override
    public void main() {
        for (int threadId = 0; threadId < PCJ.threadCount(); ++threadId) {
            // value
            if (PCJ.myId() == threadId) {
                System.out.println("--- " + PCJ.myId() + " ---");

                int[] array = IntStream.range(1, PCJ.threadCount() + 1).toArray();
                PCJ.scatter(array, Communicable.intValue);
            }
            for (int i = 0; i < PCJ.threadCount(); ++i) {
                if (PCJ.myId() == i) {
                    PCJ.waitFor(Communicable.intValue);
                    System.out.println(PCJ.myId() + " -> " + intValue);
                }
                PCJ.barrier();
            }
            PCJ.barrier();

            // array
            if (PCJ.myId() == threadId) {
                int[] array = IntStream.range(100 * threadId + 1, 100 * threadId + 1 + PCJ.threadCount()).toArray();
                PCJ.scatter(array, Communicable.intArray, 0);
            }
            for (int i = 0; i < PCJ.threadCount(); ++i) {
                if (PCJ.myId() == i) {
                    PCJ.waitFor(Communicable.intArray);
                    System.out.println(PCJ.myId() + " -> " + Arrays.toString(intArray));
                }
                PCJ.barrier();
            }
            PCJ.barrier();

            // null
            if (PCJ.myId() == threadId) {
                Serializable[] array = new Serializable[PCJ.threadCount()];
                PCJ.scatter(array, Communicable.nullObject);
            }
            for (int i = 0; i < PCJ.threadCount(); ++i) {
                if (PCJ.myId() == i) {
                    PCJ.waitFor(Communicable.nullObject);
                    System.out.println(PCJ.myId() + " -> " + nullObject);
                }
                PCJ.barrier();
            }
            PCJ.barrier();
        }
    }
}