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
import org.pcj.internal.message.at.AsyncAtRequestMessage;
import org.pcj.internal.message.at.AsyncAtResponseMessage;
import org.pcj.internal.message.hello.MessageHello;
import org.pcj.internal.message.hello.MessageHelloBonjour;
import org.pcj.internal.message.hello.MessageHelloCompleted;
import org.pcj.internal.message.hello.MessageHelloGo;
import org.pcj.internal.message.hello.MessageHelloInform;
import org.pcj.internal.message.join.GroupQueryAnswerMessage;
import org.pcj.internal.message.join.GroupJoinConfirmMessage;
import org.pcj.internal.message.join.GroupJoinInformMessage;
import org.pcj.internal.message.join.GroupQueryMessage;
import org.pcj.internal.message.join.GroupJoinRequestMessage;
import org.pcj.internal.message.join.GroupJoinResponseMessage;
import org.pcj.internal.message.peerbarrier.PeerBarrierMessage;
import org.pcj.internal.message.broadcast.BroadcastValueBytesMessage;
import org.pcj.internal.message.broadcast.BroadcastValueInformMessage;
import org.pcj.internal.message.broadcast.BroadcastValueRequestMessage;
import org.pcj.internal.message.broadcast.BroadcastValueResponseMessage;
import org.pcj.internal.message.barrier.GroupBarrierGoMessage;
import org.pcj.internal.message.barrier.GroupBarrierWaitingMessage;
import org.pcj.internal.message.get.ValueGetRequestMessage;
import org.pcj.internal.message.get.ValueGetResponseMessage;
import org.pcj.internal.message.put.ValuePutRequestMessage;
import org.pcj.internal.message.put.ValuePutResponseMessage;

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
    GROUP_BARRIER_WAITING((byte) 10, GroupBarrierWaitingMessage::new),
    GROUP_BARRIER_GO((byte) 11, GroupBarrierGoMessage::new),
    PEER_BARRIER((byte) 12, PeerBarrierMessage::new),
    GROUP_JOIN_QUERY((byte) 20, GroupQueryMessage::new),
    GROUP_JOIN_ANSWER((byte) 21, GroupQueryAnswerMessage::new),
    GROUP_JOIN_REQUEST((byte) 22, GroupJoinRequestMessage::new),
    GROUP_JOIN_INFORM((byte) 23, GroupJoinInformMessage::new),
    GROUP_JOIN_CONFIRM((byte) 24, GroupJoinConfirmMessage::new),
    GROUP_JOIN_RESPONSE((byte) 25, GroupJoinResponseMessage::new),
    VALUE_GET_REQUEST((byte) 30, ValueGetRequestMessage::new),
    VALUE_GET_RESPONSE((byte) 31, ValueGetResponseMessage::new),
    VALUE_PUT_REQUEST((byte) 32, ValuePutRequestMessage::new),
    VALUE_PUT_RESPONSE((byte) 33, ValuePutResponseMessage::new),
    ASYNC_AT_REQUEST((byte) 34, AsyncAtRequestMessage::new),
    ASYNC_AT_RESPONSE((byte) 35, AsyncAtResponseMessage::new),
    VALUE_BROADCAST_REQUEST((byte) 36, BroadcastValueRequestMessage::new),
    VALUE_BROADCAST_BYTES((byte) 37, BroadcastValueBytesMessage::new),
    VALUE_BROADCAST_INFORM((byte) 38, BroadcastValueInformMessage::new),
    VALUE_BROADCAST_RESPONSE((byte) 39, BroadcastValueResponseMessage::new),
    UNKNOWN((byte) -1, MessageUnknown::new);
    //    /* **************************************************** */
    private static final Map<Byte, MessageType> map;

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

    private final byte id;
    private final Supplier<? extends Message> constructor;

    MessageType(byte id, Supplier<? extends Message> constructor) {
        this.id = id;
        this.constructor = constructor;
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

    final public byte getId() {
        return id;
    }
}
