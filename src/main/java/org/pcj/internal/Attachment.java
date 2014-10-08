/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal;

/**
 * Attachment is used for storing intermediate data for
 * messages that are querying for data and request a response.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public interface Attachment {

    /**
     *
     * @return <tt>true</tt> if message associated with this
     * Attachment has been processed, <tt>false</tt>
     * otherwise.
     */
    public boolean isDone();
}
