/*
 * Copyright (c) 2019, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.test;


import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.pcj.NodesDescription;
import org.pcj.PCJ;
import org.pcj.PcjFuture;
import org.pcj.RegisterStorage;
import org.pcj.StartPoint;
import org.pcj.Storage;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
@RegisterStorage(PcjMicroBenchmarkReduce.Vars.class)
public class PcjMicroBenchmarkReduce implements StartPoint {

    @Storage(PcjMicroBenchmarkReduce.class)
    enum Vars {
        value,
        valueArray,
        treeArray
    }

    private double value;
    private double[] valueArray = PCJ.myId() == 0 ? new double[PCJ.threadCount()] : null;
    private double[] treeArray = new double[2];
    private List<Random> expectedRandomList = null;

    public static void main(String[] args) throws IOException {
        NodesDescription nodesDescription;
        if (args.length > 0) {
            nodesDescription = new NodesDescription(args[0]);
        } else {
            nodesDescription = new NodesDescription(new String[]{
                    "localhost",
                    "localhost",
                    "localhost",
                    "localhost",
                    "localhost",
                    "localhost",
                    "localhost",
                    "localhost",
                    "localhost",
                    "localhost",
            });
        }

        PCJ.deploy(PcjMicroBenchmarkReduce.class, nodesDescription);
    }

    private double calculateExpectedValue() {
        if (PCJ.myId() == 0) {
            return expectedRandomList.stream()
                           .mapToDouble(Random::nextDouble)
                           .sum();
        }
        return 0;
    }

    private List<Random> prepareRandomList() {
        if (PCJ.myId() == 0) {
            return IntStream.range(0, PCJ.threadCount())
                           .mapToObj(Random::new)
                           .collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public void main() {
        Locale.setDefault(Locale.ENGLISH);
        expectedRandomList = prepareRandomList();

        Random r = new Random(PCJ.myId());

        long pcjGetTime = Long.MAX_VALUE;
        long pcjAsyncGetTime = Long.MAX_VALUE;
        long pcjPutTime = Long.MAX_VALUE;
        long pcjTreePutTime = Long.MAX_VALUE;
        long pcjCollectTime = Long.MAX_VALUE;
        long pcjReduceTime = Long.MAX_VALUE;

        for (int i = 0; i < 200; ++i) {
            double expectedValue = calculateExpectedValue();
            value = r.nextDouble();
            PCJ.barrier();

            pcjGetTime = Math.min(pcjGetTime, checkTime("pcjGet", this::pcjGet, expectedValue));

            pcjAsyncGetTime = Math.min(pcjAsyncGetTime, checkTime("pcjAsyncGet", this::pcjAsyncGet, expectedValue));

            pcjPutTime = Math.min(pcjPutTime, checkTime("pcjPut", this::pcjPut, expectedValue));

            pcjTreePutTime = Math.min(pcjTreePutTime, checkTime("pcjTreePut", this::pcjTreePut, expectedValue));

            pcjCollectTime = Math.min(pcjCollectTime, checkTime("pcjCollect", this::pcjCollect, expectedValue));

            pcjReduceTime = Math.min(pcjReduceTime, checkTime("pcjReduce", this::pcjReduce, expectedValue));

            PCJ.barrier();
        }
        if (PCJ.myId() == 0) {
            System.out.printf("pcjGet =      %.7f%n", pcjGetTime / 1e9);
            System.out.printf("pcjAsyncGet = %.7f%n", pcjAsyncGetTime / 1e9);
            System.out.printf("pcjPut =      %.7f%n", pcjPutTime / 1e9);
            System.out.printf("pcjTreePut =  %.7f%n", pcjTreePutTime / 1e9);
            System.out.printf("pcjCollect =  %.7f%n", pcjCollectTime / 1e9);
            System.out.printf("pcjReduce =   %.7f%n", pcjReduceTime / 1e9);
        }
    }


    private double pcjGet() {
        if (PCJ.myId() == 0) {
            double sum = 0.0;
            for (int i = 0; i < PCJ.threadCount(); i++) {
                sum += PCJ.<Double>get(i, Vars.value);
            }
            return sum;
        }
        return 0;
    }

    private double pcjAsyncGet() {
        if (PCJ.myId() == 0) {
            Set<PcjFuture<Double>> futures = new HashSet<>();
            for (int i = 0; i < PCJ.threadCount(); i++) {
                futures.add(PCJ.asyncGet(i, Vars.value));
            }
            return futures.stream().mapToDouble(PcjFuture::get).sum();
        }
        return 0;
    }

    private double pcjPut() {
        PCJ.put(value, 0, Vars.valueArray, PCJ.myId());
        if (PCJ.myId() == 0) {
            PCJ.waitFor(Vars.valueArray, PCJ.threadCount());

            return Arrays.stream(valueArray).sum();
        }
        return 0;
    }

    private double pcjTreePut() {
        int childCount = Math.min(2, Math.max(0, PCJ.threadCount() - PCJ.myId() * 2 - 1));
        PCJ.waitFor(Vars.treeArray, childCount);
        if (PCJ.myId() > 0) {
            double v = value + Arrays.stream(treeArray).limit(childCount).sum();
            PCJ.put(v, (PCJ.myId() - 1) / 2, Vars.treeArray, (PCJ.myId() + 1) % 2);
        }
        if (PCJ.myId() == 0) {
            return value + Arrays.stream(treeArray).limit(childCount).sum();
        }
        return 0;
    }

    private double pcjCollect() {
        if (PCJ.myId() == 0) {
            double[] values = PCJ.collect(Vars.value);
            return Arrays.stream(values).sum();
        }
        return 0;
    }

    private double pcjReduce() {
        if (PCJ.myId() == 0) {
            return PCJ.reduce(Double::sum, Vars.value);
        }
        return 0;
    }

    private long checkTime(String name, Supplier<Double> method, double expectedValue) {
        double value = Double.NaN;

        for (Vars var : Vars.values()) {
            PCJ.monitor(var);
        }
        PCJ.barrier();

        long start = System.nanoTime();
        for (int i = 0; i < 100; ++i) {
            value = method.get();
        }
        long elapsed = System.nanoTime() - start;

        if (PCJ.myId() == 0 && !equalsDouble(expectedValue, value)) {
            System.err.println("Verification failed: " + value + " != " + expectedValue + " for " + name);
        }
        return elapsed;
    }

    private static boolean equalsDouble(double d1, double d2) {
        return Math.abs(d1 - d2) / Math.max(Math.abs(d1), Math.abs(d2)) < 1e-8;
    }
}
