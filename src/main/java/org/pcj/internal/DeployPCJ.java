/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.pcj.internal.storage.InternalStorage;
import org.pcj.internal.utils.NodeInfo;

/**
 * Class used for deploying PCJ when using one of deploy methods.
 *
 * @see org.pcj.PCJ#deploy(java.lang.Class, java.lang.Class)
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class DeployPCJ {

    final private NodeInfo node0;
    final private Integer clientsCount;
    final private Class<? extends InternalStartPoint> startPoint;
    final private Class<? extends InternalStorage> storage;
    final private List<Process> processes;
    private int processCount;

    private static class ProcessReader implements Runnable {

        private final BufferedReader reader;
        private final int processNo;
        private final boolean err;

        public ProcessReader(int count, BufferedReader reader) {
            this(count, reader, false);
        }

        public ProcessReader(int count, BufferedReader reader, boolean err) {
            this.reader = reader;
            this.processNo = count;
            this.err = err;
        }

        @Override
        public void run() {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (err) {
                        System.err.printf("[%d] %s\n", processNo, line);
                    } else {
                        System.out.printf("[%d] %s\n", processNo, line);
                    }
                }
                reader.close();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public static void main(String[] args) throws ClassNotFoundException, IOException {
        @SuppressWarnings("unchecked")
        Class<? extends InternalStartPoint> startPoint = (Class<? extends InternalStartPoint>) Class.forName(args[0]);
        @SuppressWarnings("unchecked")
        Class<? extends InternalStorage> storage = (Class<? extends InternalStorage>) Class.forName(args[1]);
        int clientsCount = Integer.valueOf(args[2]);
        String[] strIds = args[3].substring(1, args[3].length() - 1).split(", ");
        List<Integer> ids = new ArrayList<>(strIds.length);
        for (int i = 0; i < strIds.length; ++i) {
            ids.add(Integer.valueOf(strIds[i]));
        }

        NodeInfo node0 = new NodeInfo(args[5], Integer.valueOf(args[6]));
        NodeInfo localNode = new NodeInfo("", Integer.valueOf(args[4]), ids);

        InternalPCJ.start(startPoint, storage, node0, localNode, clientsCount);
    }

    DeployPCJ(NodeInfo node0, Integer clientsCount, Class<? extends InternalStartPoint> startPoint, Class<? extends InternalStorage> storage) {
        this.node0 = node0;
        this.clientsCount = clientsCount;
        this.startPoint = startPoint;
        this.storage = storage;

        processCount = 0;
        processes = new ArrayList<>();
    }

    void runPCJ(final NodeInfo localNode) {
        try {
            InternalPCJ.start(startPoint, storage, node0, localNode, clientsCount);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        ++processCount;
    }

    private List<String> makeJvmParams(NodeInfo node) {
//        String separator = System.getProperty("file.separator");
//        String path = System.getProperty("java.home") + separator + "bin" + separator + "java";
        String path = "java";

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
                storage.getName(), // args[1]
                Integer.toString(clientsCount), //args[2]
                Arrays.toString(node.getLocalIds()), // args[3]
                Integer.toString(node.getPort()), // args[4]
                node0.getHostname(), // args[5]
                Integer.toString(node0.getPort()) // args[6]
        ));
        return params;
    }

    private Process exec(List<String> exec) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(exec);
        //processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        processes.add(process);
        process.getOutputStream().close();
        new Thread(new ProcessReader(processCount, new BufferedReader(new InputStreamReader(process.getInputStream()))), "stdout:" + processCount).start();
        new Thread(new ProcessReader(processCount, new BufferedReader(new InputStreamReader(process.getErrorStream())), true), "stderr:" + processCount).start();

        return process;
    }

    void runJVM(NodeInfo node) throws IOException {
        List<String> jvmExec = makeJvmParams(node);

        exec(jvmExec);

        ++processCount;
    }

    void runSSH(NodeInfo node) throws IOException {
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

        ++processCount;
    }

    void waitFor() throws InterruptedException {
        for (Process process : processes) {
            process.waitFor();
        }
    }
}
