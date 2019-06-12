/*
 * Copyright (c) 2016, Marek Nowicki
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
@RegisterStorage(BroadcastByAllTest.Shared.class)
public class BroadcastByAllTest implements StartPoint {

    @Storage(BroadcastByAllTest.class)
    enum Shared {
        array
    }

    private int[] array = new int[PCJ.threadCount()];

    public static void main(String[] args) throws InterruptedException {
        Level level = Level.INFO;
//        Level level = Level.FINEST;
        Logger logger = Logger.getLogger("");
        Arrays.stream(logger.getHandlers()).forEach(handler -> handler.setLevel(level));
        logger.setLevel(level);

        String[] nodes = {
                "localhost",
                "localhost:8091",
                "localhost:8092",
                "localhost",};

//        PCJ.start(EasyTest.class, EasyTest.class,
        PCJ.executionBuilder(BroadcastByAllTest.class)
                .addNodes(nodes)
                .deploy();
    }

    @Override
    public void main() throws Throwable {
        PCJ.broadcast(PCJ.myId(), Shared.array, PCJ.myId());
        System.err.println(PCJ.myId() + " -> " + Arrays.toString(array));
    }
}
