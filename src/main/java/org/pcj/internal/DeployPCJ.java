/* 
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.pcj.NodesDescription;
import org.pcj.StartPoint;

/**
 * Class used for deploying PCJ when using one of deploy methods.
 *
 * @see org.pcj.PCJ#deploy(java.lang.Class, java.lang.Class)
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class DeployPCJ {

    private static final Logger LOGGER = Logger.getLogger(DeployPCJ.class.getName());
    private final Class<? extends StartPoint> startPoint;
    private final NodeInfo node0;
    private final NodeInfo currentJvm;
    private final Collection<NodeInfo> allNodes;
    private final List<Process> processes;
    private final int allNodesThreadCount;

    private DeployPCJ(Class<? extends StartPoint> startPoint,
            NodesDescription nodesDescription) {
        this.startPoint = startPoint;

        this.node0 = nodesDescription.getNode0();
        this.currentJvm = nodesDescription.getCurrentJvm();
        this.allNodes = nodesDescription.getAllNodes();
        this.allNodesThreadCount = nodesDescription.getAllNodesThreadCount();

        processes = new ArrayList<>();
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        String startPointStr = args[0];
        int localPort = Integer.parseInt(args[1]);
        String node0Str = args[2];
        int node0Port = Integer.parseInt(args[3]);
        int allNodesThreadCount = Integer.parseInt(args[4]);
        String threadIdsStr = args[5];

        @SuppressWarnings("unchecked")
        Class<? extends StartPoint> startPoint = (Class<? extends StartPoint>) Class.forName(startPointStr);

        NodeInfo node0 = new NodeInfo(node0Str, node0Port);
        NodeInfo currentJvm = new NodeInfo("", localPort);

        String[] threadIds = threadIdsStr.substring(1, threadIdsStr.length() - 1).split(", ");
        Stream.of(threadIds).mapToInt(Integer::parseInt).forEach(id -> currentJvm.addThreadId(id));

        LOGGER.log(Level.FINE, "Invoking InternalPCJ.start({0}, {1}, {2}, {3})",
                new Object[]{startPoint, node0, currentJvm, allNodesThreadCount});

        InternalPCJ.start(startPoint, node0, currentJvm, allNodesThreadCount);
    }

    public static void deploy(Class<? extends StartPoint> startPoint,
            NodesDescription nodesDescription) {
        DeployPCJ deploy = new DeployPCJ(startPoint, nodesDescription);
        try {
            deploy.startDeploying();

            deploy.waitForFinish();
        } catch (InterruptedException | IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private void runPCJ(NodeInfo currentJvm) {
        InternalPCJ.start(startPoint, node0, currentJvm, allNodesThreadCount);
    }

    private List<String> makeJvmParams(NodeInfo node) {
        String separator = System.getProperty("file.separator");
        String path = System.getProperty("java.home") + separator + "bin" + separator + "java";

        String classpath = System.getProperty("java.class.path");

        List<String> params = new ArrayList<>(Arrays.asList(
                path, "-cp", classpath));

        RuntimeMXBean jvmOptions = ManagementFactory.getRuntimeMXBean();
        for (String jvmArgument : jvmOptions.getInputArguments()) {
            if (jvmArgument.startsWith("-Xdebug")
                    || jvmArgument.startsWith("-Xrunjdwp:transport=")
                    || jvmArgument.startsWith("-agentpath:")) {
                continue;
            }
            params.add(jvmArgument);
        }

        params.addAll(Arrays.asList(
                DeployPCJ.class.getName(),
                startPoint.getName(), // args[0]
                Integer.toString(node.getPort()), // args[1]
                node0.getHostname(), // args[2]
                Integer.toString(node0.getPort()), // args[3]
                Long.toString(allNodesThreadCount), // args[4]
                Arrays.toString(node.getThreadIds()) // args[5]
        ));
        return params;
    }

    private Process exec(List<String> exec) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(exec);

        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);

        LOGGER.log(Level.FINE, "Starting new process {0}", exec);

        Process process = processBuilder.start();
        process.getOutputStream().close();

        processes.add(process);

        return process;
    }

    private void runJVM(NodeInfo node) throws IOException {
        List<String> jvmExec = makeJvmParams(node);

        exec(jvmExec);
    }

    private void runSSH(NodeInfo node) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String arg : makeJvmParams(node)) {
            sb.append("'");
            sb.append(arg);
            sb.append("' ");
        }

        List<String> sshExec = new ArrayList<>(Arrays.asList(
                "ssh",
                node.getHostname(), // ssh host
                sb.toString().trim() // command
        ));

        exec(sshExec);
    }

    private void waitForFinish() throws InterruptedException {
        for (Process process : processes) {
            process.waitFor();
        }
    }

    private void startDeploying() throws IOException {
        for (NodeInfo node : allNodes) {
            if (node.equals(currentJvm)) {
                continue;
            }

            if (node.isLocalAddress()) {
                this.runJVM(node);
            } else {
                this.runSSH(node);
            }
        }

        if (currentJvm != null) {
            this.runPCJ(currentJvm);
        }
    }
}
