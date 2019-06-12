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
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pcj.Group;
import org.pcj.PCJ;
import org.pcj.StartPoint;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class GroupJoinTest implements StartPoint {

    public static void main(String[] args) throws InterruptedException {
        Level level = Level.INFO;
//        Level level = Level.FINEST;
        Logger logger = Logger.getLogger("");
        Arrays.stream(logger.getHandlers()).forEach(handler -> handler.setLevel(level));
        logger.setLevel(level);

        String[] nodes = {
                "localhost:8091",
//                "localhost:8091", "localhost:8091", "localhost:8091", "localhost:8091", "localhost:8091", "localhost:8091", "localhost:8091", "localhost:8091", "localhost:8091",
                "localhost:8002",
//                "localhost:8002", "localhost:8002", "localhost:8002", "localhost:8002", "localhost:8002", "localhost:8002", "localhost:8002", "localhost:8002", "localhost:8002",
                "localhost:8003",
//                "localhost:8003", "localhost:8003", "localhost:8003", "localhost:8003", "localhost:8003", "localhost:8003", "localhost:8003", "localhost:8003", "localhost:8003",
                "localhost:8004",
//                "localhost:8004", "localhost:8004", "localhost:8004", "localhost:8004", "localhost:8004", "localhost:8004", "localhost:8004", "localhost:8004",
                "localhost:8005",
//                "localhost:8005", "localhost:8005", "localhost:8005", "localhost:8005", "localhost:8005", "localhost:8005", "localhost:8005", "localhost:8005", "localhost:8005",
                "localhost:8006",
//                "localhost:8006", "localhost:8006", "localhost:8006", "localhost:8006", "localhost:8006", "localhost:8006", "localhost:8006", "localhost:8006", "localhost:8006",
                "localhost:8007",
//                "localhost:8007", "localhost:8007", "localhost:8007", "localhost:8007", "localhost:8007", "localhost:8007", "localhost:8007", "localhost:8007", "localhost:8007",
                "localhost:8008",
//                "localhost:8008", "localhost:8008", "localhost:8008", "localhost:8008", "localhost:8008", "localhost:8008", "localhost:8008", "localhost:8008", "localhost:8008",
                "localhost:8009",
//                "localhost:8009", "localhost:8009", "localhost:8009", "localhost:8009", "localhost:8009", "localhost:8009", "localhost:8009", "localhost:8009", "localhost:8009", //
                //            // run.jvmargs=-Xmx64m
//                "localhost:8010",
//                "localhost:8010", "localhost:8010", "localhost:8010", "localhost:8010", "localhost:8010", "localhost:8010", "localhost:8010", "localhost:8010", "localhost:8010",
//                "localhost:8011",
//                "localhost:8011", "localhost:8011", "localhost:8011", "localhost:8011", "localhost:8011", "localhost:8011", "localhost:8011", "localhost:8011", "localhost:8011",
//                "localhost:8012",
//                "localhost:8012", "localhost:8012", "localhost:8012", "localhost:8012", "localhost:8012", "localhost:8012", "localhost:8012", "localhost:8012", "localhost:8012",
//                "localhost:8013",
//                "localhost:8013", "localhost:8013", "localhost:8013", "localhost:8013", "localhost:8013", "localhost:8013", "localhost:8013", "localhost:8013", "localhost:8013",
//                "localhost:8014",
//                "localhost:8014", "localhost:8014", "localhost:8014", "localhost:8014", "localhost:8014", "localhost:8014", "localhost:8014", "localhost:8014", "localhost:8014",
//                "localhost:8015",
//                "localhost:8015", "localhost:8015", "localhost:8015", "localhost:8015", "localhost:8015", "localhost:8015", "localhost:8015", "localhost:8015", "localhost:8015",
//                "localhost:8016",
//                "localhost:8016", "localhost:8016", "localhost:8016", "localhost:8016", "localhost:8016", "localhost:8016", "localhost:8016", "localhost:8016", "localhost:8016",
//                "localhost:8017",
//                "localhost:8017", "localhost:8017", "localhost:8017", "localhost:8017", "localhost:8017", "localhost:8017", "localhost:8017", "localhost:8017", "localhost:8017",
//                "localhost:8018",
//                "localhost:8018", "localhost:8018", "localhost:8018", "localhost:8018", "localhost:8018", "localhost:8018", "localhost:8018", "localhost:8018", "localhost:8018",
//                "localhost:8019",
//                "localhost:8019", "localhost:8019", "localhost:8019", "localhost:8019", "localhost:8019", "localhost:8019", "localhost:8019", "localhost:8019", "localhost:8019",
        };

        PCJ.executionBuilder(GroupJoinTest.class).addNodes(nodes).deploy();
    }

    @Override
    public void main() throws Throwable {
        Thread.sleep((PCJ.getNodeCount() - PCJ.getNodeId()) * 50);

        for (int i = PCJ.threadCount() - 1; i >= 0; --i) {
            if (PCJ.myId() == i) {
                Group g = PCJ.joinGroup("temporaryGroup");
                System.err.println(PCJ.myId() + " joined to " + g.getName());
            }
            PCJ.barrier();
        }

        Group g = PCJ.joinGroup("group" + (PCJ.myId() % 2));
        PCJ.barrier();

        for (int i = 0; i < g.threadCount(); ++i) {
            if (g.myId() == i) {
                System.out.println(g.getName() + ">>> global: " + PCJ.myId() + " group:" + g.myId() + "/" + g.threadCount());
            }
            g.asyncBarrier().get();
        }
        PCJ.barrier();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 1; i <= 500; ++i) {
            Thread.sleep((long) (random.nextDouble() * 100));
            System.out.println(PCJ.myId() + "> joining to test" + i);
            Group group = PCJ.joinGroup("test" + i);
            System.out.println(PCJ.myId() + "> joined to test" + i);
            if (i % 50 == 0) {
                PCJ.barrier();
                if (group.myId() == 0) {
                    System.err.println("group '" + group.getName() + "' has " + group.threadCount() + " members");
                }
            }
        }
        PCJ.barrier();
        if (PCJ.myId() == 0) {
            for (int i = 1; i <= 500; ++i) {
                Group group = PCJ.joinGroup("test" + i);
                if (group.threadCount() != PCJ.threadCount()) {
                    System.err.println("!!! group '" + group.getName() + "' has " + group.threadCount() + " members");
                }
            }
        }
    }
}
