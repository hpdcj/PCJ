/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.utils;

/**
 * Configuration for PCJ.
 * 
 * <p>Configuration reads System properties.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class Configuration {

    /**
     * pcj.debug (int) default: 0 1 - print processed message
     * 2 - print sent and broadcasted message 4 - print
     * details of processed, sent and broadcasted message (if
     * not 1 nor 2)
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
     * pcj.redirect.out (boolean, 1=true, otherwise false)
     * default: 1
     */
    final public static boolean REDIRECT_OUT;
    /**
     * pcj.redirect.err (boolean, 1=true, otherwise false)
     * default: 1
     */
    final public static boolean REDIRECT_ERR;
    /**
     * pcj.redirect.node0 (boolean, 1, true, otherwise false)
     * default: 0
     */
    final public static boolean REDIRECT_NODE0;
    /**
     * pcj.nodefile (String) otherwise: NODEFILE,
     * LOADL_HOSTFILE, PBS_NODEFILE or nodes.file
     */
    final public static String NODES_FILENAME;

    static {
        DEBUG = getPropertyInt("pcj.debug", 0/*7*/);
        RETRY_COUNT = getPropertyInt("pcj.retry", 3);
        RETRY_DELAY = getPropertyInt("pcj.retrydelay", 10);
        WAIT_TIME = getPropertyInt("pcj.waittime", 60);
        DEFAULT_PORT = getPropertyInt("pcj.port", 8091);
        BUFFER_SIZE = getPropertyInt("pcj.buffersize", 256 * 1024);
        REDIRECT_OUT = getPropertyInt("pcj.redirect.out", 1) == 1;
        REDIRECT_ERR = getPropertyInt("pcj.redirect.err", 1) == 1;
        REDIRECT_NODE0 = getPropertyInt("pcj.redirect.node0", 0) == 1;
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
}
