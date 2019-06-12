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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pcj.PCJ;
import org.pcj.RegisterStorage;
import org.pcj.StartPoint;
import org.pcj.Storage;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
@RegisterStorage(AccumulateTest.Shared.class)
public class AccumulateTest implements StartPoint {

    @Storage(AccumulateTest.class)
    enum Shared {
        v
    }

    private int v = 0;

    public static void main(String[] args) throws InterruptedException {
        Level level = Level.INFO;
//        Level level = Level.CONFIG;
//        Level level = Level.FINEST;
        Logger logger = Logger.getLogger("");
        Arrays.stream(logger.getHandlers()).forEach(handler -> handler.setLevel(level));
        logger.setLevel(level);

        String[] nodes = {
                "localhost",
                "localhost:8092",};

//        PCJ.start(EasyTest.class, EasyTest.class,
        PCJ.executionBuilder(AccumulateTest.class)
                .addNodes(nodes)
                .deploy();
    }

    @Override
    public void main() throws Throwable {
        PCJ.accumulate(Integer::sum, PCJ.myId() + 1, 0, Shared.v);
        PCJ.barrier();
        System.out.println(PCJ.myId() + "> v = " + v);
    }
}
