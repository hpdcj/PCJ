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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;
import org.pcj.internal.InternalExecutionBuilder;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public final class ExecutionBuilder extends InternalExecutionBuilder implements Cloneable {

    private static final String[] EMPTY_ARRAY = new String[0];
    private final Class<? extends StartPoint> startPoint;
    private final List<String> nodeList;
    private final Properties properties;

    /**
     * Should be created using {@link PCJ#executionBuilder(Class)}.
     *
     * @param startPoint startPoint class
     */
    ExecutionBuilder(Class<? extends StartPoint> startPoint) {
        this.startPoint = startPoint;

        this.nodeList = new ArrayList<>();
        this.properties = new Properties();
    }

    /**
     * Copy constructor for {@link #clone()) method.
     *
     * @param that source object to copy from
     */
    private ExecutionBuilder(ExecutionBuilder that) {
        this.startPoint = that.startPoint;
        this.nodeList = new ArrayList<>(that.nodeList);
        this.properties = (Properties) that.properties.clone();
    }

    /**
     * Creates a deep copy of this ExecutionBuilder.
     *
     * @return a clone of ExecutionBuilder
     */
    @Override
    public ExecutionBuilder clone() {
        return new ExecutionBuilder(this);
    }

    @Override
    public String toString() {
        return "ExecutionBuilder{startPoint=" + startPoint
                       + ", nodeList=" + nodeList + ", properties=" + properties + "}";
    }

    /**
     * Starts PCJ calculations on local node using specified StartPoint class.
     * <p>
     * List of all hostnames used in calculations should be defined earlier.
     * If none defined single thread on current JVM is used.
     */
    public void start() {
        String[] nodes;
        if (nodeList.isEmpty()) {
            nodes = new String[]{""};
        } else {
            nodes = nodeList.toArray(EMPTY_ARRAY);
        }
        super.start(startPoint, nodes, properties);
    }

    /**
     * Deploys and starts PCJ calculations on nodes using specified StartPoint class.
     * <p>
     * List of all hostnames used in calculations should be defined earlier.
     * If none defined, single thread on current JVM is used.
     */
    public void deploy() {
        String[] nodes;
        if (nodeList.isEmpty()) {
            nodes = new String[]{""};
        } else {
            nodes = nodeList.toArray(EMPTY_ARRAY);
        }
        super.deploy(startPoint, nodes, properties);
    }

    /**
     * Adds node to execution builder configuration.
     * <p>
     * Hostname can be specified many times, so more than one instance of PCJ (PCJ thread) will be run on node.
     * Empty hostnames means current JVM.
     * <p>
     * Hostnames can take port (after colon ':'), eg. <i>localhost:8000</i>.
     * Default port is 8091 and can be modified using <tt>pcj.port</tt> system property value (-Dpcj.port=8091).
     * <p>
     * Hostnames can be specified many times, so more than one instance of PCJ will be run on node (called threads).
     *
     * @param node hostname of node
     * @return a reference to this object
     */
    public ExecutionBuilder addNode(String node) {
        nodeList.add(node);
        return this;
    }

    /**
     * Adds multiple nodes to execution builder configuration.
     * <p>
     * For description of a single hostname see {@link #addNode(String)}.
     *
     * @param nodes array of hostnames
     * @return a reference to this object
     */
    public ExecutionBuilder addNodes(String[] nodes) {
        Collections.addAll(nodeList, nodes);
        return this;
    }

    /**
     * Adds multiple nodes to execution builder configuration using data from file.
     * <p>
     * For description of a single hostname see {@link #addNode(String)}.
     *
     * @param nodeFile file with list of hostnames
     * @return a reference to this object
     * @throws IOException if an I/O error occurs opening the file
     */
    public ExecutionBuilder addNodes(File nodeFile) throws IOException {
        try (Stream<String> lines = Files.lines(Paths.get(nodeFile.getPath()))) {
            lines.forEach(nodeList::add);
        }
        return this;
    }

    /**
     * Adds property to execution builder configuration, possibly replacing it.
     *
     * @param key   the key to be placed into property list.
     * @param value the value corresponding to key.
     * @return a reference to this object
     */
    public ExecutionBuilder addProperty(String key, String value) {
        properties.setProperty(key, value);
        return this;
    }

    /**
     * Removes property from execution builder configuration.
     *
     * @param key the key to be removed from property list.
     * @return a reference to this object
     */
    public ExecutionBuilder removeProperty(String key) {
        properties.remove(key);
        return this;
    }

    /**
     * Adds all properties parameters from {@link Properties} object, possibly replacing previous values.
     *
     * @param props properties to read from
     * @return a reference to this object
     */
    public ExecutionBuilder addProperties(Properties props) {
        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key);
            properties.setProperty(key, value);
        }
        return this;
    }
}
