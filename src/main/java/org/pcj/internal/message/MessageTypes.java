/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public enum MessageTypes {

    /**
     * Unknown MessageTypes Skipped.
     *
     * @param <i>unknown</i> <i>unknown parameters</i>
     */
    UNKNOWN(0) {
        @Override
        MessageUnknown createMessage() {
            return new MessageUnknown();
        }
    },
    /**
     * Message to ABORT all operations (like ERROR) and SHUTDOWN.
     *
     * @param <i>unknown</i> <i>unknown parameters</i>
     */
    ABORT(1) {
        @Override
        MessageAbort createMessage() {
            return new MessageAbort();
        }
    },
    /**
     * message with log text to server
     *
     * @param obj[0] groupId (<tt>int</tt>)
     * @param obj[1] group nodeId (<tt>int</tt>)
     * @param obj[2] log text (<tt>String</tt>)
     */
    LOG(4) {
        @Override
        MessageLog createMessage() {
            return new MessageLog();
        }
    },
    /**
     * sent by new-Client to Server with <b>new client connection data</b>
     *
     * @param obj[0] listen-on port of new-Client (<tt>int</tt>)
     * @param obj[1] global ids of new-Client threads (<tt>int[]</tt>)
     *
     * @see MessageTypes#HELLO_RESPONSE
     */
    HELLO(10) {
        @Override
        MessageHello createMessage() {
            return new MessageHello();
        }
    },
    /**
     * an answer to HELLO, sent by Server to new-Client - acknowledgment
     *
     * @param obj[0] physical id
     * @param obj[1] parent physicalId
     * @param obj[2] node hostname read by node0
     *
     * @see MessageTypes#HELLO
     * @see MessageTypes#HELLO_INFORM
     */
    HELLO_RESPONSE(11) {
        @Override
        MessageHelloResponse createMessage() {
            return new MessageHelloResponse();
        }
    },
    /**
     * message to all nodes (using BROADCAST) with information about new node in
     * calculations
     *
     * @param obj[0] new-Client physicalId (<tt>int</tt>)
     * @param obj[1] new-Client parent physicalId (<tt>int</tt>)
     * @param obj[2] new-Client nodeIds (<tt>int[]</tt>)
     * @param obj[3] new-Client host (<tt>String</tt>)
     * @param obj[4] new-Client port (<tt>int</tt>)
     *
     * @see MessageTypes#HELLO_BONJOUR
     */
    HELLO_INFORM(12) {
        @Override
        MessageHelloInform createMessage() {
            return new MessageHelloInform();
        }
    },
    /**
     * message to new-Client by all nodes with information about old-Client
     * nodeId
     *
     * @param obj[0] old-Client physicalId (<tt>int</tt>)
     * @param obj[1] old-Client nodeIds (<tt>int[]</tt>)
     *
     * @see MessageTypes#HELLO_INFORM
     */
    HELLO_BONJOUR(13) {
        @Override
        MessageHelloBonjour createMessage() {
            return new MessageHelloBonjour();
        }
    },
    /**
     * sent to server when all nodes with physicalNodeId less than current
     * <i>welcomes</i> this node
     *
     * @param <i>none</i> <i>without parameters</i>
     *
     * @see MessageTypes#HELLO_BONJOUR
     * @see MessageTypes#HELLO_GO
     */
    HELLO_COMPLETED(14) {
        @Override
        MessageHelloCompleted createMessage() {
            return new MessageHelloCompleted();
        }
    },
    /**
     * message to all nodes (using BROADCAST) to run calculations
     *
     * @param <i>none</i> <i>without parameters</i>
     *
     * @see MessageTypes#HELLO_COMPLETED
     */
    HELLO_GO(15) {
        @Override
        MessageHelloGo createMessage() {
            return new MessageHelloGo();
        }
    },
    /**
     * message sent by Client, when it finished all calculations
     *
     * @param <i>none</i> <i>without parameters</i>
     */
    FINISHED(16) {
        @Override
        MessageFinished createMessage() {
            return new MessageFinished();
        }
    },
    /**
     * message sent by server to clients to shutdown networker.
     *
     * @param <i>none</i> <i>without parameters</i>
     */
    FINISH_COMPLETED(17) {
        @Override
        MessageFinishCompleted createMessage() {
            return new MessageFinishCompleted();
        }
    },
    /**
     * after sending SYNC_WAIT Server collects it on specified group. When all
     * nodes in group sent that message, Server sends SYNC_GO
     *
     * @param obj[0] groupId (<tt>int</tt>)
     *
     * @see MessageTypes#SYNC_GO
     */
    SYNC_WAIT(20) {
        @Override
        MessageSyncWait createMessage() {
            return new MessageSyncWait();
        }
    },
    /**
     * message to all nodes (implicity broadcast) to continue calculations
     *
     * @param obj[0] groupId (<tt>int</tt>)
     *
     * @see MessageTypes#SYNC_WAIT
     */
    SYNC_GO(21) {
        @Override
        MessageSyncGo createMessage() {
            return new MessageSyncGo();
        }
    },
    /**
     * <b>Currently not used!</b>
     * 
     * after sending NODES_SYNC_WAIT Server collects it on globalNodeIds. When
     * all nodes from that `array` sent that message, Server sends NODES_SYNC_GO
     *
     * @param obj[0] global nodes ids (<tt>int[]</tt>)
     *
     * @see MessageTypes#NODES_SYNC_GO
     */
    @Deprecated
    NODES_SYNC_WAIT(22) {
        @Override
        MessageNodesSyncWait createMessage() {
            return new MessageNodesSyncWait();
        }
    },
    /**
     * <b>Currently not used!</b>
     * 
     * message to all nodes (implicity broadcast) to continue calculations
     *
     * @param obj[0] global nodes ids (<tt>int[]</tt>)
     *
     * @see MessageTypes#NODES_SYNC_WAIT
     */
    @Deprecated
    NODES_SYNC_GO(23) {
        @Override
        MessageNodesSyncGo createMessage() {
            return new MessageNodesSyncGo();
        }
    },
    /**
     * message to node about waiting in calculations, when received when waiting
     * - continue calculations
     *
     * @param obj[0] global node id of node that sends message (<tt>int</tt>)
     * @param obj[1] global node id of node that receives message (<tt>int</tt>)
     */
    NODE_SYNC(24) {
        @Override
        MessageNodeSync createMessage() {
            return new MessageNodeSync();
        }
    },
    /**
     * Question about groupId and master physicalId of group
     *
     * @param obj[0] groupName (<tt>String</tt>)
     *
     * @see MessageTypes#GROUP_JOIN_ANSWER
     */
    GROUP_JOIN_QUERY(30) {
        @Override
        MessageGroupJoinQuery createMessage() {
            return new MessageGroupJoinQuery();
        }
    },
    /**
     * Answer from Server with groupId and master global physicalId
     *
     * @param obj[0] groupName (<tt>String</tt>)
     * @param obj[1] groupId (<tt>int</tt>)
     * @param obj[2] master physicalId (<tt>int</tt>)
     */
    GROUP_JOIN_ANSWER(31) {
        @Override
        MessageGroupJoinAnswer createMessage() {
            return new MessageGroupJoinAnswer();
        }
    },
    /**
     * message from node to the group master with request to join into the
     * desired group
     *
     * @param obj[0] groupId (<tt>int</tt>)
     * @param obj[1] global nodeId (<tt>int</tt>)
     *
     * @see MessageTypes#GROUP_JOIN_RESPONSE
     * @see MessageTypes#GROUP_JOIN_INFORM
     */
    GROUP_JOIN_REQUEST(32) {
        @Override
        MessageGroupJoinRequest createMessage() {
            return new MessageGroupJoinRequest();
        }
    },
    /**
     * message from group master to the node with groupNodeId
     *
     * @param obj[0] groupId (<tt>int</tt>)
     * @param obj[1] global nodeId (<tt>int</tt>)
     * @param obj[2] group nodeId (<tt>int</tt>)
     * @param obj[3] parent physicalId (<tt>int</tt>)
     *
     * @see MessageTypes#GROUP_JOIN_REQUEST
     */
    GROUP_JOIN_RESPONSE(33) {
        @Override
        MessageGroupJoinResponse createMessage() {
            return new MessageGroupJoinResponse();
        }
    },
    /**
     * message to all nodes in group (using BROADCAST) with information about
     * new node in group
     *
     * @param obj[0] groupId (<tt>int</tt>)
     * @param obj[1] new-Node global nodeId (<tt>int</tt>)
     * @param obj[2] new-Node group nodeId (<tt>int</tt>)
     * @param obj[3] new-Node parent physicalId (<tt>int</tt>)
     *
     * @see MessageTypes#GROUP_JOIN_BONJOUR
     */
    GROUP_JOIN_INFORM(34) {
        @Override
        MessageGroupJoinInform createMessage() {
            return new MessageGroupJoinInform();
        }
    },
    /**
     * message from all nodes in group to new-Node
     *
     * @param obj[0] groupId (<tt>int</tt>)
     * @param obj[1] new-Node group nodeId (<tt>int</tt>)
     * @param obj[2] old-Node global nodeIds (<tt>int[]</tt>)
     * @param obj[3] old-Node group nodeIds (<tt>int[]</tt>)
     *
     * @see MessageTypes#GROUP_JOIN_INFORM
     */
    GROUP_JOIN_BONJOUR(35) {
        @Override
        MessageGroupJoinBonjour createMessage() {
            return new MessageGroupJoinBonjour();
        }
    },
    /**
     * message from node to node asking about Storage value
     *
     * @param obj[0] globalNodeId of sender node (<tt>int</tt>)
     * @param obj[1] globalNodeId of receiver node (<tt>int</tt>)
     * @param obj[2] variableName (<tt>String</tt>)
     *
     * @see MessageTypes#VALUE_ASYNC_GET_RESPONSE
     */
    VALUE_ASYNC_GET_REQUEST(42) {
        @Override
        MessageValueAsyncGetRequest createMessage() {
            return new MessageValueAsyncGetRequest();
        }
    },
    /**
     * message from node to node asking about Storage value
     *
     * @param obj[0] globalNodeId of sender node (<tt>int</tt>)
     * @param obj[1] globalNodeId of receiver node (<tt>int</tt>)
     * @param obj[2] indexes (<tt>int[]</tt>)
     * @param obj[3] variableName (<tt>String</tt>)
     *
     * @see MessageTypes#VALUE_ASYNC_GET_RESPONSE
     */
    VALUE_ASYNC_GET_REQUEST_INDEXES(45) {
        @Override
        MessageValueAsyncGetRequestIndexes createMessage() {
            return new MessageValueAsyncGetRequestIndexes();
        }
    },
    /**
     * message from node to node with variable value (response). information
     * about which variable is returned is stored in replyTo field
     *
     * @param obj[0] globalNodeId of receiving node (<tt>int</tt>)
     * @param obj[1] variableValue (<tt>byte[]</tt> - serialized object data)
     *
     * @see MessageTypes#VALUE_ASYNC_GET_REQUEST
     */
    VALUE_ASYNC_GET_RESPONSE(43) {
        @Override
        MessageValueAsyncGetResponse createMessage() {
            return new MessageValueAsyncGetResponse();
        }
    },
    /**
     * message from node to node with variable value to put
     *
     * @param obj[0] globalNodeId of remote node (<tt>int</tt>)
     * @param obj[1] variableName (<tt>String</tt>)
     * @param obj[2] variableValue (<tt>byte[]</tt> - serialized object data)
     *
     * @see MessageTypes#VALUE_GET_REQUEST
     * @see MessageTypes#VALUE_BROADCAST
     */
    VALUE_PUT(50) {
        @Override
        MessageValuePut createMessage() {
            return new MessageValuePut();
        }
    },
    /**
     * message from node to node with variable value to put
     *
     * @param obj[0] globalNodeId of remote node (<tt>int</tt>)
     * @param obj[1] variableName (<tt>String</tt>)
     * @param obj[2] indexes (<tt>int[]</tt>)
     * @param obj[3] variableValue (<tt>byte[]</tt> - serialized object data)
     *
     * @see MessageTypes#VALUE_GET_REQUEST
     * @see MessageTypes#VALUE_BROADCAST
     */
    VALUE_PUT_INDEXES(52) {
        @Override
        MessageValuePutIndexes createMessage() {
            return new MessageValuePutIndexes();
        }
    },
    /**
     * message from node to all nodes (broadcast) with variable value to put
     *
     * @param obj[0] groupId (<tt>int</tt>)
     * @param obj[1] variableName (<tt>String</tt>)
     * @param obj[2] variableValue (<tt>byte[]</tt> - serialized object data)
     *
     * @see MessageTypes#VALUE_PUT
     */
    VALUE_BROADCAST(51) {
        @Override
        MessageValueBroadcast createMessage() {
            return new MessageValueBroadcast();
        }
    };
    /* **************************************************** */
    private static final Map<Byte, MessageTypes> map;
    private final byte id;

    static {
        Map<Byte, MessageTypes> lmap = new HashMap<>();
        for (MessageTypes type : MessageTypes.values()) {
            lmap.put(type.getId(), type);
        }
        if (lmap.size() != values().length) {
            throw new InstantiationError("Two MessageType items has the same value");
        }
        map = Collections.unmodifiableMap(lmap);
    }

    public static MessageTypes valueOf(byte id) {
        MessageTypes type = map.get(id);
        return type == null ? UNKNOWN : type;
    }

    MessageTypes(int id) {
        this.id = (byte) id;
    }

    public byte getId() {
        return id;
    }

    /**
     * Creates Message object that is associated with Enum value.
     *
     * @return Message object
     */
    abstract Message createMessage();
}
