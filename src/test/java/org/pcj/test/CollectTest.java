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
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        intValue,
        doubleArray,
        string
    }

    private int intValue;
    private double[] doubleArray = new double[1];
    private String string;

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
        doubleArray[0] = PCJ.myId() + 1;
        intValue = PCJ.myId() * 10;
        string = Integer.toString(PCJ.myId());

        PCJ.barrier();

        for (int i = 0; i==0 && i < PCJ.threadCount(); ++i) {
            if (PCJ.myId() == i) {
                System.out.println("--- " + PCJ.myId() + " ---");


                List<Double> doubleList = PCJ.collect(
                        () -> Collectors.filtering(
                                (double[] v) -> ((int) v[0]) % 2 == 0,
                                Collectors.mapping((double[] v) -> (double) v[0] * v[0],
                                        Collectors.toList())),
                        Communicable.doubleArray);
                System.out.println("doubleList = " + doubleList);


                Set<Double> doubleSet = PCJ.collect(
                        () -> Collectors.mapping((Double v) -> (double) v * v,
                                Collectors.toSet()),
                        Communicable.doubleArray, 0);
                System.out.println("doubleSet = " + doubleSet);


                Map<Integer, Long> mapIntValue = PCJ.collect(
                        () -> Collectors.filtering(
                                v -> v % 2 == 0,
                                Collectors.toMap(Function.identity(), v -> (long) v * v)),
                        Communicable.intValue
                );
                System.out.println("mapIntValue = " + mapIntValue);


                String stringValue = PCJ.collect(Collectors::joining,
                        Communicable.string);
                System.out.println("stringValue = " + stringValue);
            }
            PCJ.barrier();
        }
    }
}
