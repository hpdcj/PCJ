/* 
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal;

/**
 * <b>Currently not used!</b>
 *
 * This class was used for synchronizing not-named group of
 * PCJ threads.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
@Deprecated
class NodesSyncData {//implements Comparable<NodesSyncData> {
//
//    final private int[] globalNodesIds;
//    final private BitMask localSync;
//    final private BitMask physicalSync;
//    final private List<Integer> physicalIds;
//    final private int myNodeIndex;
//    final private WaitObject syncObject;
//
//    /**
//     *
//     * @param nodes should be sorted
//     * @param localSize no of nodes of current physical node
//     * @param physicalIds should be sorted
//     */
//    NodesSyncData(int[] nodes) {
//        this.globalNodesIds = nodes;
//        this.physicalIds = null;
//        localSync = null;
//        physicalSync = null;
//        syncObject = null;
//        myNodeIndex = -1;
//    }
//
//    NodesSyncData(int myNodeId, int[] nodes) {
//        this.globalNodesIds = nodes;
//
//        int localSize = 0;
//        int index = -1;
//        Set<Integer> physicalSet = new TreeSet<>();
//        for (int id : nodes) {
//            if (id == myNodeId) {
//                index = localSize;
//            }
//            PcjNodeData data = InternalPCJ.getWorkerData();
//            int myPhysicalId = data.physicalId;
//            int physicalId = data.virtualNodes.get(id);
//            if (myPhysicalId == physicalId) {
//                ++localSize;
//            }
//            physicalSet.add(physicalId);
//        }
//        this.physicalIds = new ArrayList<>();
//        for (int id : physicalSet) {
//            physicalIds.add(id);
//        }
//        Collections.sort(physicalIds);
//
//        localSync = new BitMask(localSize);
//
//        physicalSync = new BitMask(this.physicalIds.size());
//
//        myNodeIndex = index;
//
//        syncObject = new WaitObject();
//    }
//
//    @Override
//    public boolean equals(Object obj) {
//        if (obj == this) {
//            return true;
//        }
//        if (obj == null) {
//            return false;
//        }
//
//        if (obj instanceof NodesSyncData == false) {
//            return false;
//        }
//
//        final NodesSyncData other = (NodesSyncData) obj;
//        if (globalNodesIds.length != other.globalNodesIds.length) {
//            return false;
//        }
//
//        for (int i = 0; i < globalNodesIds.length; ++i) {
//            if (globalNodesIds[i] != other.globalNodesIds[i]) {
//                return false;
//            }
//        }
//
//        return true;
//    }
//
//    @Override
//    public int hashCode() {
//        int hash = 7;
//        hash = 83 * hash + Arrays.hashCode(this.globalNodesIds);
//        return hash;
//    }
//
//    @Override
//    public int compareTo(NodesSyncData y) {
//        if (globalNodesIds.length == y.globalNodesIds.length) {
//            for (int i = 0; i < globalNodesIds.length; ++i) {
//                if (globalNodesIds[i] != y.globalNodesIds[i]) {
//                    return y.globalNodesIds[i] - globalNodesIds[i];
//                }
//            }
//        }
//        return y.globalNodesIds.length - globalNodesIds.length;
//    }
//
//    boolean localSync(int i) {
//        synchronized (localSync) {
//            localSync.set(i);
//            return localSync.isSet();
//        }
//    }
//
//    boolean physicalSync(int id) {
//        int position = physicalIds.indexOf(id);
//        synchronized (physicalSync) {
//            physicalSync.set(position);
//            return physicalSync.isSet();
//        }
//    }
//
//    List<Integer> getPhysicalIds() {
//        return physicalIds;
//    }
//
//    int getLocalNodeIndex() {
//        return myNodeIndex;
//    }
//
//    /**
//     * @return the syncObject
//     */
//    WaitObject getSyncObject() {
//        return syncObject;
//    }
}
