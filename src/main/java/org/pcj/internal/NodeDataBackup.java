/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal;

import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import org.pcj.internal.message.MessageHello;
import org.pcj.internal.network.LoopbackSocketChannel;

/**
 * 
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final class NodeDataBackup {

    /* for node0: */
    int physicalNodesCount;
    int clientsConnected;
    int helloCompletedCount;
    final Map<String, Integer> groupsIds; // key - groupName, value - groupId
    final Map<Integer, Integer> groupsMaster; // key - groupId, value - physicalId
    final Map<Integer, MessageHello> helloMessages;
    /* clients/nodes */
    final int[] localIds;
    /* connecting info */
    int physicalId;
    final Map<Integer, SocketChannel> physicalNodes;
    final Map<SocketChannel, Integer> physicalNodesIds;
    //final BlockingQueue<Message> pendingMessages;
    final Map<Integer, Integer> virtualNodes; // key = nodeId, value physicalId
    /* messages */
    final Map<Integer, Future<?>> attachmentMap; // key = messageId, all messages has different id, even from different local virtualNodes
    /* groups */
    final InternalGroup internalGlobalGroup;
    final ConcurrentMap<Integer, InternalGroup> internalGroupsById; // key - groupId, value - group
    final ConcurrentMap<String, InternalGroup> internalGroupsByName; // key - groupName, value - group
    /* virtualNodes sync */
    final ConcurrentMap<NodesSyncData, NodesSyncData> nodesSyncData;
    final ConcurrentMap<Integer, PcjThreadData> localData;
//    final ConcurrentMap<PcjThreadPair, PcjThreadPair> syncNodePair; // key=nodeId, value=0,1,...??
    /* finish */
    final Object finishObject;

    public NodeDataBackup() {
        this.groupsIds = null;
        this.groupsMaster = null;
        this.helloMessages = null;
        this.localIds = null;
        this.physicalNodes = null;
        this.physicalNodesIds = null;
        this.virtualNodes = null;
        this.attachmentMap = null;
        this.internalGlobalGroup = null;
        this.internalGroupsById = null;
        this.internalGroupsByName = null;
        this.nodesSyncData = null;
        this.localData = null;
        this.finishObject = null;
    }

//    
//    
//    public NodeData(int[] localIds, ConcurrentMap<Integer, PcjThreadData> localData, InternalGroup globalGroup) {
//        this(localIds, localData, globalGroup, null);
//    }
//
//    public NodeData(int[] localIds, ConcurrentMap<Integer, PcjThreadData> localData, InternalGroup globalGroup, Integer clientsCount) {
//        if (clientsCount == null) { // as normal
//            this.clientsCount = -1;
//            this.clientsConnected = -1;
//            physicalNodesCount = -1;
//
//            groupsMaster = null;
//            groupsIds = null;
//
//            helloMessages = null;
//        } else { // as manager -> clientsCount == null
//            this.clientsCount = clientsCount;
//            this.clientsConnected = 0;
//            physicalNodesCount = 0;
//
//            groupsIds = new ConcurrentHashMap<>();
//            groupsIds.put("", 0);
//
//            groupsMaster = new ConcurrentHashMap<>();
//            groupsMaster.put(0, 0);
//
//            helloMessages = Collections.synchronizedSortedMap(new TreeMap<Integer, MessageHello>());
//        }
//        this.localIds = localIds;
//        this.localData = localData;
//
//        physicalId = -1;
//
//        internalGlobalGroup = globalGroup;
//        internalGroupsById = new ConcurrentHashMap<>();
//        internalGroupsByName = new ConcurrentHashMap<>();
//        addGroup(internalGlobalGroup);
//
//        //pendingMessages = new LinkedBlockingQueue<>();
//
//        virtualNodes = new ConcurrentHashMap<>();
//        physicalNodes = new ConcurrentHashMap<>();
//        physicalNodesIds = new ConcurrentHashMap<>();
//
//        nodesSyncData = new ConcurrentHashMap<>();
////        syncNodePair = new ConcurrentHashMap<>();
//
//        attachmentMap = new ConcurrentHashMap<>();
//
//        finishObject = new Object();
//    }

    void addGroup(InternalGroup group) {
        synchronized (internalGroupsById) {
//            internalGroupsById.put(group.getGroupId(), group);
//            internalGroupsByName.put(group.getGroupName(), group);
        }
    }

    int getPhysicalId(SocketChannel socket) {
        return (socket instanceof LoopbackSocketChannel)
                ? physicalId
                : physicalNodesIds.get(socket);
    }
}