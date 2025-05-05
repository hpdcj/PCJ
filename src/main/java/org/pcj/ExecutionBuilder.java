/*
 * Copyright (c) 2011-2022, PCJ Library, Marek Nowicki
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Stream;
import org.pcj.internal.InternalExecutionBuilder;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public final class ExecutionBuilder extends InternalExecutionBuilder implements Cloneable {

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
        if (!nodeList.isEmpty()) {
            nodes = nodeList.toArray(new String[0]);
        } else {
            nodes = new String[]{null};
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
        if (!nodeList.isEmpty()) {
            nodes = nodeList.toArray(new String[0]);
        } else {
            nodes = new String[]{null};
        }
        super.deploy(startPoint, nodes, properties);
    }

    /**
     * Adds node to execution builder configuration.
     * <p>
     * Node is in form <i>hostname[:port]</i> (e.g. <i>localhost</i>, <i>localhost:8000</i>), where the port (number after colon ':'), is optional.
     * <br>
     * Default port number is 8091 ({@link org.pcj.internal.Configuration#DEFAULT_PORT}) and can be modified using {@systemProperty pcj.port} system property value.
     * <br>
     * Hostname cannot be {@code null}, empty, or contains only whitespaces.
     * <p>
     * Nodes can be specified multiple times, so more than one instance of PCJ will be run on node (called threads).
     * </p>
     *
     * @param node hostname of node
     * @return a reference to this object
     * @throws IllegalArgumentException - if the hostname is null, empty or contains only whitespaces
     */
    public ExecutionBuilder addNode(String node) {
        if (node == null) {
            throw new IllegalArgumentException("node is null");
        }

        node = node.trim();
        if (node.isEmpty() || node.lastIndexOf(":") == 0) {
            throw new IllegalArgumentException("node is empty or contains only whitespaces");
        }

        nodeList.add(node);
        return this;
    }

    /**
     * Adds multiple nodes to execution builder configuration.
     * <p>
     * Null, empty or blank entries are filtered out from the list.
     * <p>
     * For description of a single hostname see {@link #addNode(String)}.
     *
     * @param nodes array of hostnames
     * @return a reference to this object
     */
    public ExecutionBuilder addNodes(String[] nodes) {
        Arrays.stream(nodes)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(node -> !node.isEmpty())
                .forEach(this::addNode);
        return this;
    }

    /**
     * Adds multiple nodes to execution builder configuration using data from file.
     * <p>
     * Empty or blank lines are filtered out from the list.
     * <p>
     * For description of a single hostname see {@link #addNode(String)}.
     *
     * @param nodeFile file with list of hostnames
     * @return a reference to this object
     * @throws IOException if an I/O error occurs opening the file
     */
    public ExecutionBuilder addNodes(File nodeFile) throws IOException {
        try (Stream<String> lines = Files.lines(Paths.get(nodeFile.getPath()))) {
            addNodes(lines.toArray(String[]::new));
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
