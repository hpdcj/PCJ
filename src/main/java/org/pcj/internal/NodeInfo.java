/* 
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Class that represents physical node (hostname, port, list
 * of ids).
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class NodeInfo implements Serializable {

    private String hostname;
    private int port;
    private SortedSet<Integer> threadIds;

    public NodeInfo(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
        this.threadIds = new ConcurrentSkipListSet<>();
    }

    public NodeInfo(String hostname, int port, int[] threadIds) {
        this(hostname, port);
        Arrays.stream(threadIds).forEach(this.threadIds::add);
    }

    @Override
    public int hashCode() {
        return hostname.hashCode() * port;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof NodeInfo)) {
            return false;
        }
        NodeInfo other = (NodeInfo) obj;
        if (!Objects.equals(this.hostname, other.hostname)) {
            return false;
        }
        return this.port == other.port;
    }

    @Override
    public String toString() {
        return "NodeInfo[" + threadIds.toString() + "@" + hostname + ":" + port + "]";
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public int[] getThreadIds() {
        return threadIds.stream().mapToInt(Integer::intValue).toArray();
    }

    public void addThreadId(int i) {
        threadIds.add(i);
    }

    public boolean isLocalAddress() {
        try {
            InetAddress ia = InetAddress.getByName(hostname);

            if (ia.isAnyLocalAddress() || ia.isLoopbackAddress()) {
                return true;
            }

            return NetworkInterface.getByInetAddress(ia) != null;
        } catch (UnknownHostException | SocketException ex) {
            return false;
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int length = in.readInt();
        if (length < 0) {
            hostname = null;
        } else {
            byte[] b = new byte[length];
            in.read(b);
            hostname = new String(b, StandardCharsets.UTF_8);
        }
        port = in.readInt();

        threadIds = new ConcurrentSkipListSet<>();

        int threadId;
        while ((threadId = in.readInt()) != -1) {
            threadIds.add(threadId);
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        if (hostname == null) {
            out.writeInt(-1);
        } else {
            byte[] b = hostname.getBytes(StandardCharsets.UTF_8);
            out.writeInt(b.length);
            out.write(b);
        }
        out.writeInt(port);
        for (Integer threadId : threadIds) {
            out.writeInt(threadId);
        }
        out.writeInt(-1);
    }

}
