/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.storage;

/**
 * Internal interface describing Storage methods.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public interface InternalStorage {

    /**
     * Returns variable from Storages
     *
     * @param variable name of Shared variable
     * @param indexes (optional) indexes into the array
     *
     * @return value of variable[indexes] or variable if
     * indexes omitted
     *
     * @throws ClassCastException there is more indexes than
     * variable dimension
     * @throws ArrayIndexOutOfBoundsException one of indexes
     * is out of bound
     */
    <T> T get(String variable, int... indexes) throws ClassCastException, ArrayIndexOutOfBoundsException;

    /**
     * Puts new value of variable to Storage into the array,
     * or as variable value if indexes omitted
     *
     * @param variable name of Shared variable
     * @param newValue new value of variable
     * @param indexes (optional) indexes into the array
     *
     * @throws ClassCastException there is more indexes than
     * variable dimension or value cannot be assigned to the
     * variable
     * @throws ArrayIndexOutOfBoundsException one of indexes
     * is out of bound
     */
    void put(String variable, Object newValue, int... indexes) throws ClassCastException, ArrayIndexOutOfBoundsException;

    /**
     * Tells to monitor variable
     *
     * @param variable name of Shared variable
     */
    void monitor(String variable);

    /**
     * Pauses current Thread and wait for modification of
     * variable.
     * <p>
     * The same as calling waitFor method using
     * <code>waitFor(variable, 1)</code>.
     *
     * @param variable name of Shared variable
     */
    void waitFor(String variable);

    /**
     * Pauses current Thread and wait for <code>count</code>
     * modifications of variable. At the end, decrease
     * modification count by <code>count</code>.
     *
     * @param variable name of Shared variable
     * @param count number of modifications
     */
    void waitFor(String variable, int count);

    /**
     * Gets names of all Shared variables of the Storage
     *
     * @return array with names of all Shared variables
     */
    String[] getSharedFields();

    /**
     * Checks if value can be assigned to variable stored in
     * Storage
     *
     * @param variable name of variable stored in Storage
     * @param value to check
     * @return true if the value can be assigned to the
     * variable
     */
    boolean isAssignable(String variable, Object value, int... indexes);
}
