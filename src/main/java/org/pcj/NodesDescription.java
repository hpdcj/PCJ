/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj;

import java.io.File;
import java.io.IOException;
import org.pcj.internal.InternalNodesDescription;

/**
 * File representing list of available nodes (eg. from sheduling system).
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class NodesDescription extends InternalNodesDescription {

    /**
     * Create new nodes description using file.
     *
     * @param nodeFile name of file
     * @throws IOException I/O error
     */
    public NodesDescription(String nodeFile) throws IOException {
        super(new File(nodeFile));
    }

    /**
     * Create new nodes description using file.
     *
     * @param nodeFile file
     * @throws IOException I/O error
     */
    public NodesDescription(File nodeFile) throws IOException {
        super(nodeFile);
    }

    /**
     * Creates new nodes description using nodes from the String array.
     *
     * @param nodes String nodes array
     */
    public NodesDescription(String[] nodes) {
        super(nodes);
    }

    /**
     * Checks if current JVM is node0.
     *
     * @return true if current JVM is node0
     */
    public boolean isCurrentJvmNode0() {
        return super.getNode0().equals(super.getCurrentJvm());
    }
}
