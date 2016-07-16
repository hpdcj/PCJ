/*
 * This file is the part of the PCJ Library
 */
package org.pcj;

import org.pcj.internal.InternalStorage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.pcj.internal.DeployPCJ;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.PcjThread;

/**
 * Main PCJ class with static methods.
 *
 * Static methods provide way to use library.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class PCJ extends InternalPCJ {

    // Suppress default constructor for noninstantiability
    private PCJ() {
        throw new AssertionError();
    }

    /**
     * Starts PCJ calculations on local node using specified StartPoint and InternalStorage class.
     * NodesDescription contains list of all hostnames used in calculations. Hostnames can be
     * specified many times, so more than one instance of PCJ will be run on node (called threads).
     *
     * @param startPoint       start point class
     * @param storage          storage class
     * @param nodesDescription description of used nodes (and threads)
     */
    public static void start(Class<? extends StartPoint> startPoint,
            Class<? extends InternalStorage> storage,
            NodesDescription nodesDescription) {
        InternalPCJ.start(startPoint, nodesDescription);
    }

    /**
     * Deploys and starts PCJ calculations on nodes using specified StartPoint and InternalStorage class.
     * NodesDescription contains list of all hostnames used in calculations.
     * Hostnames can be specified many times, so more than one instance
     * of PCJ will be run on node (called threads). Empty hostnames means current JVM.
     *
     * Hostnames can take port (after colon ':'), eg. ["localhost:8000", "localhost:8001",
     * "localhost", "host2:8001", "host2"]. Default port is 8091 and can be modified using
     * <tt>pcj.port</tt> system property value.
     *
     * @param startPoint       start point class
     * @param storage          storage class
     * @param nodesDescription description of used nodes (and threads)
     */
    public static void deploy(Class<? extends StartPoint> startPoint,
            Class<? extends InternalStorage> storage,
            NodesDescription nodesDescription) {
        DeployPCJ.deploy(startPoint, nodesDescription);
    }

    /**
     * Returns global thread id.
     *
     * @return global thread id
     */
    public static int myId() {
        return ((Group) PcjThread.getThreadGlobalGroup()).myId();
    }

    /**
     * Returns global number of threads used in calculations.
     *
     * @return global number of threads used in calculations
     */
    public static int threadCount() {
        return ((Group) PcjThread.getThreadGlobalGroup()).threadCount();
    }

    /**
     * Returns node id (internal value for distinguishing nodes).
     * Multiple PCJ threads have the same physicalId on the same JVM.
     *
     * @return physical node id
     */
    public static int getNodeId() {
        return InternalPCJ.getNodeData().getPhysicalId();
    }

    /**
     * Returns total number of nodes.
     *
     * @return total number of nodes.
     */
    public static int getNodeCount() {
        return InternalPCJ.getNodeData().getTotalNodeCount();
    }

    /**
     * Synchronizes all threads used in calculations.
     */
    public static void barrier() {
        asyncBarrier().get();
    }

    /**
     * Asynchronous barrier. Invoke get method of returned PcjFuture to wait for barrier completion.
     *
     * @return PcjFuture that can check the barrier status.
     */
    public static PcjFuture<Void> asyncBarrier() {
        return ((Group) PcjThread.getThreadGlobalGroup()).asyncBarrier();
    }

    public static void createShared(String name, Class<?> variableType) {
        PcjThread.getThreadStorage().createShared(name, variableType);
    }

    /**
     * Resets the monitoring state.
     *
     * @param variable name of variable
     */
    public static void monitor(String variable) {
        PcjThread.getThreadStorage().monitor(variable);
    }

    /**
     * Causes the current thread to wait until the variable was <i>touched</i>. Resets the state
     * after <i>touch</i>. The waitFor(variable) method has the same effect as:
     * <pre><code>waitFor(variable, 1)</code></pre>
     *
     * @param variable name of variable
     */
    public static int waitFor(String variable) {
        return waitFor(variable, 1);
    }

    /**
     * Causes the current thread to wait until the variable was <i>touched</i>
     * count times. Resets the state after <i>touches</i>.
     *
     * @param variable name of variable
     * @param count    number of <i>touches</i>
     */
    public static int waitFor(String variable, int count) {
        return PcjThread.getThreadStorage().waitFor(variable, count);
    }

    /**
     * Causes the current thread to wait until the variable was <i>touched</i>
     * count times. Resets the state after <i>touches</i>.
     *
     * @param variable name of variable
     * @param count    number of <i>touches</i>
     */
    public static int waitFor(String variable, int count,
            long timeout, TimeUnit unit) throws TimeoutException {
        return PcjThread.getThreadStorage().waitFor(variable, count, timeout, unit);
    }

    /**
     * Gets the value from current thread InternalStorage.
     *
     * @param variable name of variable
     *
     * @return value of variable
     */
    public static <T> T getLocal(String variable) {
        return PcjThread.getThreadStorage().get(variable);
    }

    /**
     * Gets the value from current thread InternalStorage
     *
     * @param variable name of array variable
     * @param indices  indices of array
     *
     * @return value of variable
     */
    public static <T> T getLocal(String variable, int... indices) {
        return PcjThread.getThreadStorage().get(variable, indices);
    }

    /**
     * Puts the value to current thread InternalStorage
     *
     * @param variable name of variable
     * @param newValue new value of variable
     *
     * @throws ClassCastException when the value cannot be cast to the type of variable in InternalStorage
     */
    public static <T> void putLocal(String variable, T newValue) throws ClassCastException {
        PcjThread.getThreadStorage().put(variable, newValue);
    }

    /**
     * Puts the value to current thread InternalStorage
     *
     * @param variable name of array variable
     * @param newValue new value of variable
     * @param indices  indices of array
     *
     * @throws ClassCastException when the value cannot be cast to the type of variable in InternalStorage
     */
    public static <T> void putLocal(String variable, T newValue, int... indices) throws ClassCastException {
        PcjThread.getThreadStorage().put(variable, newValue, indices);
    }

//    /**
//     * Fully asynchronous get from other thread Storage
//     *
//     * @param threadId global thread id
//     * @param variable name of array variable
//     *
//     * @return FutureObject that will contain received data
//     */
//    public static <T> PcjFuture<T> asyncGet(int threadId, String variable) {
//        return ((Group) PcjThread.getThreadGlobalGroup()).asyncGet(threadId, variable);
//    }
////
//    /**
//     * Fully asynchronous get from other thread Storage
//     *
//     * @param threadId global thread id
//     * @param variable name of array variable
//     * @param indices  indices of array
//     *
//     * @return FutureObject that will contain received data
//     */
//    public static <T> PcjFuture<T> getFutureObject(int threadId, String variable, int... indices) {
//        return ((Group) PcjThread.getThreadGlobalGroup()).getFutureObject(threadId, variable, indices);
//    }
//
//    public static <T> T get(int threadId, String variable) throws PcjRuntimeException{
//        PcjFuture<T> futureObject = getFutureObject(threadId, variable);
//
//        return futureObject.get();
//    }
//
//    public static <T> T get(int threadId, String variable, int... indices) throws PcjRuntimeException {
//        PcjFuture<T> futureObject = getFutureObject(threadId, variable, indices);
//
//        return futureObject.get();
//    }
//    /**
//     * Puts the value to other thread InternalStorage
//     *
//     * @param threadId other thread global thread id
//     * @param variable name of variable
//     * @param newValue new value of variable
//     *
//     * @throws ClassCastException when the value cannot be cast to the type of variable in InternalStorage
//     */
//    public static <T> PcjFuture<Void> put(int threadId, String variable, T newValue) throws ClassCastException, PcjRuntimeException {
//        if (PcjThread.threadStorage().isAssignable(variable, newValue) == false) {
//            throw new ClassCastException("Cannot cast " + newValue.getClass().getCanonicalName()
//                    + " to the type of variable '" + variable + "'");
//        }
//        return ((Group) PcjThread.getThreadGlobalGroup()).put(threadId, variable, newValue);
//    }
//
//    /**
//     * Puts the value to other thread InternalStorage
//     *
//     * @param threadId other thread global thread id
//     * @param variable name of array variable
//     * @param newValue new value of variable
//     * @param indices  indices of array
//     *
//     * @throws ClassCastException when the value cannot be cast to the type of variable in InternalStorage
//     */
//    public static <T> PcjFuture<Void> put(int threadId, String variable, T newValue, int... indices) throws ClassCastException, PcjRuntimeException {
//        if (PcjThread.threadStorage().isAssignable(variable, newValue, indices) == false) {
//            throw new ClassCastException("Cannot cast " + newValue.getClass().getCanonicalName()
//                    + " to the type of variable '" + variable + "'");
//        }
//        return ((Group) PcjThread.getThreadGlobalGroup()).put(threadId, variable, newValue, indices);
//    }
//
//    /**
//     * Broadcast the value to all threads and inserts it into InternalStorage
//     *
//     * @param variable name of variable
//     * @param newValue new value of variable
//     *
//     * @throws ClassCastException when the value cannot be cast to the type of variable in InternalStorage
//     */
//    public static PcjFuture<Void> broadcast(String variable, Object newValue) throws ClassCastException, PcjRuntimeException {
//        if (PcjThread.threadStorage().isAssignable(variable, newValue) == false) {
//            throw new ClassCastException("Cannot cast " + newValue.getClass().getCanonicalName()
//                    + " to the type of variable '" + variable + "'");
//        }
//        return ((Group) PcjThread.getThreadGlobalGroup()).broadcast(variable, newValue);
//    }
//    
//    public static PcjFuture<R> asyncAt(int threadId, Function<R,T> lambda) throws PcjRuntimeException {
//    }
//
//    /**
//     * Returns the global group
//     *
//     * @return the global group
//     */
//    public static Group getGlobalGroup() {
//        return ((Group) PcjThread.getThreadGlobalGroup());
//    }
//
//    /**
//     * Returns group by name
//     *
//     * @param name name of the group
//     *
//     * @return group by name
//     */
//    public static Group getGroup(String name) {
//        return (Group) PcjThread.threadGroup(name);
//    }
//
//    /**
//     * Joins the current thread to the group
//     *
//     * @param name name of the group
//     *
//     * @return group to which thread joined
//     */
//    public static Group join(String name) {
//        int myThreadId = ((Group) PcjThread.getThreadGlobalGroup()).myId();
//        return (Group) InternalPCJ.join(myThreadId, name);
//    }
}
