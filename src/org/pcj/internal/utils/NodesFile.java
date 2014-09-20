/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * File representing list of available nodes (eg. from
 * sheduling system).
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class NodesFile {

    private int clientsCount;
    private NodeInfo node0;
    private Map<String, NodeInfo> nodes;

    public NodesFile(String nodeFile) throws IOException {
        List<String> nodesList = new ArrayList<>();
        try (FileReader fr = new FileReader(nodeFile);
                BufferedReader br = new BufferedReader(fr)) {
            String node;
            for (clientsCount = 0; (node = br.readLine()) != null; ++clientsCount) {
                if (node.trim().isEmpty() == false) {
                    nodesList.add(node);
                }
            }
        }

        parseArray(nodesList.toArray(new String[0]));
    }

    public NodesFile(String[] nodes) {
        parseArray(nodes);
    }

    private NodeInfo parse(String node) {
        String hostname = node;
        int port = Configuration.DEFAULT_PORT;

        try {
            int index = node.lastIndexOf(':');
            if (index != -1) {
                hostname = node.substring(0, index);
                port = Integer.parseInt(node.substring(index + 1));
            }
        } catch (NumberFormatException ex) {
            // nothing to do
        }

        return new NodeInfo(hostname, port);
    }

    private void parseArray(String[] nodesList) {
        nodes = new LinkedHashMap<>();
        clientsCount = 0;
        for (String node : nodesList) {
            if (node0 == null) {
                node0 = parse(node);
            }

            NodeInfo nodeInfo = nodes.get(node);
            if (nodeInfo == null) {
                nodeInfo = parse(node);
                if (NetworkUtils.isLocalAddress(nodeInfo.getHostname())) {
                    nodeInfo.setLocalNode(true);

                    if (Configuration.DEFAULT_PORT == nodeInfo.getPort()) {
                        node = "";
                    }
                }

                if (nodes.containsKey(node) == false) {
                    nodes.put(node, nodeInfo);
                } else {
                    nodeInfo = nodes.get(node);
                }
            }

            nodeInfo.getLocalIdsList().add(clientsCount++);
        }
    }

    public NodeInfo getNode0() {
        return node0;
    }

    public NodeInfo getLocalNode() {
        return nodes.get("");
    }

    public Collection<NodeInfo> getAllNodes() {
        return nodes.values();
    }

    public int getClientsCount() {
        return clientsCount;
    }
}
