/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal;

import org.pcj.internal.utils.PcjThreadPair;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.MessageFinishCompleted;
import org.pcj.internal.message.MessageFinished;
import org.pcj.internal.message.MessageGroupJoinAnswer;
import org.pcj.internal.message.MessageGroupJoinBonjour;
import org.pcj.internal.message.MessageGroupJoinInform;
import org.pcj.internal.message.MessageGroupJoinQuery;
import org.pcj.internal.message.MessageGroupJoinRequest;
import org.pcj.internal.message.MessageGroupJoinResponse;
import org.pcj.internal.message.MessageHello;
import org.pcj.internal.message.MessageHelloBonjour;
import org.pcj.internal.message.MessageHelloCompleted;
import org.pcj.internal.message.MessageHelloGo;
import org.pcj.internal.message.MessageHelloInform;
import org.pcj.internal.message.MessageHelloResponse;
import org.pcj.internal.message.MessageLog;
import org.pcj.internal.message.MessageThreadPairSync;
import org.pcj.internal.message.MessageThreadsSyncGo;
import org.pcj.internal.message.MessageThreadsSyncWait;
import org.pcj.internal.message.MessageSyncGo;
import org.pcj.internal.message.MessageSyncWait;
import org.pcj.internal.network.SocketData;
import org.pcj.internal.message.MessageTypes;
import org.pcj.internal.message.MessageValueAsyncGetRequest;
import org.pcj.internal.message.MessageValueAsyncGetResponse;
import org.pcj.internal.message.MessageValueBroadcast;
import org.pcj.internal.message.MessageValueCompareAndSetRequest;
import org.pcj.internal.message.MessageValueCompareAndSetResponse;
import org.pcj.internal.message.MessageValuePut;
import org.pcj.internal.network.LoopbackSocketChannel;
import org.pcj.internal.utils.BitMask;
import org.pcj.internal.utils.CloneObject;
import org.pcj.internal.utils.Configuration;
import org.pcj.internal.utils.Utilities;
import org.pcj.internal.utils.WaitObject;

/**
 * This class processes incoming messages.
 *
 * At present this class can be used only in sequential manner.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class Worker implements Runnable {

    private final WorkerData data;
    private final BlockingQueue<Message> requestQueue;
    private final Map<SelectableChannel, SocketData> socketsOutputStream;
    private Networker networker;

    Worker(WorkerData data) {
        this.data = data;

        socketsOutputStream = new ConcurrentHashMap<>();
        requestQueue = new LinkedBlockingQueue<>();
    }

    void setNetworker(Networker networker) {
        if (this.networker != null) {
            throw new IllegalStateException("Networker already set.");
        }
        this.networker = networker;
    }

    public void connected(SocketChannel socket) {
        synchronized (socketsOutputStream) {
            socketsOutputStream.put(socket, new SocketData());
        }
    }

    public void channelClosed(SocketChannel socket) {
        if (socket.equals(InternalPCJ.getNode0Socket())) {
            networker.shutdown();
        }
    }

    public void parseRequest(SocketChannel socket, ByteBuffer request) {
        Message req;
        SocketData msgData = socketsOutputStream.get(socket);
        synchronized (msgData) {
            while ((req = msgData.parse(request)) != null) {
                enqueueMessage(socket, req);
            }
        }
    }

    public void enqueueMessage(SocketChannel socket, Message message) {
        try {
            message.setSocket(socket);
            requestQueue.put(message);
        } catch (InterruptedException ex) {
            ex.printStackTrace(System.err);
        }
    }

    @Override
    public void run() {
        try {
            for (;;) {
                try {
                    for (;;) {
                        processMessage(requestQueue.take());
                    }
                } catch (IOException ex) {
                    Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (InterruptedException ex) {
            //ex.printStackTrace(System.err);
        }
    }

    WorkerData getData() {
        return data;
    }

    private void broadcast(InternalGroup group, Message message) {
//        System.err.println("broadcast:" + message + " to " + group.getGroupName());
        Integer leftChildrenIndex = group.getPhysicalLeft();
        Integer rightChildrenIndex = group.getPhysicalRight();

        SocketChannel left = null;
        if (leftChildrenIndex != null) {
            left = data.physicalNodes.get(group.getPhysicalLeft());
        }

        SocketChannel right = null;
        if (rightChildrenIndex != null) {
            right = data.physicalNodes.get(group.getPhysicalRight());
        }

        networker.broadcast(left, right, message);
    }

    private void processMessage(Message message) throws IOException {
        if ((Configuration.DEBUG & 1) == 1) {
            if ((Configuration.DEBUG & 4) == 4) {
                System.err.println("" + this.data.physicalId + " recv " + message + " from " + message.getSocket().getRemoteAddress());
            } else {
                System.err.println("" + this.data.physicalId + " recv " + message.getType() + " from " + message.getSocket().getRemoteAddress());
            }
        }
        switch (message.getType()) {
            case UNKNOWN:
                break;
            case ABORT:
                break;
            case LOG:
                log((MessageLog) message);
                break;
            case HELLO:
                hello((MessageHello) message);
                break;
            case HELLO_RESPONSE:
                helloResponse((MessageHelloResponse) message);
                break;
            case HELLO_INFORM:
                helloInform((MessageHelloInform) message);
                break;
            case HELLO_BONJOUR:
                helloBonjour((MessageHelloBonjour) message);
                break;
            case HELLO_COMPLETED:
                helloCompleted();
                break;
            case HELLO_GO:
                helloGo((MessageHelloGo) message);
                break;
            case FINISHED:
                finished((MessageFinished) message);
                break;
            case FINISH_COMPLETED:
                finishCompleted((MessageFinishCompleted) message);
                break;
            case SYNC_WAIT:
                syncWait((MessageSyncWait) message);
                break;
            case SYNC_GO:
                syncGo((MessageSyncGo) message);
                break;
            case THREADS_SYNC_WAIT:
                nodesSyncWait((MessageThreadsSyncWait) message);
                break;
            case THREADS_SYNC_GO:
                nodesSyncGo((MessageThreadsSyncGo) message);
                break;
            case THREAD_PAIR_SYNC:
                nodeSync((MessageThreadPairSync) message);
                break;
            case GROUP_JOIN_QUERY:
                groupJoinQuery((MessageGroupJoinQuery) message);
                break;
            case GROUP_JOIN_ANSWER:
                groupJoinAnswer((MessageGroupJoinAnswer) message);
                break;
            case GROUP_JOIN_REQUEST:
                groupJoinRequest((MessageGroupJoinRequest) message);
                break;
            case GROUP_JOIN_RESPONSE:
                groupJoinResponse((MessageGroupJoinResponse) message);
                break;
            case GROUP_JOIN_INFORM:
                groupJoinInform((MessageGroupJoinInform) message);
                break;
            case GROUP_JOIN_BONJOUR:
                groupJoinBonjour((MessageGroupJoinBonjour) message);
                break;
            case VALUE_ASYNC_GET_REQUEST:
                valueAsyncGetRequest((MessageValueAsyncGetRequest) message);
                break;
            case VALUE_ASYNC_GET_RESPONSE:
                valueAsyncGetResponse((MessageValueAsyncGetResponse) message);
                break;
            case VALUE_COMPARE_AND_SET_REQUEST:
                valueCompareAndSetRequest((MessageValueCompareAndSetRequest) message);
                break;
            case VALUE_COMPARE_AND_SET_RESPONSE:
                valueCompareAndSetResponse((MessageValueCompareAndSetResponse) message);
                break;
            case VALUE_PUT:
                valuePut((MessageValuePut) message);
                break;
            case VALUE_BROADCAST:
                valueBroadcast((MessageValueBroadcast) message);
                break;
            default:
                throw new AssertionError(message.getType().name());
        }
    }

    /**
     * @see MessageTypes#LOG
     */
    private void log(MessageLog message) {
        int groupId = message.getGroupId();
        InternalPCJ.getStdout().println(
                (groupId == 0 ? "" : (data.internalGroupsById.get(groupId).getGroupName() + ":"))
                + message.getGroupNodeId()
                + " > "
                + message.getLogText());
    }

    /**
     * @see MessageTypes#HELLO
     */
    private void hello(MessageHello message) throws IOException {
        int physicalId = -(++data.physicalNodesCount);
        data.physicalNodes.put(physicalId, message.getSocket());
        data.helloMessages.put(physicalId, message);
        int[] ids = message.getNodeIds();
        for (int id : ids) {
            data.virtualNodes.put(id, physicalId);
        }

        synchronized (data) {
            data.clientsConnected += ids.length;

            /* if everyone is connected */
            if (data.clientsConnected == data.clientsCount) {

                /* renumber physical ids and prepare IDs lists */
                Map<Integer, List<Integer>> nodesIds = new HashMap<>();

                physicalId = 0;
                Map<Integer, Integer> mapping = new HashMap<>();
                int[] messageIds = new int[data.physicalNodesCount];
                int[] ports = new int[data.physicalNodesCount];

                Integer[] virtualIds = data.virtualNodes.keySet().toArray(new Integer[0]);
                Arrays.sort(virtualIds);

                /* process all virtualIds in ascending order */
                for (int virtualId : virtualIds) {
                    /* getFutureObject physicalId associeted with virtualId */
                    int oldPhysicalId = data.virtualNodes.remove(virtualId);

                    /* if that physicalId isn't mapped... */
                    if (mapping.containsKey(oldPhysicalId) == false) {
                        /* map old-physicalId with new-physicalId (0, 1, 2, ...) */
                        mapping.put(oldPhysicalId, physicalId);

                        /* getFutureObject socket associeted with old-physicalId */
                        SocketChannel socketChannel = data.physicalNodes.remove(oldPhysicalId);

                        /* and put it as new-physicalId */
                        data.physicalNodes.put(physicalId, socketChannel);
                        if (socketChannel != null) {
                            data.physicalNodesIds.put(socketChannel, physicalId);
                        }

                        /* getFutureObject hello messageId and portNo from helloMessages map */
                        message = data.helloMessages.remove(oldPhysicalId);
                        messageIds[physicalId] = message.getMessageId();
                        ports[physicalId] = message.getPort();

                        /* prepare for new physicalId */
                        ++physicalId;
                    }

                    int newPhysicalId = mapping.get(oldPhysicalId);

                    /* save information about nodes using new-physicalId */
                    if (nodesIds.containsKey(newPhysicalId) == false) {
                        nodesIds.put(newPhysicalId, new ArrayList<Integer>());
                    }
                    nodesIds.get(newPhysicalId).add(virtualId);

                    /* assiciate virtual id with new-physicalId */
                    data.virtualNodes.put(virtualId, newPhysicalId);
                }

                /* reply message with physical id to each node */
                for (Entry<Integer, SocketChannel> entry : data.physicalNodes.entrySet()) {
                    MessageHelloResponse reply = new MessageHelloResponse();
                    int id = entry.getKey();
                    reply.setInReplyTo(messageIds[id]);
                    reply.setPhysicalId(id);
                    int parentPhysicalId;
                    if (id <= 0) {
                        parentPhysicalId = -1;
                    } else {
                        parentPhysicalId = (id - 1) / 2;
                    }
                    reply.setParentPhysicalId(parentPhysicalId);
                    String hostString = ((InetSocketAddress) data.physicalNodes.get(id).getRemoteAddress()).getHostString();

                    reply.setHostname(hostString);
                    networker.send(entry.getValue(), reply);
                }

                InternalGroup globalGroup = data.internalGlobalGroup;

                /* broadcast information about all physical nodes */
                for (int id = 0; id < data.physicalNodesCount; ++id) {
                    globalGroup.addPhysicalId(id);

                    String hostString = ((InetSocketAddress) data.physicalNodes.get(id).getRemoteAddress()).getHostString();

                    MessageHelloInform msg = new MessageHelloInform();
                    msg.setPhysicalId(id);
                    msg.setNodeIds(Utilities.listAsArray(nodesIds.get(id)));
                    int parentPhysicalId;
                    if (id <= 0) {
                        parentPhysicalId = -1;
                    } else {
                        parentPhysicalId = (id - 1) / 2;
                    }
                    msg.setParentPhysicalId(parentPhysicalId);
                    msg.setHost(hostString);
                    msg.setPort(ports[id]);

                    //networker.send(InternalPCJ.getNode0Socket(), msg);
                    enqueueMessage(InternalPCJ.getNode0Socket(), msg);
                }
            }
        }
    }

    /**
     * @see MessageTypes#HELLO_RESPONSE
     */
    private void helloResponse(MessageHelloResponse message) {
        int physicalId = message.getPhysicalId();

        InternalGroup globalGroup = data.internalGlobalGroup;

        data.physicalId = physicalId;
        data.physicalNodes.put(physicalId, LoopbackSocketChannel.getInstance());
        for (int id : data.localIds) {
            data.virtualNodes.put(id, physicalId);
            globalGroup.add(id, id, physicalId);
        }

        globalGroup.setPhysicalMaster(0);
        globalGroup.setPhysicalParent(message.getParentPhysicalId());
    }

    /**
     * @see MessageTypes#HELLO_INFORM
     */
    private void helloInform(MessageHelloInform message) throws IOException {
        int physicalId = message.getPhysicalId();
        InternalGroup globalGroup = data.internalGlobalGroup;
        broadcast(globalGroup, message);

        /* add information about new-node */
        for (int id : message.getNodeIds()) {
            data.virtualNodes.put(id, physicalId);
            globalGroup.add(id, id, physicalId);
        }

        /* connect to new-node */
        SocketChannel newNodeChannel = null;
        if (data.physicalNodes.get(physicalId) == null) {
            if (!message.getHost().isEmpty()) {
                newNodeChannel = networker.connectTo(
                        InetAddress.getByName(message.getHost()),
                        message.getPort(),
                        Configuration.RETRY_COUNT, Configuration.RETRY_DELAY);
            } else {
                //newNodeChannel = new LoopSocketChannel();
            }
            data.physicalNodes.put(physicalId, newNodeChannel);
            if (newNodeChannel != null) {
                data.physicalNodesIds.put(newNodeChannel, physicalId);
            }
        } else {
            newNodeChannel = data.physicalNodes.get(physicalId);
        }

        if (message.getParentPhysicalId() == data.physicalId && physicalId != data.physicalId) {
            if (globalGroup.getPhysicalLeft() == null) {
                globalGroup.setPhysicalLeft(physicalId);
            } else if (globalGroup.getPhysicalRight() == null) {
                globalGroup.setPhysicalRight(physicalId);
            } else {
                throw new IllegalStateException("Node " + data.physicalId + " already has two children.");
            }
        }

        /* "welcomes" new-node */
        MessageHelloBonjour reply = new MessageHelloBonjour();
        reply.setPhysicalId(data.physicalId);
        reply.setNodeIds(data.localIds);
        networker.send(newNodeChannel, reply);
    }

    /**
     * @see MessageTypes#HELLO_BONJOUR
     */
    private void helloBonjour(MessageHelloBonjour message) throws IOException {
        InternalGroup globalGroup = data.internalGlobalGroup;
        int physicalId = message.getPhysicalId();

        data.physicalNodes.put(physicalId, message.getSocket());
        data.physicalNodesIds.put(message.getSocket(), physicalId);

        for (int id : message.getNodeIds()) {
            data.virtualNodes.put(id, physicalId);
            globalGroup.add(id, id, physicalId);
        }

        boolean allPrevious;

        BitMask mask = globalGroup.getJoinBitmask(data.physicalId);
        synchronized (mask) {
            mask.set(physicalId);

            allPrevious = mask.isSet();
        }

        if (allPrevious) {
            MessageHelloCompleted msg = new MessageHelloCompleted();
            networker.send(InternalPCJ.getNode0Socket(), msg);
        }
    }

    /**
     * @see MessageTypes#HELLO_COMPLETED
     */
    private void helloCompleted() throws IOException {
        if (++data.helloCompletedCount == data.physicalNodesCount) {
            MessageHelloGo msg = new MessageHelloGo();
            networker.send(data.internalGlobalGroup, msg);
        }
    }

    /**
     * @see MessageTypes#HELLO_GO
     */
    private void helloGo(MessageHelloGo message) {
        InternalGroup globalGroup = data.internalGlobalGroup;
        broadcast(globalGroup, message);

        WaitObject sync = data.internalGlobalGroup.getSyncObject();
        sync.signalAll();
    }

    /**
     * @see MessageTypes#FINISHED
     */
    private void finished(MessageFinished message) throws IOException {
        if (--data.physicalNodesCount == 0) {
            InternalGroup globalGroup = data.internalGlobalGroup;

            MessageFinishCompleted reply = new MessageFinishCompleted();
            reply.setInReplyTo(message.getMessageId());
            networker.send(globalGroup, reply);
        }
    }

    private void finishCompleted(MessageFinishCompleted message) {
        InternalGroup globalGroup = data.internalGlobalGroup;
        broadcast(globalGroup, message);

        synchronized (data.finishObject) {
            data.finishObject.notifyAll();
        }
    }

    /**
     * @see MessageTypes#SYNC_WAIT
     */
    private void syncWait(MessageSyncWait message) throws IOException {
        InternalGroup group = data.internalGroupsById.get(message.getGroupId());
        int physicalId = data.getPhysicalId(message.getSocket());
        final BitMask physicalSync = group.getPhysicalSync();
        synchronized (physicalSync) {
            if (group.physicalSync(physicalId)) {
                physicalSync.clear();

                MessageSyncGo msg = new MessageSyncGo();
                msg.setGroupId(message.getGroupId());
                networker.send(group, msg);
            }
        }
    }

    /**
     * @see MessageTypes#SYNC_GO
     */
    private void syncGo(MessageSyncGo message) {
        InternalGroup group = data.internalGroupsById.get(message.getGroupId());
        broadcast(group, message);

        WaitObject sync = group.getSyncObject();
        sync.signalAll();
    }

    /**
     * @see MessageTypes#GROUP_JOIN_QUERY
     */
    private void groupJoinQuery(MessageGroupJoinQuery message) throws IOException {
        String groupName = message.getGroupName();
        int groupId;
        int masterPhysicalId;

        synchronized (data.groupsIds) {
            if (data.groupsIds.containsKey(groupName)) {
                groupId = data.groupsIds.get(groupName);
                masterPhysicalId = data.groupsMaster.get(groupId);
            } else {
                groupId = data.groupsIds.size();
                masterPhysicalId = data.getPhysicalId(message.getSocket());

                data.groupsIds.put(groupName, groupId);
                data.groupsMaster.put(groupId, masterPhysicalId);
            }
        }

        MessageGroupJoinAnswer reply = new MessageGroupJoinAnswer();
        reply.setInReplyTo(message.getMessageId());
        reply.setMasterPhysicalId(masterPhysicalId);
        reply.setGroupId(groupId);
        reply.setGroupName(groupName);
        networker.send(message.getSocket(), reply);
    }

    /**
     * @see MessageTypes#GROUP_JOIN_ANSWER
     */
    private void groupJoinAnswer(MessageGroupJoinAnswer message) {
        int groupId = message.getGroupId();
        InternalGroup group;
        synchronized (data.internalGroupsById) {
            group = data.internalGroupsById.get(groupId);
            if (group == null) {
                group = new InternalGroup(groupId, message.getGroupName());
                group.setPhysicalMaster(message.getMasterPhysicalId());
                if (message.getMasterPhysicalId() == data.physicalId) {
                    group.addPhysicalId(data.physicalId);
                }
                data.addGroup(group);
            }
        }

        InternalResponseAttachment attachment
                = (InternalResponseAttachment) data.attachmentMap.remove(message.getInReplyTo());

        attachment.setObject(new Object[]{message.getMasterPhysicalId(), groupId});

        synchronized (attachment) {
            attachment.notifyAll();
        }
    }

    /**
     * @see MessageTypes#GROUP_JOIN_REQUEST
     */
    private void groupJoinRequest(MessageGroupJoinRequest message) throws IOException {
        int groupId = message.getGroupId();
        int globalNodeId = message.getGlobalNodeId();

        InternalGroup group;
        synchronized (data.internalGroupsById) {
            group = data.internalGroupsById.get(groupId);
            if (group == null) {
                group = new InternalGroup(groupId, message.getGroupName());
                group.setPhysicalMaster(data.physicalId);
                group.addPhysicalId(data.physicalId);
                data.addGroup(group);
            }
        }

        int groupNodeId = group.nextNodeNum();

        int physicalNodeId = data.getPhysicalId(message.getSocket());
        int index = group.addPhysicalId(physicalNodeId);
        int parentPhysicalId;
        if (index <= 0) {
            parentPhysicalId = -1;
        } else {
            parentPhysicalId = group.getPhysicalIds()[(index - 1) / 2];
        }

        MessageGroupJoinResponse reply = new MessageGroupJoinResponse();
        reply.setInReplyTo(message.getMessageId());
        reply.setGroupId(groupId);
        reply.setGlobalNodeId(globalNodeId);
        reply.setGroupNodeId(groupNodeId);
        reply.setParentPhysicalId(parentPhysicalId);
        networker.send(message.getSocket(), reply);

        MessageGroupJoinInform msg = new MessageGroupJoinInform();
        msg.setGroupId(groupId);
        msg.setGlobalNodeId(globalNodeId);
        msg.setGroupNodeId(groupNodeId);
        msg.setParentPhysicalId(parentPhysicalId);
        networker.send(group, msg);
    }

    /**
     * @see MessageTypes#GROUP_JOIN_RESPONSE
     */
    private void groupJoinResponse(MessageGroupJoinResponse message) throws IOException {
        int groupId = message.getGroupId();
        int globalNodeId = message.getGlobalNodeId();
        int groupNodeId = message.getGroupNodeId();
        int parentPhysicalId = message.getParentPhysicalId();

        InternalGroup group = data.internalGroupsById.get(groupId);
        group.add(groupNodeId, globalNodeId, data.physicalId);

        PcjThreadLocalData localData = data.localData.get(globalNodeId);
        InternalGroup lGroup = localData.createGroup(groupNodeId, group);
        localData.addGroup(lGroup);

        if (parentPhysicalId >= 0) {
            synchronized (group) {
                group.setPhysicalParent(parentPhysicalId);
            }
        }

        int inReplyTo = message.getInReplyTo();
        InternalResponseAttachment attachment = (InternalResponseAttachment) data.attachmentMap.get(inReplyTo);

        attachment.setObject(lGroup);
        data.attachmentMap.remove(inReplyTo);

        synchronized (attachment) {
            attachment.notifyAll();
        }
    }

    /**
     * @see MessageTypes#GROUP_JOIN_INFORM
     */
    private void groupJoinInform(MessageGroupJoinInform message) throws IOException {
        int groupId = message.getGroupId();
        int globalNodeId = message.getGlobalNodeId();
        int groupNodeId = message.getGroupNodeId();
        int physicalNodeId = data.virtualNodes.get(globalNodeId);
        InternalGroup group = data.internalGroupsById.get(groupId);

        if (message.getParentPhysicalId() == data.physicalId) {
            synchronized (group) {
                Integer left = group.getPhysicalLeft();
                Integer right = group.getPhysicalRight();
                if (left == null) {
                    group.setPhysicalLeft(physicalNodeId);
                } else if (left == physicalNodeId) {
                } else if (right == null) {
                    group.setPhysicalRight(physicalNodeId);
                } else if (right == physicalNodeId) {
                } else {
                    throw new IllegalStateException("Node " + data.physicalId + " already has two children.");
                }
            }
        }

        broadcast(group, message);

        group.add(groupNodeId, globalNodeId, physicalNodeId);

        List<Integer> localIds = group.getLocalIds();
        int localSize = localIds.size();
        if (localSize > 0) {
            int[] groupIds = new int[localSize];
            int[] globalIds = new int[localSize];
            int i = 0;
            for (int id : localIds) {
                groupIds[i] = id;
                globalIds[i] = group.getNode(id);
                ++i;
            }

            MessageGroupJoinBonjour msg = new MessageGroupJoinBonjour();
            msg.setGroupId(groupId);
            msg.setNewNodeId(groupNodeId);
            msg.setGlobalNodeIds(globalIds);
            msg.setGroupNodeIds(groupIds);
            networker.send(data.physicalNodes.get(physicalNodeId), msg);
        }
//        System.out.println(data.physicalId + " sends " + msg + " to " + physicalNodeId + " " + data.physicalNodes.getFutureObject(physicalNodeId));
    }

    /**
     * @see MessageTypes#GROUP_JOIN_BONJOUR
     */
    private void groupJoinBonjour(MessageGroupJoinBonjour message) {
//        System.out.println(data.physicalId + " received " + message);
        int physicalNodeId = data.getPhysicalId(message.getSocket());
        int groupId = message.getGroupId();
        int nodeGroupId = message.getNewNodeId();
        int[] globalIds = message.getGlobalNodeIds();
        int[] groupIds = message.getGroupNodeIds();

        InternalGroup group = data.internalGroupsById.get(groupId);
        BitMask mask = group.getJoinBitmask(nodeGroupId);

        synchronized (mask) {
            for (int i = 0; i < globalIds.length; ++i) {
                if (groupIds[i] < nodeGroupId) {
                    mask.set(groupIds[i]);
                }
                group.add(groupIds[i], globalIds[i], physicalNodeId);
            }

            if (mask.isSet()) {
                mask.notifyAll();
            }
        }
    }

    private void nodesSyncWait(MessageThreadsSyncWait message) throws IOException {
        int physicalId = data.getPhysicalId(message.getSocket());
        int[] nodes = message.getNodesGlobalIds();
        NodesSyncData nsd = new NodesSyncData(-1, nodes);

        NodesSyncData old = data.nodesSyncData.putIfAbsent(nsd, nsd);
        if (old != null) {
            nsd = old;
        }

        synchronized (nsd) {
            if (nsd.physicalSync(physicalId)) {
                MessageThreadsSyncGo msg = new MessageThreadsSyncGo();
                msg.setNodesGlobalIds(nodes);

                networker.send(data.physicalNodes.get(nsd.getPhysicalIds().get(0)), msg);
            }
        }
    }

    private void nodesSyncGo(MessageThreadsSyncGo message) {
        int[] nodes = message.getNodesGlobalIds();
        NodesSyncData nsd = new NodesSyncData(nodes);
        nsd = data.nodesSyncData.remove(nsd);

        List<Integer> physicalIds = nsd.getPhysicalIds();
        int physicalIndex = physicalIds.indexOf(data.physicalId);

        SocketChannel left = null;
        SocketChannel right = null;
        if (physicalIndex * 2 + 1 < physicalIds.size()) {
            left = data.physicalNodes.get(physicalIds.get(physicalIndex * 2 + 1));

            if (physicalIndex * 2 + 2 < physicalIds.size()) {
                right = data.physicalNodes.get(physicalIds.get(physicalIndex * 2 + 2));
            }
        }

        networker.broadcast(left, right, message);

        nsd.getSyncObject().signalAll();
    }

    /**
     * @see MessageTypes#THREAD_PAIR_SYNC
     */
    private void nodeSync(MessageThreadPairSync message) {
        final Map<PcjThreadPair, PcjThreadPair> syncNode = data.syncNodePair;
        PcjThreadPair pair = new PcjThreadPair(
                message.getReceiverGlobalNodeId(),
                message.getSenderGlobalNodeId());

        synchronized (syncNode) {
            if (syncNode.containsKey(pair)) {
                pair = syncNode.get(pair);
            } else {
                syncNode.put(pair, pair);
            }
            pair.increase();
            syncNode.notifyAll();
        }
    }

    /**
     * @see MessageTypes#VALUE_ASYNC_GET_REQUEST
     */
    private void valueAsyncGetRequest(MessageValueAsyncGetRequest message) throws IOException {
        Object value = data.localData.get(message.getReceiverGlobalNodeId()).getStorage().
                get(message.getVariableName(), message.getIndexes());

        MessageValueAsyncGetResponse reply = new MessageValueAsyncGetResponse();
        reply.setInReplyTo(message.getMessageId());
        reply.setReceiverGlobalNodeId(message.getSenderGlobalNodeId());
        reply.setVariableValue(CloneObject.serialize(value));
        networker.send(message.getSocket(), reply);
    }

    /**
     * @see MessageTypes#VALUE_ASYNC_GET_RESPONSE
     */
    private void valueAsyncGetResponse(MessageValueAsyncGetResponse message) {
        int inReplyTo = message.getInReplyTo();
        ResponseAttachment attachment = (ResponseAttachment) data.attachmentMap.remove(inReplyTo);
        data.localData.get(message.getReceiverGlobalNodeId()).getDeserializer()
                .deserialize(message.getVariableValue(), attachment);
    }

    /**
     * @see MessageTypes#VALUE_COMPARE_AND_SET_REQUEST
     */
    private void valueCompareAndSetRequest(MessageValueCompareAndSetRequest message) throws IOException {
        Object expectedValue = data.localData.get(message.getReceiverGlobalNodeId()).
                deserializeObject(message.getExpectedValue());
        Object newValue = data.localData.get(message.getReceiverGlobalNodeId()).
                deserializeObject(message.getNewValue());
        Object value = data.localData.get(message.getReceiverGlobalNodeId()).getStorage().
                cas(message.getVariableName(), expectedValue, newValue, message.getIndexes());

        MessageValueCompareAndSetResponse reply = new MessageValueCompareAndSetResponse();
        reply.setInReplyTo(message.getMessageId());
        reply.setReceiverGlobalNodeId(message.getSenderGlobalNodeId());
        reply.setVariableValue(CloneObject.serialize(value));
        networker.send(message.getSocket(), reply);
    }

    /**
     * @see MessageTypes#VALUE_COMPARE_AND_SET_RESPONSE
     */
    private void valueCompareAndSetResponse(MessageValueCompareAndSetResponse message) {
        int inReplyTo = message.getInReplyTo();
        ResponseAttachment attachment = (ResponseAttachment) data.attachmentMap.remove(inReplyTo);
        data.localData.get(message.getReceiverGlobalNodeId()).getDeserializer()
                .deserialize(message.getVariableValue(), attachment);
    }

    /**
     * @see MessageTypes#VALUE_PUT
     */
    private void valuePut(MessageValuePut message) {
        data.localData.get(message.getReceiverGlobalNodeId()).getDeserializer()
                .deserialize(message.getVariableValue(), message.getVariableName(), message.getIndexes());
    }

    /**
     * @see MessageTypes#VALUE_BROADCAST
     */
    private void valueBroadcast(MessageValueBroadcast message) {
        InternalGroup group = data.internalGroupsById.get(message.getGroupId());
        broadcast(group, message);

        for (int id : group.getLocalIds()) {
            int nodeId = group.getNode(id);
            data.localData.get(nodeId).getDeserializer()
                    .deserialize(message.getVariableValue(), message.getVariableName());
        }
    }
}
