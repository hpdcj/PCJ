/*
 * Copyright (c) 2016-2021, Marek Nowicki
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
import org.pcj.test.SizesTest.SharedEnum;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
@RegisterStorage(SharedEnum.class)
public class SizesTest implements StartPoint {

    @Storage(SizesTest.class)
    enum SharedEnum {
        b
    }

    byte[] b;

    public static void main(String[] args) throws InterruptedException {
        Level level = Level.INFO;
//        Level level = Level.FINEST;
        Logger logger = Logger.getLogger("");
        Arrays.stream(logger.getHandlers()).forEach(handler -> handler.setLevel(level));
        logger.setLevel(level);

        String[] nodes = {
                "localhost:8091",
                "localhost:8002",};
//        NodesDescription nodesDescription = new NodesDescription(nodes);

        PCJ.executionBuilder(SizesTest.class)
                .addNodes(nodes)
                .deploy();
    }

    @Override
    public void main() throws Throwable {
        for (int n = 0; n <= 80000; ++n) {
            System.out.println("n=" + n);
            byte[] bytes = new byte[n];
            if (PCJ.myId() == 0) {
                PCJ.put(bytes, 1, SharedEnum.b);
            }
            PCJ.barrier();
        }
        System.out.println("DONE");
    }
}
