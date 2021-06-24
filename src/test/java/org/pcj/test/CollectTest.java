/*
 * Copyright (c) 2011-2021, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.test;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pcj.PCJ;
import org.pcj.RegisterStorage;
import org.pcj.StartPoint;
import org.pcj.Storage;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
@RegisterStorage(CollectTest.Communicable.class)
public class CollectTest implements StartPoint {

    @Storage(CollectTest.class)
    enum Communicable {
        intArray,
    }

    private int[] intArray = new int[1];

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

        PCJ.executionBuilder(CollectTest.class)
                .addNodes(nodes)
                .deploy();
    }

    @Override
    public void main() {
        intArray[0] = PCJ.myId() + 5;
        PCJ.barrier();

        if (PCJ.myId() == 0) {
            Map<Integer, Long> map = PCJ.collect(
                    () -> Collectors.filtering(
                            v -> v % 2 == 0,
                            Collectors.toMap(Function.identity(), v -> (long) v * v)),
                    Communicable.intArray,
                    0
            );

//        Map<Integer, Long> map = IntStream.range(0, 10)
//                .boxed()
//                .collect(Collectors.filtering(
//                        v -> v % 2 == 0,
//                        Collectors.toMap(Function.identity(), v -> (long)v * v)));
            System.out.println("map = " + map);
        }
    }
}
