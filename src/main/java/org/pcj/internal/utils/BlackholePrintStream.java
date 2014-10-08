/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Class that eats everything that one would like to print.
 * <p>
 * This class is especially useful for changing
 * {@link java.lang.System#out} and
 * {@link java.lang.System#err}.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class BlackholePrintStream extends PrintStream {

    public BlackholePrintStream() {
        super(new OutputStream() {
            @Override
            public void write(int b) {
            }
        });
    }

    @Override
    public void write(int b) {
    }

    @Override
    public void write(byte[] b) throws IOException {
    }

    @Override
    public void write(byte buf[], int off, int len) {
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
}
