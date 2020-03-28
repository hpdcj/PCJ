/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.test;

import org.pcj.PCJ;
import org.pcj.RegisterStorage;
import org.pcj.StartPoint;
import org.pcj.Storage;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
@RegisterStorage(AbortTest.Vars.class)
public class AbortTest implements StartPoint {
    @Storage(AbortTest.class)
    enum Vars {wait}

    private boolean wait;

    public static void main(String[] args) {
        Level level = Level.INFO;
//        Level level = Level.CONFIG;
        Logger logger = Logger.getLogger("");
        Arrays.stream(logger.getHandlers()).forEach(handler -> handler.setLevel(level));
        logger.setLevel(level);

        String[] nodes = {
                "localhost:8091",
                "localhost:8002",
                "localhost:8003",
                "localhost:8004",
                "localhost:8091",
                "localhost:8002",
                "localhost:8003",
                "localhost:8004",
        };

        PCJ.executionBuilder(AbortTest.class)
                .addNodes(nodes)
                .deploy();
    }

    @Override
    public void main() throws Throwable {
        if (PCJ.myId() == 2) {
            Thread.sleep(1_000);
            throw new RuntimeException("Thread-" + PCJ.myId() + ": Abort");
        }
        PCJ.at(0, () -> {
            try {
                Thread.sleep(2_000);
            } catch (Exception e) {
                System.err.println("Thread-" + PCJ.myId() + " exception: " + e);
            }
            PCJ.waitFor(Vars.wait);
        });
        try {
            Thread.sleep(2_000);
        } catch (Exception e) {
            System.err.println("Thread-" + PCJ.myId() + " exception: " + e);
        }
        PCJ.waitFor(Vars.wait);
    }
}
