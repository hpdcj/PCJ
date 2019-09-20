/*
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * File representing list of available nodes (eg. from scheduling system).
 *
 * <b>This is only a fake class for
 * {@link PCJ#start(Class, NodesDescription)}
 * and
 * {@link PCJ#hashCode()}
 * deprecated methods.</b>
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 * @deprecated use {@link PCJ#executionBuilder(Class)} instead
 */
@Deprecated
public class NodesDescription {

    private final String[] nodes;

    /**
     * Create new nodes description using file.
     *
     * @param nodeFile name of file
     * @throws IOException I/O error
     */
    public NodesDescription(String nodeFile) throws IOException {
        this(new File(nodeFile));
    }

    /**
     * Create new nodes description using file.
     *
     * @param nodeFile file
     * @throws IOException I/O error
     */
    public NodesDescription(File nodeFile) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(nodeFile))) {
            this.nodes = br.lines()
                                 .map(String::trim)
                                 .filter(line -> !line.isEmpty())
                                 .toArray(String[]::new);
        }

    }

    /**
     * Creates new nodes description using nodes from the String array.
     *
     * @param nodes String nodes array
     */
    public NodesDescription(String[] nodes) {
        this.nodes = nodes;
    }

    protected String[] getNodes() {
        return nodes;
    }

//    /**
//     * Always throws exception.
//     *
//     * @throws UnsupportedOperationException
//     */
//    public NodeInfo getNode0() {
//        throw new UnsupportedOperationException("Deprecated. For removal.");
//    }
//
//    /**
//     * Always throws exception.
//     *
//     * @throws UnsupportedOperationException
//     */
//    public NodeInfo getCurrentJvm() {
//        throw new UnsupportedOperationException("Deprecated. For removal.");
//    }
//
//    /**
//     * Always throws exception.
//     *
//     * @throws UnsupportedOperationException
//     */
//    public Collection<NodeInfo> getAllNodes() {
//        throw new UnsupportedOperationException("Deprecated. For removal.");
//    }
//
//    /**
//     * Always throws exception.
//     *
//     * @throws UnsupportedOperationException
//     */
//    public int getAllNodesThreadCount() {
//        throw new UnsupportedOperationException("Deprecated. For removal.");
//    }
}
