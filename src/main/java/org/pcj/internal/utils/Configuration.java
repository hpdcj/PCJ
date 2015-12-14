/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration for PCJ.
 *
 * <p>
 * Configuration reads System properties.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class Configuration {

    /**
     * pcj.debug (int) default: 0 1 - print processed message 2 - print sent and broadcasted message
     * 4 - print details of processed, sent and broadcasted message (if not 1 nor 2)
     */
    final public static int DEBUG;
    /**
     * pcj.retry (int) default: 3
     */
    final public static int RETRY_COUNT;
    /**
     * pcj.retrydelay (int) default: 10
     */
    final public static int RETRY_DELAY;
    /**
     * pcj.waittime (int) default: 60
     */
    final public static int WAIT_TIME;
    /**
     * pcj.port (int) default: 8091
     */
    final public static int DEFAULT_PORT;
    /**
     * pcj.buffersize (int) default: 262144
     */
    final public static int BUFFER_SIZE;
    /**
     * pcj.nodefile (String) otherwise: NODEFILE, LOADL_HOSTFILE, PBS_NODEFILE or nodes.file
     */
    final public static String NODES_FILENAME;

    static {
        DEBUG = getPropertyInt("pcj.debug", 0/*7*/);
        RETRY_COUNT = getPropertyInt("pcj.retry", 3);
        RETRY_DELAY = getPropertyInt("pcj.retrydelay", 10);
        WAIT_TIME = getPropertyInt("pcj.waittime", 60);
        DEFAULT_PORT = getPropertyInt("pcj.port", 8091);
        BUFFER_SIZE = getPropertyInt("pcj.buffersize", 256 * 1024);
        NODES_FILENAME = getNodesFilename();
    }

    private static int getPropertyInt(String name, int defaultValue) {
        try {
            return Integer.parseInt(System.getProperty(name, String.valueOf(defaultValue)));
        } catch (NumberFormatException ex) {
            ex.printStackTrace(System.err);
        }
        return defaultValue;
    }

    private static String getNodesFilename() {
        String nodeFileName = System.getProperty("pcj.nodefile");
        if (nodeFileName == null) {
            nodeFileName = System.getenv().get("NODEFILE");
        }
        if (nodeFileName == null) {
            nodeFileName = System.getenv().get("LOADL_HOSTFILE");
        }
        if (nodeFileName == null) {
            nodeFileName = System.getenv().get("PBS_NODEFILE");
        }
        if (nodeFileName == null) {
            nodeFileName = "nodes.file";
        }
        return nodeFileName;
    }

    private static List<String> getSystemPackagesList() {
        String sysPackages = System.getProperty("pcj.syspackages");
        if (sysPackages == null) {
            return Collections.emptyList();
        }
        List<String> packages = new ArrayList<>();
        for (String p : sysPackages.split(",")) {
            p = p.trim();
            if (p.endsWith(".") == false && p.equals(p.toLowerCase())) {
                p = p + ".";
            }
            packages.add(p);
        }
        return Collections.unmodifiableList(packages);
    }
}
