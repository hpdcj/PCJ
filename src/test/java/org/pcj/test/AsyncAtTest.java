/*
 * Copyright (c) 2019, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pcj.NodesDescription;
import org.pcj.PCJ;
import org.pcj.PcjFuture;
import org.pcj.PcjRuntimeException;
import org.pcj.RegisterStorage;
import org.pcj.StartPoint;
import org.pcj.Storage;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
@RegisterStorage(AsyncAtTest.Shared.class)
public class AsyncAtTest implements StartPoint {

    @Storage(AsyncAtTest.class)
    enum Shared {
        v
    }

    private int v = -1;

    public static void main(String[] args) throws InterruptedException {
        System.setProperty("pcj.async.workers.count", "3");
        System.setProperty("pcj.async.workers.keepalive", "1");
        System.setProperty("pcj.async.workers.queuesize", "-1");

        Level level = Level.INFO;
//        Level level = Level.CONFIG;
//        Level level = Level.FINEST;
        Logger logger = Logger.getLogger("");
        Arrays.stream(logger.getHandlers()).forEach(handler -> handler.setLevel(level));
        logger.setLevel(level);

        NodesDescription nodesDescription = new NodesDescription(new String[]{
                "localhost",
                "localhost:8092",});

//        PCJ.start(EasyTest.class, EasyTest.class,
        PCJ.deploy(AsyncAtTest.class, nodesDescription);
    }

    @Override
    public void main() throws Throwable {
        if (PCJ.myId() == 1) {
            Set<PcjFuture<?>> futures = new HashSet<>();

            for (int i = 1; i <= 20; ++i) {
                futures.add(PCJ.asyncAt(0, () -> {
                    System.out.println("Hello World from " + Thread.currentThread().getName());
                    Thread.sleep(500);
                }));
                if (i % 3 == 0 || i % 4 == 0) {
                    Thread.sleep(600);
                }
            }
            futures.forEach(future -> {
                try {
                    future.get();
                } catch (PcjRuntimeException ex) {
                    System.out.println("Exception: " + ex.getMessage() + " -> " + Arrays.stream(ex.getSuppressed()).map(Throwable::getMessage).collect(Collectors.joining(", ")));
                }
            });


        }
        if (PCJ.myId() == 0) {
            try {
                PCJ.asyncAt(1, () -> {
                    PCJ.putLocal(PCJ.myId(), Shared.v);
                    // v = PCJ.myId(); // throws java.io.NotSerializableException: org.pcj.test.ExecuteAsyncAtTest
                    System.out.println("output: " + PCJ.myId());
                    throw new RuntimeException("rzucony wyjatek");
                }).get();
            } catch (PcjRuntimeException ex) {
                System.err.println(ex.toString() + ":\n"
                                           + Arrays.stream(ex.getSuppressed())
                                                     .map(e -> "\t * " + e.toString())
                                                     .collect(Collectors.joining("\n")));
            }
        }
        PCJ.barrier();
        System.out.println("v = " + v);
    }
}
