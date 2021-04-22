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
import org.pcj.Group;
import org.pcj.PCJ;
import org.pcj.StartPoint;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class SplitGroupTest implements StartPoint {

    public static void main(String[] args) throws InterruptedException {
        Level level = Level.INFO;
//        Level level = Level.FINEST;
        Logger logger = Logger.getLogger("");
        Arrays.stream(logger.getHandlers()).forEach(handler -> handler.setLevel(level));
        logger.setLevel(level);

        String[] nodes = {
                "localhost:8091",
                "localhost:8091", "localhost:8091",
                "localhost:8002",
                "localhost:8003",
                "localhost:8004",
                "localhost:8005",
                "localhost:8006",
                "localhost:8007",
                "localhost:8008",
                "localhost:8009",
        };

        PCJ.executionBuilder(SplitGroupTest.class)
                .addNodes(nodes)
                .deploy();
    }

    @Override
    public void main() throws Throwable {
//        if (PCJ.myId() == 0) {
//            System.out.println("***** no join *****");
//        }
//        PCJ.barrier();
//
//        Group gNull = PCJ.splitGroup(null, 0);
//        for (int i = 0; i < PCJ.threadCount(); ++i) {
//            if (PCJ.myId() == i) {
//                System.out.println(">>> global: " + PCJ.myId() + ", group: " + gNull);
//            }
//            PCJ.barrier();
//        }


//        PCJ.barrier();
//        if (PCJ.myId() == 0) {
//            System.out.println("***** odd/even *****");
//        }
//        PCJ.barrier();
//
//        Group gOddEven = PCJ.splitGroup((PCJ.myId() % 2), 0);
//        for (int i = 0; i < gOddEven.threadCount(); ++i) {
//            if (gOddEven.myId() == i) {
//                System.out.println(">>> global: " + PCJ.myId() + ", group: " + gOddEven.myId() + "/" + gOddEven.threadCount());
//            }
//            gOddEven.asyncBarrier().get();
//        }


//        PCJ.barrier();
//        if (PCJ.myId() == 0) {
//            System.out.println("***** odd/even in subgroups *****");
//        }
//        PCJ.barrier();
//
//        Group gSubGroup = gOddEven.asyncSplitGroup(gOddEven.myId() % 2, 0).get();
//        for (int i = 0; i < gSubGroup.threadCount(); ++i) {
//            if (gSubGroup.myId() == i) {
//                System.out.println(">>> global: " + PCJ.myId() + ", oddEven: " + gOddEven.myId() + ", group: " + gSubGroup.myId() + "/" + gSubGroup.threadCount());
//            }
//            gSubGroup.asyncBarrier().get();
//        }


        PCJ.barrier();
        if (PCJ.myId() == 0) {
            System.out.println("***** odd/even reverse order *****");
        }
        PCJ.barrier();

        Group gOddEvenReversed = PCJ.splitGroup((PCJ.myId() % 2), PCJ.threadCount() - PCJ.myId());
        System.out.println(PCJ.getNodeId() + " gOddEvenReversed received");
        for (int i = 0; i < gOddEvenReversed.threadCount(); ++i) {
            if (gOddEvenReversed.myId() == i) {
                System.out.println(">>> MINE " + PCJ.myId() + "(" + gOddEvenReversed.myId() + ")/" + gOddEvenReversed.threadCount());
            } else {
                System.out.println("OTHER " + i + " " + PCJ.myId() + "(" + gOddEvenReversed.myId() + ")/" + gOddEvenReversed.threadCount());
            }
            gOddEvenReversed.asyncBarrier().get();
        }
        PCJ.barrier();
        System.out.println("FINISHED " + PCJ.myId() + "(" + gOddEvenReversed.myId()+")");
    }
}
