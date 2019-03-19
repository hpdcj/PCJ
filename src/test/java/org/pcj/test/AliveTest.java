/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
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
import org.pcj.NodesDescription;
import org.pcj.PCJ;
import org.pcj.StartPoint;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class AliveTest implements StartPoint {

    public static void main(String[] args) {
        // -Dpcj.alive.timeout=3 -Dpcj.alive.heartbeat=1

//        Level level = Level.INFO;
        Level level = Level.CONFIG;
        Logger logger = Logger.getLogger("");
        Arrays.stream(logger.getHandlers()).forEach(handler -> handler.setLevel(level));
        logger.setLevel(level);

        NodesDescription nodesDescription = new NodesDescription(new String[]{
                "localhost:8091",
                "localhost:8002",
                "localhost:8003",
                "localhost:8004",
                "localhost:8005",
                "localhost:8006",
                "localhost:8007",
                "localhost:8008",
        });

        PCJ.deploy(AliveTest.class, nodesDescription);
    }

    @Override
    public void main() throws InterruptedException {
        if (PCJ.myId() == 0) {
            Thread.sleep(5_000);
            throw new RuntimeException("Abort");
        }
        if (PCJ.myId() == 1) {
            Thread.sleep(4_000);
            System.exit(0);
        }
        PCJ.barrier();
    }
}
