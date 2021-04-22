/*
 * Copyright (c) 2011-2021, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.bye;

import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;
import org.pcj.internal.InternalFuture;
import org.pcj.internal.InternalGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.Networker;
import org.pcj.internal.NodeData;

/*
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class ByeState {

    private final ByeFuture future;
    private final AtomicInteger notificationCount;

    public ByeState(int childCount) {
        this.future = new ByeFuture();

        this.notificationCount = new AtomicInteger(childCount + 1);
    }

    public void await() throws InterruptedException {
        future.get();
    }

    public void signalDone() {
        future.signalDone();
    }

    public void nodeProcessed() {
        int leftPhysical = notificationCount.decrementAndGet();
        if (leftPhysical == 0) {
            NodeData nodeData = InternalPCJ.getNodeData();
            Networker networker = InternalPCJ.getNetworker();

            int currentPhysicalId = nodeData.getCurrentNodePhysicalId();
            if (currentPhysicalId == 0) {
                SocketChannel node0Socket = nodeData.getNode0Socket();

                ByeCompletedMessage byeCompletedMessage = new ByeCompletedMessage();
                networker.send(node0Socket, byeCompletedMessage);
            } else {
                SocketChannel parentSocketChannel = nodeData.getSocketChannelByPhysicalId((currentPhysicalId - 1) / 2);

                ByeNotifyMessage byeNotifyMessage = new ByeNotifyMessage();
                networker.send(parentSocketChannel, byeNotifyMessage);
            }
        }
    }

    public static class ByeFuture extends InternalFuture<InternalGroup> {
        protected void signalDone() {
            super.signal();
        }

        private void get() throws InterruptedException {
            super.await();
        }
    }
}
