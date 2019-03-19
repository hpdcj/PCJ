/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.alive;

import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pcj.PcjRuntimeException;
import org.pcj.internal.Configuration;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.Networker;
import org.pcj.internal.NodeData;
import org.pcj.internal.message.Message;

/*
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class AliveState {
    private static final Logger LOGGER = Logger.getLogger(AliveState.class.getName());
    private final ConcurrentMap<SocketChannel, LocalDateTime> nodeLastNotificationMap;
    private final ScheduledExecutorService scheduledExecutorService;
    private final AliveMessage aliveMessage;
    private final AbortMessage abortMessage;
    private volatile boolean aborted;
    private Thread mainThread;

    public AliveState() {
        nodeLastNotificationMap = new ConcurrentHashMap<>();
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

        aliveMessage = new AliveMessage();
        abortMessage = new AbortMessage();

        aborted = false;
    }

    public void start(Thread mainThread) {
        this.mainThread = mainThread;

        updateNeighbours();

        if (Configuration.ALIVE_HEARTBEAT > 0) {
            scheduledExecutorService.scheduleAtFixedRate(this::sendAliveMessage,
                    Configuration.ALIVE_HEARTBEAT, Configuration.ALIVE_HEARTBEAT, TimeUnit.SECONDS);

            if (Configuration.ALIVE_TIMEOUT > Configuration.ALIVE_HEARTBEAT) {
                scheduledExecutorService.scheduleAtFixedRate(this::checkAliveNotifyTimeout,
                        Configuration.ALIVE_TIMEOUT, Configuration.ALIVE_HEARTBEAT, TimeUnit.SECONDS);
            }
        }
    }

    private void updateNeighbours() {
        NodeData nodeData = InternalPCJ.getNodeData();
        int physicalId = nodeData.getCurrentNodePhysicalId();

        LocalDateTime now = LocalDateTime.now();

        // parent
        if (physicalId > 0) {
            nodeLastNotificationMap.put(nodeData.getSocketChannelByPhysicalId((physicalId - 1) / 2), now);
        }

        // child 1
        if (physicalId * 2 + 1 < nodeData.getTotalNodeCount()) {
            nodeLastNotificationMap.put(nodeData.getSocketChannelByPhysicalId(physicalId * 2 + 1), now);
        }

        // child 2
        if (physicalId * 2 + 2 < nodeData.getTotalNodeCount()) {
            nodeLastNotificationMap.put(nodeData.getSocketChannelByPhysicalId(physicalId * 2 + 2), now);
        }
    }

    public void stop() {
        scheduledExecutorService.shutdown();
    }

    private void checkAliveNotifyTimeout() {
        LocalDateTime now = LocalDateTime.now();
        Optional<SocketChannel> lostNotificationChannel
                = nodeLastNotificationMap.entrySet()
                          .stream()
                          .filter(entry -> ChronoUnit.SECONDS.between(entry.getValue(), now) > Configuration.ALIVE_TIMEOUT)
                          .map(Map.Entry::getKey)
                          .findAny();

        if (lostNotificationChannel.isPresent()) {
            NodeData nodeData = InternalPCJ.getNodeData();

            SocketChannel socketChannel = lostNotificationChannel.get();
            LocalDateTime lastNotification = nodeLastNotificationMap.get(socketChannel);

            int physicalId = nodeData.getPhysicalIdBySocketChannel(socketChannel);

            LOGGER.log(Level.SEVERE, "No AliveMessage from {0} in last {1} seconds.",
                    new Object[]{
                            physicalId,
                            ChronoUnit.SECONDS.between(lastNotification, now)
                    });

            nodeAborted(socketChannel);
        }
    }

    void nodeNotified(SocketChannel sender) {
        nodeLastNotificationMap.put(sender, LocalDateTime.now());
    }

    void nodeAborted(SocketChannel sender) {
        nodeLastNotificationMap.remove(sender);

        abort();
    }

    public void nodeAborted() {
        abort();
    }

    private void abort() {
        aborted = true;

        sendAbortMessage();

        mainThread.interrupt();
    }

    private void sendAliveMessage() {
        sendToNeighbours(aliveMessage);
    }

    private void sendAbortMessage() {
        sendToNeighbours(abortMessage);
    }

    private void sendToNeighbours(Message message) {
        Networker networker = InternalPCJ.getNetworker();

        for (SocketChannel socketChannel : nodeLastNotificationMap.keySet()) {
            try {
                networker.send(socketChannel, message);
            } catch (PcjRuntimeException ex) {
                int physicalId = InternalPCJ.getNodeData().getPhysicalIdBySocketChannel(socketChannel);
                LOGGER.log(Level.SEVERE, "Unable to send {0} to {1}",
                        new Object[]{message.getClass().getSimpleName(), physicalId});

                nodeAborted(socketChannel);
            }
        }
    }

    public boolean isAborted() {
        return aborted;
    }
}
