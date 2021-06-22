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
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pcj.PCJ;
import org.pcj.PcjFuture;
import org.pcj.RegisterStorage;
import org.pcj.StartPoint;
import org.pcj.Storage;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
@RegisterStorage(GatherTest.Communicable.class)
public class GatherTest implements StartPoint {

    @Storage(GatherTest.class)
    enum Communicable {
        intArray,
        nullObject
    }

    private int[] intArray = new int[1];
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

        PCJ.executionBuilder(GatherTest.class)
                .addNodes(nodes)
                .deploy();
    }

    @Override
    public void main() {
        intArray[0] = PCJ.myId() + 1;
        PCJ.barrier();
        for (int i = 0; i < PCJ.threadCount(); ++i) {
            if (PCJ.myId() == i) {
                System.out.println("--- " + PCJ.myId() + " ---");
                Map<Integer, Integer> intSubArrayMap = PCJ.gather(Communicable.intArray, 0);
                System.out.println(intSubArrayMap);

                Map<Integer, int[]> intArrayMap = PCJ.gather(Communicable.intArray);
                System.out.println(getStringFromMap(intArrayMap, Arrays::toString));

                PcjFuture<Map<Integer, Serializable>> nullObjectsFuture = PCJ.asyncGather(Communicable.nullObject);
                System.out.println(getStringFromMap(nullObjectsFuture.get(), Objects::toString));
            }
            PCJ.barrier();
        }
    }

    private <T> String getStringFromMap(Map<Integer, T> intArrayMap, Function<T, String> toString) {
        return intArrayMap.entrySet()
                       .stream()
                       .sorted(Map.Entry.comparingByKey())
                       .map(e -> e.getKey() + "=" + toString.apply(e.getValue()))
                       .collect(Collectors.joining(", ", "{", "}"));
    }
}
