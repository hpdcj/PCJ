/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.alive;

import java.util.logging.Logger;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.Networker;
import org.pcj.internal.NodeData;

/*
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class AliveState {
    private static final Logger LOGGER = Logger.getLogger(AliveState.class.getName());

    public void heartbeat() {
        NodeData nodeData = InternalPCJ.getNodeData();
        Networker networker = InternalPCJ.getNetworker();

        int physicalId = nodeData.getCurrentNodePhysicalId();
        // parent
        if (physicalId > 0) {
            networker.send(nodeData.getSocketChannelByPhysicalId((physicalId - 1) / 2), new AliveMessage());
        }
        // child 1
        if (physicalId * 2 + 1 < nodeData.getTotalNodeCount()) {
            networker.send(nodeData.getSocketChannelByPhysicalId(physicalId * 2 + 1), new AliveMessage());
        }
        // child 2
        if (physicalId * 2 + 2 < nodeData.getTotalNodeCount()) {
            networker.send(nodeData.getSocketChannelByPhysicalId(physicalId * 2 + 2), new AliveMessage());
        }
    }
}
