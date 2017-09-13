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
 *
 * @author faramir
 */
final public class AsyncTask {

    public interface Task<T> extends Serializable {

        T call() throws Exception;
    }

    public interface VoidTask extends Task<Void> {

        void run() throws Exception;

        @Override
        public default Void call() throws Exception {
            run();
            return null;
        }

    }

}
