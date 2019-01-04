/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
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
import java.util.function.Supplier;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public enum MessageType {

    HELLO((byte) 1, MessageHello::new),
    HELLO_INFORM((byte) 2, MessageHelloInform::new),
    HELLO_BONJOUR((byte) 3, MessageHelloBonjour::new),
    HELLO_COMPLETED((byte) 4, MessageHelloCompleted::new),
    HELLO_GO((byte) 5, MessageHelloGo::new),
    BYE((byte) 6, MessageBye::new),
    BYE_COMPLETED((byte) 7, MessageByeCompleted::new),
    GROUP_BARRIER_WAITING((byte) 10, MessageGroupBarrierWaiting::new),
    GROUP_BARRIER_GO((byte) 11, MessageGroupBarrierGo::new),
    PEER_BARRIER((byte) 12, MessagePeerBarrier::new),
    GROUP_JOIN_QUERY((byte) 20, MessageGroupJoinQuery::new),
    GROUP_JOIN_ANSWER((byte) 21, MessageGroupJoinAnswer::new),
    GROUP_JOIN_REQUEST((byte) 22, MessageGroupJoinRequest::new),
    GROUP_JOIN_INFORM((byte) 23, MessageGroupJoinInform::new),
    GROUP_JOIN_CONFIRM((byte) 24, MessageGroupJoinConfirm::new),
    GROUP_JOIN_RESPONSE((byte) 25, MessageGroupJoinResponse::new),
    VALUE_GET_REQUEST((byte) 30, MessageValueGetRequest::new),
    VALUE_GET_RESPONSE((byte) 31, MessageValueGetResponse::new),
    VALUE_PUT_REQUEST((byte) 32, MessageValuePutRequest::new),
    VALUE_PUT_RESPONSE((byte) 33, MessageValuePutResponse::new),
    ASYNC_AT_REQUEST((byte) 34, MessageAsyncAtRequest::new),
    ASYNC_AT_RESPONSE((byte) 35, MessageAsyncAtResponse::new),
    VALUE_BROADCAST_REQUEST((byte) 36, MessageValueBroadcastRequest::new),
    VALUE_BROADCAST_BYTES((byte) 37, MessageValueBroadcastBytes::new),
    VALUE_BROADCAST_INFORM((byte) 38, MessageValueBroadcastInform::new),
    VALUE_BROADCAST_RESPONSE((byte) 39, MessageValueBroadcastResponse::new),
    UNKNOWN((byte) -1, MessageUnknown::new);
    //    /* **************************************************** */
    private static final Map<Byte, MessageType> map;
    private final byte id;
    private final Supplier<? extends Message> constructor;

    static {
        Map<Byte, MessageType> localMap = new HashMap<>(40, 1.0f);
        for (MessageType type : MessageType.values()) {
            localMap.put(type.getId(), type);
        }
        if (localMap.size() != values().length) {
            throw new InstantiationError("At least two MessageType items has the same value");
        }
        map = Collections.unmodifiableMap(localMap);
    }

    /**
     * Creates Message object that is associated with id
     *
     * @return Message object
     */
    final public static Message createMessage(byte messageTypeId) {
        MessageType type = map.get(messageTypeId);
        Supplier<? extends Message> constructor = type == null ? UNKNOWN.constructor : type.constructor;
        return constructor.get();
    }

    MessageType(byte id, Supplier<? extends Message> constructor) {
        this.id = id;
        this.constructor = constructor;
    }

    final public byte getId() {
        return id;
    }
}
