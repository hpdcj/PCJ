/*
 * This file is the part of the PCJ Library
 */
package org.pcj;

import java.io.IOException;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.InternalStartPoint;
import org.pcj.internal.PcjThread;
import org.pcj.internal.storage.InternalStorage;
import org.pcj.internal.utils.Configuration;
import org.pcj.internal.utils.NodesFile;

/**
 * Main PCJ class with static methods.
 * 
 * Static methods provide way to use library.
 * 
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class PCJ extends org.pcj.internal.InternalPCJ {

    // Suppress default constructor for noninstantiability
    private PCJ() {
        throw new AssertionError();
    }

    /**
     * Deploys and starts PCJ calculations on nodes using specified StartPoint
     * and Storage class. Array <tt>nodes</tt> contains list of hostnames.
     * Hostnames can be specified many times, so more than one instance of PCJ
     * will be run on node. Empty hostnames means current JVM.
     *
     * Hostnames can take port (after colon ':'), eg. ["localhost:8000",
     * "localhost:8001", "localhost", "host2:8001", "host2"]. Default port is
     * 8091 and can be modified using <tt>pcj.port</tt> system property value.
     *
     * @param startPoint start point class
     * @param storage storage class
     * @param nodes array of nodes
     */
    public static void deploy(Class<? extends InternalStartPoint> startPoint,
            Class<? extends InternalStorage> storage,
            String[] nodes) {
        NodesFile nodesFile = new NodesFile(nodes);
        InternalPCJ.deploy(startPoint, storage, nodesFile);
    }

    /**
     * Deploys and starts PCJ calculations on nodes using specified StartPoint
     * and Storage class.
     *
     * @param startPoint start point class
     * @param storage storage class
     * @param nodesFilename file with descriptions of nodes
     */
    public static void deploy(Class<? extends InternalStartPoint> startPoint,
            Class<? extends InternalStorage> storage,
            String nodesFilename) {
        try {
            NodesFile nodesFile = new NodesFile(nodesFilename);
            InternalPCJ.deploy(startPoint, storage, nodesFile);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Deploys and starts PCJ calculations on nodes using specified StartPoint
     * and Storage class. Descriptions of nodes are read from default nodefile
     * according to the system settings:
     * <ol><li>pcj.nodefile - JVM property</li>
     * <li>NODEFILE - system property</li>
     * <li>LOADL_HOSTFILE - system property</li>
     * <li>PBS_NODEFILE - system property</li>
     * <li>or <i>"nodes.file"</i></li></ol>
     *
     * @param startPoint start point class
     * @param storage storage class
     */
    public static void deploy(Class<? extends InternalStartPoint> startPoint,
            Class<? extends InternalStorage> storage) {
        deploy(startPoint, storage, Configuration.NODES_FILENAME);
    }

    /**
     * Starts PCJ calculations on local node using specified StartPoint and
     * Storage class. Array <tt>nodes</tt> contains list of all hostnames used
     * in calculations.
     *
     * @param startPoint start point class
     * @param storage storage class
     * @param nodes array of nodes
     */
    public static void start(Class<? extends InternalStartPoint> startPoint,
            Class<? extends InternalStorage> storage,
            String[] nodes) {
        NodesFile nodesFile = new NodesFile(nodes);
        InternalPCJ.start(startPoint, storage, nodesFile);
    }

    /**
     * Starts PCJ calculations on local node using specified StartPoint and
     * Storage class. Descriptions of all nodes used in calculations are read
     * from supplied file.
     *
     * @param startPoint start point class
     * @param storage storage class
     * @param nodesFilename file with descriptions of nodes
     */
    public static void start(Class<? extends InternalStartPoint> startPoint,
            Class<? extends InternalStorage> storage,
            String nodesFilename) {
        try {
            NodesFile nodesFile = new NodesFile(nodesFilename);
            InternalPCJ.start(startPoint, storage, nodesFile);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Starts PCJ calculations on local node using specified StartPoint and
     * Storage class. Descriptions of all nodes used in calculations are read
     * from default nodefile according to the system settings:
     * <ol><li>pcj.nodefile - JVM property</li>
     * <li>NODEFILE - system property</li>
     * <li>LOADL_HOSTFILE - system property</li>
     * <li>PBS_NODEFILE - system property</li>
     * <li>or <i>"nodes.file"</i></li></ol>
     *
     * @param startPoint start point class
     * @param storage storage class
     */
    public static void start(Class<? extends InternalStartPoint> startPoint,
            Class<? extends InternalStorage> storage) {
        start(startPoint, storage, Configuration.NODES_FILENAME);
    }

//    @SuppressWarnings("unchecked")
//    public static <X extends Storage> X getStorage() {
//        return (X) PcjThread.threadStorage();
//    }
    /**
     * Returns global node id.
     *
     * @return global node id
     */
    public static int myId() {
        return ((Group) PcjThread.threadGlobalGroup()).myId();
    }

    /**
     * Returns physical node id (internal value for distinguishing nodes).
     * 
     * @return physical node id
     */
    public static int getPhysicalNodeId() {
        return InternalPCJ.getPhysicalNodeId();
    }
    
    /**
     * Returns global number of nodes used in calculations.
     *
     * @return global number of nodes used in calculations
     */
    public static int threadCount() {
        return ((Group) PcjThread.threadGlobalGroup()).threadCount();
    }

    /**
     * Synchronizes all nodes used in calculations.
     */
    public static void barrier() {
        ((Group) PcjThread.threadGlobalGroup()).barrier();
    }

    public static void barrier(int node) {
        ((Group) PcjThread.threadGlobalGroup()).barrier(node);
    }

    /**
     * Resets the monitoring state.
     *
     * @param variable name of variable
     */
    public static void monitor(String variable) {
        PcjThread.threadStorage().monitor(variable);
    }

    /**
     * Causes the current thread to wait until the variable was <i>touched</i>.
     * Resets the state after <i>touch</i>. The waitFor(variable) method has the
     * same effect as:
     * <pre><code>waitFor(variable, 1)</code></pre>
     *
     * @param variable name of variable
     */
    public static void waitFor(String variable) {
        PcjThread.threadStorage().waitFor(variable);
    }

    /**
     * Causes the current thread to wait until the variable was <i>touched</i>
     * count times. Resets the state after <i>touches</i>.
     *
     * @param variable name of variable
     * @param count number of <i>touches</i>
     */
    public static void waitFor(String variable, int count) {
        PcjThread.threadStorage().waitFor(variable, count);
    }

    /**
     * Gets the value from current thread Storage.
     *
     * @param variable name of variable
     * @return value of variable
     */
    public static <T> T getLocal(String variable) {
        return PcjThread.threadStorage().get(variable);
    }

    /**
     * Gets the value from current thread Storage
     *
     * @param variable name of array variable
     * @param indexes indexes of array
     * @return value of variable
     */
    public static <T> T getLocal(String variable, int... indexes) {
        return PcjThread.threadStorage().get(variable, indexes);
    }

    /**
     * Fully asynchronous get from other thread Storage
     *
     * @param nodeId global node id
     * @param variable name of array variable
     * @return FutureObject that will contain received data
     */
    public static <T> FutureObject<T> getFutureObject(int nodeId, String variable) {
        return ((Group) PcjThread.threadGlobalGroup()).getFutureObject(nodeId, variable);
    }

    /**
     * Fully asynchronous get from other thread Storage
     *
     * @param nodeId global node id
     * @param variable name of array variable
     * @param indexes indexes of array
     * @return FutureObject that will contain received data
     */
    public static <T> FutureObject<T> getFutureObject(int nodeId, String variable, int... indexes) {
        return ((Group) PcjThread.threadGlobalGroup()).getFutureObject(nodeId, variable, indexes);
    }

    public static <T> T get(int nodeId, String variable) {
        FutureObject<T> futureObject = getFutureObject(nodeId, variable);

        return futureObject.get();
    }

    public static <T> T get(int nodeId, String variable, int... indexes) {
        FutureObject<T> futureObject = getFutureObject(nodeId, variable, indexes);

        return futureObject.get();
    }

    /**
     * Puts the value to current thread Storage
     *
     * @param variable name of variable
     * @param newValue new value of variable
     * @throws ClassCastException when the value cannot be cast to the type of
     * variable in Storage
     */
    public static void putLocal(String variable, Object newValue) throws ClassCastException {
        PcjThread.threadStorage().put(variable, newValue);
    }

    /**
     * Puts the value to current thread Storage
     *
     * @param variable name of array variable
     * @param newValue new value of variable
     * @param indexes indexes of array
     * @throws ClassCastException when the value cannot be cast to the type of
     * variable in Storage
     */
    public static void putLocal(String variable, Object newValue, int... indexes) throws ClassCastException {
        PcjThread.threadStorage().put(variable, newValue, indexes);
    }

    /**
     * Puts the value to other thread Storage
     *
     * @param nodeId other node global node id
     * @param variable name of variable
     * @param newValue new value of variable
     * @throws ClassCastException when the value cannot be cast to the type of
     * variable in Storage
     */
    public static <T> void put(int nodeId, String variable, T newValue) throws ClassCastException {
        if (PcjThread.threadStorage().isAssignable(variable, newValue) == false) {
            throw new ClassCastException("Cannot cast " + newValue.getClass().getCanonicalName()
                    + " to the type of variable '" + variable + "'");
        }
        ((Group) PcjThread.threadGlobalGroup()).put(nodeId, variable, newValue);
    }

    /**
     * Puts the value to other thread Storage
     *
     * @param nodeId other node global node id
     * @param variable name of array variable
     * @param newValue new value of variable
     * @param indexes indexes of array
     * @throws ClassCastException when the value cannot be cast to the type of
     * variable in Storage
     */
    public static <T> void put(int nodeId, String variable, T newValue, int... indexes) throws ClassCastException {
        if (PcjThread.threadStorage().isAssignable(variable, newValue, indexes) == false) {
            throw new ClassCastException("Cannot cast " + newValue.getClass().getCanonicalName()
                    + " to the type of variable '" + variable + "'");
        }
        ((Group) PcjThread.threadGlobalGroup()).put(nodeId, variable, newValue, indexes);
    }

    /**
     * Broadcast the value to all threads and inserts it into Storage
     *
     * @param variable name of variable
     * @param newValue new value of variable
     * @throws ClassCastException when the value cannot be cast to the type of
     * variable in Storage
     */
    public static void broadcast(String variable, Object newValue) throws ClassCastException {
        if (PcjThread.threadStorage().isAssignable(variable, newValue) == false) {
            throw new ClassCastException("Cannot cast " + newValue.getClass().getCanonicalName()
                    + " to the type of variable '" + variable + "'");
        }
        ((Group) PcjThread.threadGlobalGroup()).broadcast(variable, newValue);
    }

    /**
     * Returns the global group
     *
     * @return the global group
     */
    public static Group getGlobalGroup() {
        return ((Group) PcjThread.threadGlobalGroup());
    }

    /**
     * Returns group by name
     *
     * @param name name of the group
     * @return group by name
     */
    public static Group getGroup(String name) {
        return (Group) PcjThread.threadGroup(name);
    }

    /**
     * Joins the current thread to the group
     *
     * @param name name of the group
     * @return group to which thread joined
     */
    public static Group join(String name) {
        int myNodeId = ((Group) PcjThread.threadGlobalGroup()).myId();
        return (Group) InternalPCJ.join(myNodeId, name);
    }

    /**
     * Sends message with log message
     *
     * @param text text to send
     */
    public static void log(String text) {
        ((Group) PcjThread.threadGlobalGroup()).log(text);
    }
}
