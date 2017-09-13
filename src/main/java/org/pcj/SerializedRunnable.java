/*
 * Copyright (c) 2017, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj;

/**
 *
 * @author faramir
 */
public interface SerializedRunnable extends SerializedCallable<Void>, Runnable {

    @Override
    public default Void call() throws Exception {
        run();
        return null;
    }

}
