package org.pcj.internal.message.hello;

import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.pcj.internal.InternalGroup;
import org.pcj.internal.NodeInfo;
import org.pcj.internal.futures.InternalFuture;

public class HelloState {

    private final HelloFuture future;
    private final ConcurrentMap<Integer, SocketChannel> socketChannelByPhysicalId; // physicalId -> nodeInfo
    private final ConcurrentMap<Integer, NodeInfo> nodeInfoByPhysicalId; // physicalId -> nodeInfo
    private final AtomicInteger threadsLeftToConnect;
    private final AtomicInteger connectedNodeCount;

    public HelloState(int allNodesThreadCount) {
        this.future = new HelloFuture();

        this.socketChannelByPhysicalId = new ConcurrentHashMap<>();
        this.nodeInfoByPhysicalId = new ConcurrentHashMap<>();

        this.threadsLeftToConnect = new AtomicInteger(allNodesThreadCount);
        this.connectedNodeCount = new AtomicInteger(0);
    }


    public void await() throws InterruptedException {
        future.get();
    }

    public void signalDone() {
        future.signalDone();;
    }

    public int getNextPhysicalId() {
        return connectedNodeCount.incrementAndGet();
    }

    public ConcurrentMap<Integer, SocketChannel> getSocketChannelByPhysicalId() {
        return socketChannelByPhysicalId;
    }

    public ConcurrentMap<Integer, NodeInfo> getNodeInfoByPhysicalId() {
        return nodeInfoByPhysicalId;
    }

    public int decrementThreadsLeftToConnect(int threadConnected) {
        return threadsLeftToConnect.addAndGet(-threadConnected);
    }

    public static class HelloFuture extends InternalFuture<InternalGroup> {
        protected void signalDone() {
            super.signal();
        }

        private void get() throws InterruptedException {
            super.await();
        }
    }
}
