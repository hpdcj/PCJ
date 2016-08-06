/* 
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj;

/**
 * Class that wraps exceptions.
 *
 * @author Marek Nowicki
 */
public class PcjRuntimeException extends RuntimeException {

    public PcjRuntimeException() {
        super();
    }

    public PcjRuntimeException(String message) {
        super(message);
    }

    public PcjRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public PcjRuntimeException(Throwable cause) {
        super(cause);
    }

    protected PcjRuntimeException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
