/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal;

/**
 * Internal (with common ClassLoader) class for representing
 * external ResponseAttachment.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class InternalResponseAttachment implements ResponseAttachment {

    private Object response;
    private boolean done;

    public InternalResponseAttachment() {
        done = false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getObject() {
        return (T) response;
    }

    @Override
    public void setObject(Object response) {
        this.response = response;
        this.done = true;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @SuppressWarnings("unchecked")
    public <T> T waitForResponse() {
        synchronized (this) {
            while (done == false) {
                try {
                    this.wait();
                } catch (InterruptedException ex) {
                    ex.printStackTrace(System.err);
                }
            }

            return (T) response;
        }
    }
}
