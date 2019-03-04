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
public class HelloTest implements StartPoint {

    public static void main(String[] args) {
        Level level = Level.INFO;
//        Level level = Level.FINEST;
        Logger logger = Logger.getLogger("");
        Arrays.stream(logger.getHandlers()).forEach(handler -> handler.setLevel(level));
        logger.setLevel(level);

        NodesDescription nodesDescription = new NodesDescription(new String[]{
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
//                "localhost:8009",
//                "localhost:8009", "localhost:8009", "localhost:8009", "localhost:8009", "localhost:8009", "localhost:8009", "localhost:8009", "localhost:8009", "localhost:8009", //
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
        });

        long[] time = new long[10];
        for (int i = 0; i < time.length; i++) {
            System.err.println("--- iteration " + (i + 1) + " of " + time.length + " ---");

            long start = System.nanoTime();
            PCJ.deploy(HelloTest.class, nodesDescription);
            time[i] = (System.nanoTime() - start);
        }
        System.out.println("t[]= \t" + Arrays.toString(Arrays.stream(time).mapToDouble(t -> t / 1e9).toArray()));
        System.out.println("min: \t" + Arrays.stream(time).min().getAsLong() / 1e9);
        System.out.println("max: \t" + Arrays.stream(time).max().getAsLong() / 1e9);
        System.out.println("avg: \t" + Arrays.stream(time).average().getAsDouble() / 1e9);
        System.out.println("median:\t" + Arrays.stream(time).sorted().skip((time.length - 1) / 2).limit(time.length % 2 == 1 ? 1 : 2).average().getAsDouble() / 1e9);
    }

    @Override
    public void main() {
        if (PCJ.myId() == 0) {
            System.out.println("nodeCount: " + PCJ.getNodeCount());
        }
    }
}
