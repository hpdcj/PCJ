/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal;

import org.pcj.internal.futures.LocalBarrier;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.pcj.PcjFuture;
import org.pcj.internal.message.MessageGroupBarrierWaiting;

/**
 * Internal (with common ClassLoader) representation of Group. It contains
 * common data for groups.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class InternalGroup {

    final public static int GLOBAL_GROUP_ID = 0;
    final public static String GLOBAL_GROUP_NAME = "";

    final private ConcurrentMap<Integer, Integer> threadsMapping; // groupId, globalId
    final private int groupId;
    final private String groupName;
    final private List<Integer> localIds;
    final private List<Integer> physicalIds;
//    final private Bitmask localBarrierBitmask;
    final private Bitmask localBitmask;
    private final ConcurrentMap<Integer, LocalBarrier> localBarrierMap;
    final private ConcurrentMap<Integer, Bitmask> physicalBitmaskMap;
//    final private MessageGroupBarrierWaiting groupBarrierWaitingMessage;
//    final private ConcurrentMap<Integer, BitMask> joinBitmaskMap;
//    final private AtomicInteger nodeNum = new AtomicInteger(0);
//    /**
//     * list of local node group ids
//     */
//    final private ArrayList<Integer> localIds;
//    /**
//     * list of remote computers ids in this group (for broadcast)
//     */
//    final private List<Integer> physicalIds;
//    /**
//     * sync
//     */
//    final private BitMask localSync;
//    final private BitMask localSyncMask;
//    final private BitMask physicalSync;
//    /**
//     * Physical Parent, Left, Right
//     */
    final private CommunicationTree physicalTree;

    //private final InternalGroup g;
    public InternalGroup(InternalGroup g) {
        this.groupId = g.groupId;
        this.groupName = g.groupName;
        this.physicalTree = g.physicalTree;

        this.threadsMapping = g.threadsMapping;
        this.physicalBitmaskMap = g.physicalBitmaskMap;
        this.localBarrierMap = g.localBarrierMap;
        this.localBitmask = g.localBitmask;

//        this.groupBarrierWaitingMessage = g.groupBarrierWaitingMessage;
//        this.joinBitmaskMap = g.joinBitmaskMap;
//
//        this.syncMessage = g.syncMessage;
//
        this.localIds = g.localIds;
        this.physicalIds = g.physicalIds;
//
//        this.localSync = g.localSync;
//        this.localSyncMask = g.localSyncMask;
    }

    public InternalGroup(int groupMaster, int groupId, String groupName) {
        this.groupId = groupId;
        this.groupName = groupName;
        physicalTree = new CommunicationTree(groupMaster);

        threadsMapping = new ConcurrentHashMap<>();
        physicalBitmaskMap = new ConcurrentHashMap<>();
        localBarrierMap = new ConcurrentHashMap<>();
        localBitmask = new Bitmask();

//        groupBarrierWaitingMessage = new MessageGroupBarrierWaiting(groupId, InternalPCJ.getNodeData().getPhysicalId());
//        this.joinBitmaskMap = new ConcurrentHashMap<>();
//
//        syncMessage = new MessageSyncWait();
//        syncMessage.setGroupId(groupId);
//
        localIds = new ArrayList<>();
        physicalIds = new ArrayList<>();

//
//        localSync = new BitMask();
//        localSyncMask = new BitMask();
//
//        waitObject = new WaitObject();
//
    }

    protected int getGroupId() {
        return groupId;
    }

    protected String getGroupName() {
        return groupName;
    }

    public int getGroupMasterNode() {
        return physicalTree.getRootNode();
    }

    public List<Integer> getChildrenNodes() {
        return physicalTree.getChildrenNodes();
    }

    public Integer getPhysicalIdIndex(int physicalId) {
        return physicalIds.indexOf(physicalId);
    }

    protected int myId() {
        throw new IllegalStateException("This method has to be overriden!");
    }

    protected int threadCount() {
        return threadsMapping.size();
    }

    final public void addThread(int physicalId, int groupThreadId, int globalThreadId) {
        int currentPhysicalId = InternalPCJ.getNodeData().getPhysicalId();

        threadsMapping.put(groupThreadId, globalThreadId);
        synchronized (physicalIds) {
            if (physicalIds.contains(physicalId) == false) {
                physicalIds.add(physicalId);

                int index = physicalIds.size() - 1;
                if (index > 0) {
                    if (physicalId == currentPhysicalId) {
                        physicalTree.setParentNode((index - 1) / 2);
                    }
                    if (physicalIds.get((index - 1) / 2) == currentPhysicalId) {
                        physicalTree.getChildrenNodes().add(physicalId);
                    }
                }
            }
        }

        if (physicalId == currentPhysicalId) {
            synchronized (localIds) {
                localIds.add(groupThreadId);
                if (groupThreadId + 1 > localBitmask.getSize()) {
                    localBitmask.setSize(groupThreadId + 1);
                }
                localBitmask.set(groupThreadId);
            }
        }
    }

    protected PcjFuture<Void> asyncBarrier() {
        throw new IllegalStateException("This method has to be overriden!");
    }

    protected LocalBarrier barrier(int threadId, int barrierRound) {
        LocalBarrier localBarrier = localBarrierMap
                .computeIfAbsent(barrierRound, round -> new LocalBarrier(round, localBitmask));

        synchronized (localBarrier) {
            localBarrier.set(threadId);
            if (localBarrier.isSet()) {
                int groupMasterId = getGroupMasterNode();
                SocketChannel groupMasterSocket = InternalPCJ.getNodeData()
                        .getSocketChannelByPhysicalId().get(groupMasterId);

                MessageGroupBarrierWaiting message = new MessageGroupBarrierWaiting(
                        groupId, InternalPCJ.getNodeData().getPhysicalId(), localBarrier.getRound());

                InternalPCJ.getNetworker().send(groupMasterSocket, message);
            }
        }

        return localBarrier;
    }

    public Bitmask getPhysicalBitmask(int round) {
        return physicalBitmaskMap
                .computeIfAbsent(round, k -> new Bitmask(physicalIds.size()));
    }

    public ConcurrentMap<Integer, LocalBarrier> getLocalBarrierMap() {
        return localBarrierMap;
    }

//    protected <T> PcjFuture<T> asyncGet(int myThreadId, int threadId, String variable, int... indices) {
//        threadId = nodes.get(threadId);
//        
//        MessageValueAsyncGetRequest msg = new MessageValueAsyncGetRequest();
//        msg.setSenderGlobalNodeId(myThreadId);
//        msg.setReceiverGlobalNodeId(threadId);
//        msg.setIndexes(indices);
//        msg.setVariableName(variable);
//        
//        FutureObject<T> futureObject = new FutureObject<>();
//        
//        InternalPCJ.getWorkerData().attachmentMap.put(msg.getMessageId(), futureObject);
//        try {
//            InternalPCJ.getNetworker().send(threadId, msg);
//            return futureObject;
//        } catch (IOException ex) {
//            throw new PcjRuntimeException(ex);
//        }
//    }
//    /**
//     * Used while joining. joinBitmaskMap stores information about nodes that
//     * received information about new node (current) and send bonjour message.
//     *
//     * @param groupNodeId
//     *
//     * @return
//     */
//    BitMask getJoinBitmask(int groupNodeId) {
//        BitMask newMask = new BitMask(groupNodeId + 1);
//        BitMask mask = joinBitmaskMap.putIfAbsent(groupNodeId, newMask);
//        if (mask == null) {
//            mask = newMask;
//            synchronized (mask) {
//                mask.set(groupNodeId);
//            }
//        }
//        return mask;
//    }
//
//    /**
//     * @return the nodeNum
//     */
//    int nextNodeNum() {
//        return nodeNum.getAndIncrement();
//    }
//
//    synchronized int addPhysicalId(int id) {
//        int index = physicalIds.indexOf(id);
//        if (index < 0) {
//            physicalIds.add(id);
//            // FIXME: to wszystko psuje ---v-v-v---
//            // sortowanie powinno być wykonywane w jakiś mądrzejszy sposób
//            // ...
//            // dodatkowo bez sortowania kolejność fizycznych węzłów na liście
//            // jest różna, a co za tym idzie, broadcast może się zapętlić...
//            // ...
//            // kolejność powinna być w jakiś sposób wspólna dla wszystkich
//            // nawet w trakcie tworzenia grupy
//            // ...
//            // może przez wysyłanie razem z numerem groupId, wartości index?
//            //Collections.sort(physicalIds);
//            index = physicalIds.size() - 1;
//            physicalSync.insert(index, 0);
//        }
//        return index;
//    }
//
//    /**
//     * adds info about new node in group, or nothing if groupNodeId exists in
//     * group
//     *
//     * @param groupNodeId          groupNodeId of adding node
//     * @param globalNodeId         globalNodeId of adding node
//     * @param remotePhysicalNodeId physicalId of adding node
//     */
//    synchronized void add(int groupNodeId, int globalNodeId, int remotePhysicalNodeId) {
//        if (nodes.containsKey(groupNodeId) == false) {
//            nodes.put(groupNodeId, globalNodeId);
////            addPhysicalId(remotePhysicalNodeId);
//
//            int size = Math.max(localSync.getSize(), groupNodeId + 1);
//            localSync.setSize(size);
//            localSyncMask.setSize(size);
//            if (remotePhysicalNodeId == InternalPCJ.getWorkerData().physicalId) {
//                localIds.add(groupNodeId);
//                localSyncMask.set(groupNodeId);
//            }
//        }
//    }
//
//    synchronized int[] getPhysicalIds() {
//        int[] ids = new int[physicalIds.size()];
//        int i = 0;
//        for (int id : physicalIds) {
//            ids[i++] = id;
//        }
//        return ids;
//    }
//
//    boolean physicalSync(int physicalId) {
//        int position = physicalIds.indexOf(physicalId);
//        physicalSync.set(position);
//        return physicalSync.isSet();
//    }
//
//    /**
//     * @return the localIds
//     */
//    List<Integer> getLocalIds() {
//        return localIds;
//    }
//
//    /**
//     * Gets global node id from group node id
//     *
//     * @param nodeId group node id
//     *
//     * @return global node id or -1 if group doesn't have specified group node
//     *         id
//     */
//    int getNode(int nodeId) {
//        if (nodes.containsKey(nodeId)) {
//            return nodes.get(nodeId);
//        }
//        return -1;
//    }
//
//
//    /**
//     * Synchronize current node and node with specified group nodeId
//     *
//     * @param nodeId group node id
//     */
//    protected void barrier(int myNodeId, int nodeId) {
//        // FIXME: trzeba sprawdzić jak to będzie działać w pętli.
////        if (true) {
////            sync(myNodeId, new int[]{nodeId});
////        }
//
//        /* change current group nodeId to global nodeId */
//        nodeId = nodes.get(nodeId);
//        myNodeId = nodes.get(myNodeId);
//
//        try {
//            MessageThreadPairSync msg = new MessageThreadPairSync();
//            msg.setSenderGlobalNodeId(myNodeId);
//            msg.setReceiverGlobalNodeId(nodeId);
//
//            InternalPCJ.getNetworker().send(nodeId, msg);
//        } catch (IOException ex) {
//            throw new RuntimeException(ex);
//        }
//
//        final Map<PcjThreadPair, PcjThreadPair> syncNode = InternalPCJ.getWorkerData().syncNodePair;
//        PcjThreadPair pair = new PcjThreadPair(myNodeId, nodeId);
//        synchronized (syncNode) {
//            while (syncNode.containsKey(pair) == false) {
//                try {
//                    syncNode.wait();
//                } catch (InterruptedException ex) {
//                    throw new RuntimeException(ex);
//                }
//            }
//            pair = syncNode.remove(pair);
//            if (pair.get() > 1) {
//                pair.decrease();
//                syncNode.put(pair, pair);
//            }
//        }
//    }
//
//
//    protected PcjFuture<Void> put(int nodeId, String variable, Object newValue, int... indices) {
//        nodeId = nodes.get(nodeId);
//
//        MessageValuePut msg = new MessageValuePut();
//        msg.setReceiverGlobalNodeId(nodeId);
//        msg.setVariableName(variable);
//        msg.setIndexes(indices);
//        msg.setVariableValue(CloneObject.serialize(newValue));
//
//        try {
//            InternalPCJ.getNetworker().send(nodeId, msg);
//            return new PcjFuture<Void>() {
//                @Override
//                public boolean cancel(boolean mayInterruptIfRunning) {
//                    throw new IllegalStateException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//                }
//
//                @Override
//                public boolean isCancelled() {
//                    throw new IllegalStateException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//                }
//
//                @Override
//                public boolean isDone() {
//                    throw new IllegalStateException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//                }
//
//                @Override
//                public Void get() {
//                    throw new IllegalStateException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//                }
//
//                @Override
//                public Void get(long timeout, TimeUnit unit) throws TimeoutException {
//                    throw new IllegalStateException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//                }
//            };
//        } catch (IOException ex) {
//            throw new RuntimeException(ex);
//        }
//    }
//
//    protected PcjFuture<Void> broadcast(String variable, Object newValue) {
//        MessageValueBroadcast msg = new MessageValueBroadcast();
//        msg.setGroupId(groupId);
//        msg.setVariableName(variable);
//        msg.setVariableValue(CloneObject.serialize(newValue));
//
//        try {
//            InternalPCJ.getNetworker().send(this, msg);
//        } catch (IOException ex) {
//            throw new RuntimeException(ex);
//        }
//
//        return new PcjFuture<Void>() {
//            @Override
//            public boolean cancel(boolean mayInterruptIfRunning) {
//                throw new IllegalStateException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//            }
//
//            @Override
//            public boolean isCancelled() {
//                throw new IllegalStateException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//            }
//
//            @Override
//            public boolean isDone() {
//                throw new IllegalStateException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//            }
//
//            @Override
//            public Void get() {
//                throw new IllegalStateException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//            }
//
//            @Override
//            public Void get(long timeout, TimeUnit unit) throws TimeoutException {
//                throw new IllegalStateException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//            }
//        };
//    }
    /**
     * Class for representing part of communication tree.
     *
     * @author Marek Nowicki (faramir@mat.umk.pl)
     */
    public static class CommunicationTree {

        private final int rootNode;
        private int parentNode;
        private final List<Integer> childrenNodes;

        public CommunicationTree(int rootNode) {
            this.rootNode = rootNode;
            childrenNodes = new ArrayList<>();
        }

        public int getRootNode() {
            return rootNode;
        }

        public void setParentNode(int parentNode) {
            this.parentNode = parentNode;
        }

        public int getParentNode() {
            return parentNode;
        }

        public List<Integer> getChildrenNodes() {
            return childrenNodes;
        }
    }

}
