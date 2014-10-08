/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal;

/**
 * Interface for representing attachment as being attached.
 *
 * It differs from {@link org.pcj.internal.Attachment} in that
 * way it contains some response data.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public interface ResponseAttachment extends Attachment {

    public <T> T getObject();

    public void setObject(Object response);
}
