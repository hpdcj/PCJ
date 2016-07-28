/*
 * This file is the part of the PCJ Library
 */
package org.pcj;

/**
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
