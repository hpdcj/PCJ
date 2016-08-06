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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.pcj.internal.Configuration;
import org.pcj.internal.NodeInfo;

/**
 * File representing list of available nodes (eg. from sheduling system).
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class NodesDescription {

    private NodeInfo node0;
    private Map<String, NodeInfo> nodesMap;

    /**
     * Create new nodes description using file.
     *
     * @param nodeFile name of file
     *
     * @throws IOException I/O error
     */
    public NodesDescription(String nodeFile) throws IOException {
        this(new File(nodeFile));
    }

    /**
     * Create new nodes description using file.
     *
     * @param nodeFile file
     *
     * @throws IOException I/O error
     */
    public NodesDescription(File nodeFile) throws IOException {
        String[] nodes;
        try (BufferedReader br = new BufferedReader(new FileReader(nodeFile))) {
            nodes = br.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .toArray(String[]::new);
        }
        parseArray(nodes);
    }

    /**
     * Creates new nodes description using nodes from the String array.
     *
     * @param nodes String nodes array
     */
    public NodesDescription(String[] nodes) {
        parseArray(nodes);
    }

    private void parseArray(String[] nodesList) {
        if (nodesList == null || nodesList.length < 1) {
            throw new IllegalArgumentException("nodesList is null or empty");
        }

        if (node0 == null) {
            node0 = parseNodeString(nodesList[0]);
        }

        nodesMap = new LinkedHashMap<>();
        int clientsCount = 0;
        for (String node : nodesList) {
            NodeInfo nodeInfo = nodesMap.get(node);
            if (nodeInfo == null) {
                nodeInfo = parseNodeString(node);
                if (nodeInfo.getPort() == Configuration.DEFAULT_PORT
                        && nodeInfo.isLocalAddress()) {
                    node = "";
                }

                if (nodesMap.containsKey(node) == false) {
                    nodesMap.put(node, nodeInfo);
                } else {
                    nodeInfo = nodesMap.get(node);
                }
            }

            nodeInfo.addThreadId(clientsCount++);
        }
    }

    private NodeInfo parseNodeString(String node) {
        String hostname = node;
        int port = Configuration.DEFAULT_PORT;

        int index = node.lastIndexOf(':');
        if (index != -1) {
            hostname = node.substring(0, index);

            try {
                port = Integer.parseInt(node.substring(index + 1));
            } catch (NumberFormatException ex) {
                /* nothing to do, port already set */
            }
        }

        return new NodeInfo(hostname, port);
    }

    /**
     * Returns information about node0.
     *
     * @return node0
     */
    public NodeInfo getNode0() {
        return node0;
    }

    /**
     * Returns information about current node (current JVM).
     *
     * @return current node
     */
    public NodeInfo getCurrentJvm() {
        return nodesMap.get("");
    }

    /**
     * Returns informatino about all nodes.
     *
     * @return all nodes
     */
    public Collection<NodeInfo> getAllNodes() {
        return Collections.unmodifiableCollection(nodesMap.values());
    }

    /**
     * Returns all thread count in the nodes description.
     *
     * @return thread count
     */
    public int getAllNodesThreadCount() {
        return nodesMap.values().stream()
                .mapToInt(nodeInfo -> nodeInfo.getThreadIds().length)
                .sum();
    }
}
