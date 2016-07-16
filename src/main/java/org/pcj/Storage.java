/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author faramir
 */
public interface Storage {
    
    void createShared(String name, Class<?> type) throws NullPointerException, IllegalArgumentException, IllegalStateException;

    /**
     * Returns variable from Storages
     *
     * @param name    name of Shared variable
     * @param indices (optional) indices into the array
     *
     * @return value of variable[indices] or variable if indices omitted
     *
     * @throws ClassCastException             there is more indices than variable dimension
     * @throws ArrayIndexOutOfBoundsException one of indices is out of bound
     */
    @SuppressWarnings(value = "unchecked")
    <T> T get(String name, int... indices) throws ArrayIndexOutOfBoundsException, ClassCastException;

    /**
     * Tells to monitor variable. Set the variable modification counter to zero.
     *
     * @param variable name of Shared variable
     */
    void monitor(String variable);

    /**
     * Puts new value of variable to Storage into the array, or as variable
     * value if indices omitted
     *
     * @param name     name of Shared variable
     * @param newValue new value of variable
     * @param indices  (optional) indices into the array
     *
     * @throws ClassCastException             there is more indices than variable dimension
     *                                        or value cannot be assigned to the variable
     * @throws ArrayIndexOutOfBoundsException one of indices is out of bound
     */
    <T> void put(String name, T value, int... indices) throws ArrayIndexOutOfBoundsException, ClassCastException, NullPointerException;

    /**
     * Pauses current Thread and wait for <code>count</code> modifications of
     * variable. After modification decreases the variable modification counter by
     * <code>count</code>.
     *
     * @param variable name of Shared variable
     * @param count    number of modifications. If 0 - the method exits immediately.
     *
     *
     */
    int waitFor(String variable, int count);

    /**
     * Pauses current Thread and wait for <code>count</code> modifications of
     * variable. After modification decreases the variable modification counter by
     * <code>count</code>.
     *
     * @param variable name of Shared variable
     * @param count    number of modifications. If 0 - the method exits immediately.
     */
    int waitFor(String variable, int count, long timeout, TimeUnit unit) throws TimeoutException;
    
}
