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
import java.util.stream.Collector;
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

        for (int i = 0; i < PCJ.threadCount(); ++i) {
            if (PCJ.myId() == i) {
                System.out.println("--- " + PCJ.myId() + " ---");


                List<Double> doubleList = PCJ.asyncCollect(
                        () -> Collectors.filtering(
                                (double[] v) -> ((int) v[0]) % 2 == 0,
                                Collectors.mapping((double[] v) -> v[0] * v[0],
                                        Collectors.toList())),
                        Communicable.doubleArray).get();
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


                double average = PCJ.collect(
                        () -> Collectors.averagingDouble(v -> (double) (int) v),
                        Communicable.intValue
                );
                System.out.println("average = " + average);


                String stringValue = PCJ.collect(Collectors::joining,
                        Communicable.string);
                System.out.println("stringValue = " + stringValue);


                String joiningString = PCJ.collect(() -> Collector.of(StringBuilder::new,
                        (StringBuilder a, String b) -> {
                            if (a.isEmpty()) a.append(b);
                            else a.append(", ").append(b);
                        },
                        (StringBuilder a, StringBuilder b) -> {
                            if (a.isEmpty()) return a.append(b);
                            else return a.append(", ").append(b);
                        },
                        (StringBuilder a) -> "{" + a.toString() + "}"),
                        Communicable.string);
                System.out.println("joiningString = " + joiningString);
            }
            PCJ.barrier();
        }
    }
}
