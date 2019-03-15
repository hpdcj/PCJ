/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.alive;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.Networker;
import org.pcj.internal.NodeData;

/*
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class AliveState {
    private static final Logger LOGGER = Logger.getLogger(AliveState.class.getName());
    private final AtomicBoolean nodeFailureOccurredAtomic;
    private final ConcurrentMap<Integer, LocalDateTime> nodeLastNotificationMap;

    public AliveState() {
        nodeFailureOccurredAtomic = new AtomicBoolean(false);
        nodeLastNotificationMap = new ConcurrentHashMap<>();
    }

    public void heartbeat() {
        // check nodeLastNotificationMap for last notification
        if (!nodeFailureOccurredAtomic.get()) {
            LocalDateTime now = LocalDateTime.now();
            boolean lostNotification = nodeLastNotificationMap.values()
                                               .stream()
                                               .map(lastNotification -> ChronoUnit.SECONDS.between(lastNotification, now))
                                               .filter(seconds -> seconds > 60)
                                               .map(seconds -> true)
                                               .findAny()
                                               .orElse(false);

            updateNodeFailureOccurred(lostNotification);
        }

        // send
        NodeData nodeData = InternalPCJ.getNodeData();
        Networker networker = InternalPCJ.getNetworker();

        int physicalId = nodeData.getCurrentNodePhysicalId();
        AliveMessage aliveMessage = new AliveMessage(physicalId, nodeFailureOccurredAtomic.get());
        // parent
        if (physicalId > 0) {
            networker.send(nodeData.getSocketChannelByPhysicalId((physicalId - 1) / 2), aliveMessage);
        }
        // child 1
        if (physicalId * 2 + 1 < nodeData.getTotalNodeCount()) {
            networker.send(nodeData.getSocketChannelByPhysicalId(physicalId * 2 + 1), aliveMessage);
        }
        // child 2
        if (physicalId * 2 + 2 < nodeData.getTotalNodeCount()) {
            networker.send(nodeData.getSocketChannelByPhysicalId(physicalId * 2 + 2), aliveMessage);
        }
    }

    public void updateNodeFailureOccurred(boolean nodeFailureOccurred) {
        boolean expected;
        do {
            expected = nodeFailureOccurredAtomic.get();
        } while (!nodeFailureOccurredAtomic.compareAndSet(expected, expected | nodeFailureOccurred));
    }

    public void updateNotificationTime(int physicalId) {
        LocalDateTime localDateTime = LocalDateTime.now();
        nodeLastNotificationMap.put(physicalId, localDateTime);
    }
}
