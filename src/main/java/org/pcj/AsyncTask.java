/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * Serializable Task to be used as functional interface with returing value.
 *
 * @param <T> type of returned value
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
@FunctionalInterface
public interface AsyncTask<T> extends Callable<T>, Serializable {

    /**
     * Serializable Task to be used as functional interface without returning
     * value.
     */
    @FunctionalInterface
    interface VoidTask extends AsyncTask<Void> {

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
        default Void call() throws Exception {
            run();
            return null;
        }
    }

}
