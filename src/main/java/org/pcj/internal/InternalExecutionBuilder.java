/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.pcj.PcjRuntimeException;
import org.pcj.StartPoint;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public abstract class InternalExecutionBuilder {

    private static final String CURRENT_JVM_HOSTNAME = "";

    protected InternalExecutionBuilder() {
    }

    protected void start(Class<? extends StartPoint> startPoint, String[] nodes, Properties props) {
        InternalPCJ.setConfiguration(new Configuration(props));

        Map<String, NodeInfo> nodesMap = parseArray(nodes);

        NodeInfo currentJvm = nodesMap.get(CURRENT_JVM_HOSTNAME);

        NodeInfo node0 = nodesMap.values().stream()
                                 .filter(nodeInfo -> nodeInfo.getThreadIds().contains(0))
                                 .findFirst()
                                 .orElseThrow(() -> new PcjRuntimeException("Cannot find node0"));

        int allNodesThreadCount = nodesMap.values().stream()
                                          .map(NodeInfo::getThreadIds)
                                          .mapToInt(Set::size)
                                          .sum();

        InternalPCJ.start(startPoint, node0, currentJvm, allNodesThreadCount);
    }

    protected void deploy(Class<? extends StartPoint> startPoint, String[] nodes, Properties props) {
        InternalPCJ.setConfiguration(new Configuration(props));

        Map<String, NodeInfo> nodesMap = parseArray(nodes);

        NodeInfo currentJvm = nodesMap.get(CURRENT_JVM_HOSTNAME);

        NodeInfo node0 = nodesMap.values().stream()
                                 .filter(nodeInfo -> nodeInfo.getThreadIds().contains(0))
                                 .findFirst()
                                 .orElseThrow(() -> new PcjRuntimeException("Cannot find node0"));

        Collection<NodeInfo> allNodes = nodesMap.values();

        DeployPCJ.deploy(startPoint, node0, currentJvm, allNodes, props);
    }

    private Map<String, NodeInfo> parseArray(String[] nodesList) {
        if (nodesList == null || nodesList.length == 0) {
            throw new IllegalArgumentException("nodesList is null or empty");
        }

        int defaultPort = InternalPCJ.getConfiguration().DEFAULT_PORT;

        Map<String, NodeInfo> nodesMap = new LinkedHashMap<>();
        int clientsCount = 0;
        for (String nodeName : nodesList) {
            NodeInfo nodeInfo = nodesMap.get(nodeName);
            if (nodeInfo == null) {
                nodeInfo = parseNodeString(nodeName, defaultPort);

                if (nodeInfo.getPort() == defaultPort && nodeInfo.isLocalAddress()) {
                    nodeName = CURRENT_JVM_HOSTNAME;
                }

                if (!nodesMap.containsKey(nodeName)) {
                    nodesMap.put(nodeName, nodeInfo);
                } else {
                    nodeInfo = nodesMap.get(nodeName);
                }
            }

            nodeInfo.addThreadId(clientsCount++);
        }
        return nodesMap;
    }

    private NodeInfo parseNodeString(String node, int defaultPort) {
        if (node == null) {
            return new NodeInfo(null, defaultPort);
        }

        String hostname;
        int port;
        try {
            URI uri = new URI("pcj://" + node);

            hostname = uri.getHost();
            port = uri.getPort();
        } catch (URISyntaxException e) {
            hostname = null;
            port = -1;
        }

        if (hostname == null) {
            throw new IllegalArgumentException("Invalid node name: '" + node + "'");
        }

        if (port == -1) {
            port = defaultPort;
        }

        return new NodeInfo(hostname, port);
    }
}