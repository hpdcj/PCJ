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
public enum MessageType {

    /**
     * @see MessageHello
     */
    HELLO((byte) 1) {
        @Override
        public MessageHello create() {
            return new MessageHello();
        }
    },
    /**
     * @see MessageHelloInform
     */
    HELLO_INFORM((byte) 2) {
        @Override
        public MessageHelloInform create() {
            return new MessageHelloInform();
        }
    },
    /**
     * @see MessageHelloBonjour
     */
    HELLO_BONJOUR((byte) 3) {
        @Override
        public MessageHelloBonjour create() {
            return new MessageHelloBonjour();
        }
    },
    /**
     * @see MessageHelloBonjour
     */
    HELLO_COMPLETED((byte) 4) {
        @Override
        public MessageHelloCompleted create() {
            return new MessageHelloCompleted();
        }
    },
    /**
     * @see MessageHelloGo
     */
    HELLO_GO((byte) 5) {
        @Override
        public MessageHelloGo create() {
            return new MessageHelloGo();
        }
    },
    /**
     * @see MessageHelloBonjour
     */
    BYE((byte) 6) {
        @Override
        public MessageBye create() {
            return new MessageBye();
        }
    },
    /**
     * @see MessageHelloGo
     */
    BYE_COMPLETED((byte) 7) {
        @Override
        public MessageByeCompleted create() {
            return new MessageByeCompleted();
        }
    },
    /**
     * @see MessageGroupBarrierWaiting
     */
    GROUP_BARRIER_WAITING((byte) 10) {
        @Override
        public MessageGroupBarrierWaiting create() {
            return new MessageGroupBarrierWaiting();
        }
    },
    /**
     * @see MessageGroupBarrierGo
     */
    GROUP_BARRIER_GO((byte) 11) {
        @Override
        public MessageGroupBarrierGo create() {
            return new MessageGroupBarrierGo();
        }
    },
    VALUE_GET_REQUEST((byte) 20) {
        @Override
        public MessageValueGetRequest create() {
            return new MessageValueGetRequest();
        }
    },
    VALUE_GET_RESPONSE((byte) 21) {
        @Override
        public MessageValueGetResponse create() {
            return new MessageValueGetResponse();
        }
    },
    //    /**
    //     * after sending SYNC_WAIT Server collects it on specified group. When all
    //     * nodes in group sent that message, Server sends SYNC_GO
    //     *
    //     * @param obj[0] groupId (<tt>int</tt>)
    //     *
    //     * @see MessageType#SYNC_GO
    //     */
    //    SYNC_WAIT(20) {
    //                @Override
    //                MessageSyncWait createMessage() {
    //                    return new MessageSyncWait();
    //                }
    //            },
    //    /**
    //     * message to all nodes (implicity broadcast) to continue calculations
    //     *
    //     * @param obj[0] groupId (<tt>int</tt>)
    //     *
    //     * @see MessageType#SYNC_WAIT
    //     */
    //    SYNC_GO(21) {
    //                @Override
    //                MessageSyncGo createMessage() {
    //                    return new MessageSyncGo();
    //                }
    //            },
    //    /**
    //     * <b>Currently not used!</b>
    //     *
    // after sending THREADS_SYNC_WAIT Server collects it on globalNodeIds. When
    //     * all nodes from that `array` sent that message, Server sends
    //     * THREADS_SYNC_GO
    //     *
    //     * @param obj[0] global threads ids (<tt>int[]</tt>)
    //     *
    //     * @see MessageType#THREADS_SYNC_GO
    //     */
    //    @Deprecated
    //    THREADS_SYNC_WAIT(22) {
    //                @Override
    //                MessageThreadsSyncWait createMessage() {
    //                    return new MessageThreadsSyncWait();
    //                }
    //            },
    //    /**
    //     * <b>Currently not used!</b>
    //     *
    //     * message to all nodes (implicity broadcast) to continue calculations
    //     *
    //     * @param obj[0] global threads ids (<tt>int[]</tt>)
    //     *
    //     * @see MessageType#THREADS_SYNC_WAIT
    //     */
    //    @Deprecated
    //    THREADS_SYNC_GO(23) {
    //                @Override
    //                MessageThreadsSyncGo createMessage() {
    //                    return new MessageThreadsSyncGo();
    //                }
    //            },
    //    /**
    //     * message to thread about waiting in calculations, when received when
    //     * waiting - continue calculations
    //     *
    //     * @param obj[0] global thread id of thread that sends message
    //     * (<tt>int</tt>)
    //     * @param obj[1] global thread id of thread that receives message
    //     * (<tt>int</tt>)
    //     */
    //    THREAD_PAIR_SYNC(24) {
    //                @Override
    //                MessageThreadPairSync createMessage() {
    //                    return new MessageThreadPairSync();
    //                }
    //            },
    //    /**
    //     * Question about groupId and master physicalId of group
    //     *
    //     * @param obj[0] groupName (<tt>String</tt>)
    //     *
    //     * @see MessageType#GROUP_JOIN_ANSWER
    //     */
    //    GROUP_JOIN_QUERY(30) {
    //                @Override
    //                MessageGroupJoinQuery createMessage() {
    //                    return new MessageGroupJoinQuery();
    //                }
    //            },
    //    /**
    //     * Answer from Server with groupId and master global physicalId
    //     *
    //     * @param obj[0] groupName (<tt>String</tt>)
    //     * @param obj[1] groupId (<tt>int</tt>)
    //     * @param obj[2] master physicalId (<tt>int</tt>)
    //     */
    //    GROUP_JOIN_ANSWER(31) {
    //                @Override
    //                MessageGroupJoinAnswer createMessage() {
    //                    return new MessageGroupJoinAnswer();
    //                }
    //            },
    //    /**
    //     * message from thread to the group master with request to join into the
    //     * desired group
    //     *
    //     * @param obj[0] groupId (<tt>int</tt>)
    //     * @param obj[1] global threadId (<tt>int</tt>)
    //     *
    //     * @see MessageType#GROUP_JOIN_RESPONSE
    //     * @see MessageType#GROUP_JOIN_INFORM
    //     */
    //    GROUP_JOIN_REQUEST(32) {
    //                @Override
    //                MessageGroupJoinRequest createMessage() {
    //                    return new MessageGroupJoinRequest();
    //                }
    //            },
    //    /**
    //     * message from group master to the node with groupThreadId
    //     *
    //     * @param obj[0] groupId (<tt>int</tt>)
    //     * @param obj[1] global threadId (<tt>int</tt>)
    //     * @param obj[2] group threadId (<tt>int</tt>)
    //     * @param obj[3] parent physicalId (<tt>int</tt>)
    //     *
    //     * @see MessageType#GROUP_JOIN_REQUEST
    //     */
    //    GROUP_JOIN_RESPONSE(33) {
    //                @Override
    //                MessageGroupJoinResponse createMessage() {
    //                    return new MessageGroupJoinResponse();
    //                }
    //            },
    //    /**
    //     * message to all nodes in group (using BROADCAST) with information about
    //     * new thread in group
    //     *
    //     * @param obj[0] groupId (<tt>int</tt>)
    //     * @param obj[1] new-thread global threadId (<tt>int</tt>)
    //     * @param obj[2] new-thread group threadId (<tt>int</tt>)
    //     * @param obj[3] new-thread parent physicalId (<tt>int</tt>)
    //     *
    //     * @see MessageType#GROUP_JOIN_BONJOUR
    //     */
    //    GROUP_JOIN_INFORM(34) {
    //                @Override
    //                MessageGroupJoinInform createMessage() {
    //                    return new MessageGroupJoinInform();
    //                }
    //            },
    //    /**
    //     * message from all nodes in group to new-thread
    //     *
    //     * @param obj[0] groupId (<tt>int</tt>)
    //     * @param obj[1] new-thread group threadId (<tt>int</tt>)
    //     * @param obj[2] old-thread global threadIds (<tt>int[]</tt>)
    //     * @param obj[3] old-thread group threadIds (<tt>int[]</tt>)
    //     *
    //     * @see MessageType#GROUP_JOIN_INFORM
    //     */
    //    GROUP_JOIN_BONJOUR(35) {
    //                @Override
    //                MessageGroupJoinBonjour createMessage() {
    //                    return new MessageGroupJoinBonjour();
    //                }
    //            },
    //    /**
    //     * message from one thread to another asking about Storage value
    //     *
    //     * @param obj[0] globalThreadId of sender thread (<tt>int</tt>)
    //     * @param obj[1] globalThreadId of receiver thread (<tt>int</tt>)
    //     * @param obj[2] indexes (<tt>int[]</tt>)
    //     * @param obj[3] variableName (<tt>String</tt>)
    //     *
    //     * @see MessageType#VALUE_ASYNC_GET_RESPONSE
    //     */
    //    VALUE_ASYNC_GET_REQUEST(45) {
    //                @Override
    //                MessageValueAsyncGetRequest createMessage() {
    //                    return new MessageValueAsyncGetRequest();
    //                }
    //            },
    //    /**
    //     * message from one thread to another thread with variable value (response).
    //     * information about which variable is returned is stored in replyTo field
    //     *
    //     * @param obj[0] globalThreadId of receiving thread (<tt>int</tt>)
    //     * @param obj[1] variableValue (<tt>byte[]</tt> - serialized object data)
    //     *
    //     * @see MessageType#VALUE_ASYNC_GET_REQUEST
    //     */
    //    VALUE_ASYNC_GET_RESPONSE(43) {
    //                @Override
    //                MessageValueAsyncGetResponse createMessage() {
    //                    return new MessageValueAsyncGetResponse();
    //                }
    //            },
    //    /**
    //     * message from one thread to another thread with variable value to put
    //     *
    //     * @param obj[0] globalThreadId of remote thread (<tt>int</tt>)
    //     * @param obj[1] variableName (<tt>String</tt>)
    //     * @param obj[2] indexes (<tt>int[]</tt>)
    //     * @param obj[3] variableValue (<tt>byte[]</tt> - serialized object data)
    //     *
    //     * @see MessageType#VALUE_GET_REQUEST
    //     * @see MessageType#VALUE_BROADCAST
    //     */
    //    VALUE_PUT(52) {
    //                @Override
    //                MessageValuePut createMessage() {
    //                    return new MessageValuePut();
    //                }
    //            },
    //    /**
    //     * message from one thread to all threads (broadcast) with variable value to
    //     * put
    //     *
    //     * @param obj[0] groupId (<tt>int</tt>)
    //     * @param obj[1] variableName (<tt>String</tt>)
    //     * @param obj[2] variableValue (<tt>byte[]</tt> - serialized object data)
    //     *
    //     * @see MessageType#VALUE_PUT
    //     */
    //    VALUE_BROADCAST(51) {
    //                @Override
    //                MessageValueBroadcast createMessage() {
    //                    return new MessageValueBroadcast();
    //                }
    //            },
    //    /**
    //     * message from one thread to other thread asking to perform CAS
    //     * (Compare-And-Set) operation. This operation is atomic. It checks if
    //     * stored value is equal to expected value, and if so, change value to new
    //     * value. It returns (previously) stored value.
    //     *
    //     * @param obj[0] globalThreadId of sender thread (<tt>int</tt>)
    //     * @param obj[1] globalThreadId of receiver thread (<tt>int</tt>)
    //     * @param obj[2] variableName (<tt>String</tt>)
    //     * @param obj[3] indexes (<tt>int[]</tt>)
    //     * @param obj[4] expectedValue (<tt>byte[]</tt> - serialized object data)
    //     * @param obj[5] newValue (<tt>byte[]</tt> - serialized object data)
    //     *
    //     * @see MessageType#VALUE_COMPARE_AND_SET_RESPONSE
    //     */
    //    VALUE_COMPARE_AND_SET_REQUEST(60) {
    //                @Override
    //                MessageValueCompareAndSetRequest createMessage() {
    //                    return new MessageValueCompareAndSetRequest();
    //                }
    //            },
    //    /**
    //     * response of the CAS (Compare-And-Set) operation
    //     *
    //     * @param obj[0] globalThreadId of receiving thread (<tt>int</tt>)
    //     * @param obj[1] variableValue (<tt>byte[]</tt> - serialized object data)
    //     *
    //     * @see MessageType#VALUE_COMPARE_AND_SET_REQUEST
    //     */
    //    VALUE_COMPARE_AND_SET_RESPONSE(62) {
    //                @Override
    //                MessageValueCompareAndSetResponse createMessage() {
    //                    return new MessageValueCompareAndSetResponse();
    //                }
    //            };
    /**
     * @see MessageUnknown
     */
    UNKNOWN((byte) -1) {
        @Override
        public MessageUnknown create() {
            return new MessageUnknown();
        }
    };
//    /* **************************************************** */
    private static final Map<Byte, MessageType> map;
    private final byte id;

    static {
        Map<Byte, MessageType> localMap = new HashMap<>(32, 1.0f);
        for (MessageType type : MessageType.values()) {
            localMap.put(type.getId(), type);
        }
        if (localMap.size() != values().length) {
            throw new InstantiationError("At least two MessageType items has the same value");
        }
        map = Collections.unmodifiableMap(localMap);
    }

    public static MessageType valueOf(byte id) {
        MessageType type = map.get(id);
        return type == null ? UNKNOWN : type;
    }

    MessageType(byte id) {
        this.id = id;
    }

    public byte getId() {
        return id;
    }

    /**
     * Creates Message object that is associated with Enum value.
     *
     * @return Message object
     */
    public abstract Message create();
}
