/*
 * Copyright (c) 2011-2021, PCJ Library, Marek Nowicki
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
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pcj.PCJ;
import org.pcj.StartPoint;
import org.pcj.internal.DeployPCJ;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class DeployTest implements StartPoint {

    public static void main(String[] args) throws IOException, InterruptedException {
        Level level = Level.INFO;
//        Level level = Level.CONFIG;
        Logger logger = Logger.getLogger("");
        Arrays.stream(logger.getHandlers()).forEach(handler -> handler.setLevel(level));
        logger.setLevel(level);

        Set<Process> processes = deployPcj(
                new int[][]{{2, 4}, {1, 3, 5}}
        );

        for (Process process : processes) {
            process.waitFor();
        }
    }

    private static Set<Process> deployPcj(int[][] nodesThreadIds) throws IOException {
        String separator = System.getProperty("file.separator");
        String javaPath = System.getProperty("java.home") + separator + "bin" + separator + "java";

        int totalThreads = Arrays.stream(nodesThreadIds).mapToInt(threadIds -> threadIds.length).sum();

        Set<Process> processes = new HashSet<>();
        for (int i = 0; i < nodesThreadIds.length; ++i) {
            int[] threadIds = nodesThreadIds[i];

            ProcessBuilder processBuilder = new ProcessBuilder(
                    Arrays.asList(javaPath,
                            "-cp", System.getProperty("java.class.path"),
                            DeployPCJ.class.getName(),
                            DeployTest.class.getName(),  // args[0]: startPointClass
                            String.valueOf(8091 + i),  // args[1]: port
                            "localhost",  // args[2]: masterHostname
                            "8091", // args[3]: masterPort
                            String.valueOf(totalThreads), // args[4]: totalThreadCount
                            Arrays.toString(threadIds), // args[5]: threads
                            "\"\"")); // propertie)

            processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
            processBuilder.redirectInput(ProcessBuilder.Redirect.INHERIT);

            Process process = processBuilder.start();
            processes.add(process);
        }
        return processes;
    }

    @Override
    public void main() throws Throwable {
        System.out.println("[" + PCJ.getNodeId() + "/" + PCJ.getNodeCount() + "]" +
                                   " Hello from " + PCJ.myId() + " of " + PCJ.threadCount());
    }
}
