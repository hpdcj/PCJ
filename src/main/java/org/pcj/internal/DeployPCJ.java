/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.pcj.StartPoint;

/**
 * Class used for deploying PCJ when using one of deploy methods.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public final class DeployPCJ {

    private static final Logger LOGGER = Logger.getLogger(DeployPCJ.class.getName());
    private final Class<? extends StartPoint> startPoint;
    private final NodeInfo node0;
    private final NodeInfo currentJvm;
    private final Collection<NodeInfo> allNodes;
    private final List<Process> processes;
    private final Properties properties;
    private final int allNodesThreadCount;

    private DeployPCJ(Class<? extends StartPoint> startPoint,
                      NodeInfo node0,
                      NodeInfo currentJvm,
                      Collection<NodeInfo> allNodes,
                      Properties props) {
        this.startPoint = startPoint;
        this.node0 = node0;
        this.currentJvm = currentJvm;
        this.allNodes = allNodes;
        this.properties = props;

        this.allNodesThreadCount = allNodes.stream()
                                           .map(NodeInfo::getThreadIds)
                                           .mapToInt(Set::size)
                                           .sum();

        this.processes = new ArrayList<>();
    }

    public static void main(String[] args) throws ClassNotFoundException {
        String startPointStr = args[0];
        int localPort = Integer.parseInt(args[1]);
        String node0Str = args[2];
        int node0Port = Integer.parseInt(args[3]);
        int allNodesThreadCount = Integer.parseInt(args[4]);
        String threadIdsStr = args[5];
        Properties props = new Properties();
        try {
            if (args.length >= 7) {
                props.load(new StringReader(args[6]));
            } else {
                LOGGER.log(Level.FINE, "Properties not set");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Unable to parse properties", e);
        }

        InternalPCJ.setConfiguration(new Configuration(props));

        @SuppressWarnings("unchecked")
        Class<? extends StartPoint> startPoint = (Class<? extends StartPoint>) Class.forName(startPointStr);

        NodeInfo node0 = new NodeInfo(node0Str, node0Port);
        NodeInfo currentJvm = new NodeInfo("", localPort);

        String[] threadIds = threadIdsStr.substring(1, threadIdsStr.length() - 1).split(", ");
        Stream.of(threadIds).mapToInt(Integer::parseInt).forEach(currentJvm::addThreadId);

        LOGGER.log(Level.FINE, "Invoking InternalPCJ.start({0}, {1}, {2}, {3})",
                new Object[]{startPoint, node0, currentJvm, allNodesThreadCount});

        InternalPCJ.start(startPoint, node0, currentJvm, allNodesThreadCount);
    }

    public static void deploy(Class<? extends StartPoint> startPoint,
                              NodeInfo node0,
                              NodeInfo currentJvm,
                              Collection<NodeInfo> allNodes,
                              Properties props) {
        DeployPCJ deploy = new DeployPCJ(startPoint, node0, currentJvm, allNodes, props);
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

        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        for (String jvmArgument : runtimeMXBean.getInputArguments()) {
            if (jvmArgument.startsWith("-Xdebug")
                        || jvmArgument.startsWith("-Xrunjdwp:transport=")
                        || jvmArgument.startsWith("-agentpath:")
                        || jvmArgument.startsWith("-agentlib:")
                        || jvmArgument.startsWith("-javaagent:")
            ) {
                continue;
            }
            params.add(jvmArgument);
        }

        String propertiesString = "";
        try (StringWriter sw = new StringWriter()) {
            properties.store(sw, null);
            propertiesString = sw.toString();

            // remove comments from properties, eg. with timestamp
            propertiesString = propertiesString.replaceAll("^#.*\\R", "");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Unable to write properties", e);
        }

        params.addAll(Arrays.asList(
                DeployPCJ.class.getName(),
                startPoint.getName(), // args[0]
                Integer.toString(node.getPort()), // args[1]
                node0.getHostname(), // args[2]
                Integer.toString(node0.getPort()), // args[3]
                Long.toString(allNodesThreadCount), // args[4]
                node.getThreadIds().toString(), // args[5]
                propertiesString // args[6]
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

        return process;
    }

    private void runJVM(NodeInfo node) throws IOException {
        List<String> jvmExec = makeJvmParams(node);

        processes.add(exec(jvmExec));
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

        processes.add(exec(sshExec));
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
