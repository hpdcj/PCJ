/* 
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
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
    PEER_BARRIER((byte) 12) {
        @Override
        public MessagePeerBarrier create() {
            return new MessagePeerBarrier();
        }
    },
    GROUP_JOIN_QUERY((byte) 20) {
        @Override
        public MessageGroupJoinQuery create() {
            return new MessageGroupJoinQuery();
        }
    },
    GROUP_JOIN_ANSWER((byte) 21) {
        @Override
        public MessageGroupJoinAnswer create() {
            return new MessageGroupJoinAnswer();
        }
    },
    GROUP_JOIN_REQUEST((byte) 22) {
        @Override
        public MessageGroupJoinRequest create() {
            return new MessageGroupJoinRequest();
        }
    },
    GROUP_JOIN_INFORM((byte) 23) {
        @Override
        public MessageGroupJoinInform create() {
            return new MessageGroupJoinInform();
        }
    },
    GROUP_JOIN_CONFIRM((byte) 24) {
        @Override
        public MessageGroupJoinConfirm create() {
            return new MessageGroupJoinConfirm();
        }
    },
    GROUP_JOIN_RESPONSE((byte) 25) {
        @Override
        public MessageGroupJoinResponse create() {
            return new MessageGroupJoinResponse();
        }
    },
    VALUE_GET_REQUEST((byte) 30) {
        @Override
        public MessageValueGetRequest create() {
            return new MessageValueGetRequest();
        }
    },
    VALUE_GET_RESPONSE((byte) 31) {
        @Override
        public MessageValueGetResponse create() {
            return new MessageValueGetResponse();
        }
    },
    VALUE_PUT_REQUEST((byte) 32) {
        @Override
        public MessageValuePutRequest create() {
            return new MessageValuePutRequest();
        }
    },
    VALUE_PUT_RESPONSE((byte) 33) {
        @Override
        public MessageValuePutResponse create() {
            return new MessageValuePutResponse();
        }
    },
    VALUE_BROADCAST_REQUEST((byte) 34) {
        @Override
        public MessageValueBroadcastRequest create() {
            return new MessageValueBroadcastRequest();
        }
    },
    VALUE_BROADCAST_BYTES((byte) 35) {
        @Override
        public MessageValueBroadcastBytes create() {
            return new MessageValueBroadcastBytes();
        }
    },
    VALUE_BROADCAST_INFORM((byte) 36) {
        @Override
        public MessageValueBroadcastInform create() {
            return new MessageValueBroadcastInform();
        }
    },
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
