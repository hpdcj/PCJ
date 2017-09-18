/*
 * Copyright (c) 2017, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj;

import java.io.Serializable;

/**
 * Serializable Task to be used as functional interface with returing value.
 *
 * @param <T> type of returned value
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
@FunctionalInterface
public interface AsyncTask<T> extends Serializable {

    /**
     * Method to implement.
     *
     * @return value returned by the task
     * @throws Exception can throw any exception
     */
    T call() throws Exception;

    /**
     * Serializable Task to be used as functional interface without returning
     * value.
     */
    @FunctionalInterface
    public interface VoidTask extends AsyncTask<Void> {

        /**
         * Method to implement.
         *
         * @throws Exception can throw any exception
         */
        void run() throws Exception;

        /**
         * Default method that wraps the invokation of the <code>run()</code>
         * method.
         */
        @Override
        public default Void call() throws Exception {
            run();
            return null;
        }

    }

}
