/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Class that represents physical node (hostname, port, list
 * of ids).
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class NodeInfo {

    private String hostname;
    private int port;
    private List<Integer> localIds;
    private boolean localNode;

    public NodeInfo(String hostname, int port) {
        this(hostname, port, new ArrayList<>());
    }

    public NodeInfo(String hostname, int port, List<Integer> localIds) {
        this.hostname = hostname;
        this.port = port;
        this.localIds = localIds;
    }

    @Override
    public int hashCode() {
        return port;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof NodeInfo == false) {
            return false;
        }
        final NodeInfo other = (NodeInfo) obj;
        if (!Objects.equals(this.hostname, other.hostname)) {
            return false;
        }
        return this.port == other.port;
    }

    @Override
    public String toString() {
        return "NodeInfo[" + hostname + ":" + port + "]";
    }

    /**
     * @return the hostname
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    public List<Integer> getLocalIdsList() {
        return localIds;
    }

    public int[] getLocalIds() {
        int[] ids = new int[localIds.size()];
        int i = 0;
        for (Integer id : localIds) {
            ids[i++] = id;
        }
        return ids;
    }

    public boolean isLocalNode() {
        return localNode;
    }

    void setLocalNode(boolean localNode) {
        this.localNode = localNode;
    }
}
