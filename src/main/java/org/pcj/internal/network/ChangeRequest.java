/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.network;

import java.nio.channels.SelectableChannel;

/**
 * Helper class for changing socket interest in Selector.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
class ChangeRequest {

    private final SelectableChannel socket;
    private final ChangeRequestType type;
    private final int ops;

    public ChangeRequest(SelectableChannel socket, ChangeRequestType type, int ops) {
        this.socket = socket;
        this.type = type;
        this.ops = ops;
    }

    /**
     * @return the socket
     */
    public SelectableChannel getSocket() {
        return socket;
    }

    /**
     * @return the type
     */
    public ChangeRequestType getType() {
        return type;
    }

    /**
     * @return the ops
     */
    public int getOps() {
        return ops;
    }
}
