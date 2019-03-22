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
import org.pcj.internal.message.accumulate.ValueAccumulateRequestMessage;
import org.pcj.internal.message.accumulate.ValueAccumulateResponseMessage;
import org.pcj.internal.message.alive.AbortMessage;
import org.pcj.internal.message.alive.AliveMessage;
import org.pcj.internal.message.at.AsyncAtRequestMessage;
import org.pcj.internal.message.at.AsyncAtResponseMessage;
import org.pcj.internal.message.barrier.GroupBarrierGoMessage;
import org.pcj.internal.message.barrier.GroupBarrierWaitingMessage;
import org.pcj.internal.message.broadcast.BroadcastBytesMessage;
import org.pcj.internal.message.broadcast.BroadcastInformMessage;
import org.pcj.internal.message.broadcast.BroadcastRequestMessage;
import org.pcj.internal.message.broadcast.BroadcastResponseMessage;
import org.pcj.internal.message.bye.ByeCompletedMessage;
import org.pcj.internal.message.bye.ByeNotifyMessage;
import org.pcj.internal.message.collect.CollectRequestMessage;
import org.pcj.internal.message.collect.CollectResponseMessage;
import org.pcj.internal.message.collect.CollectValueMessage;
import org.pcj.internal.message.get.ValueGetRequestMessage;
import org.pcj.internal.message.get.ValueGetResponseMessage;
import org.pcj.internal.message.hello.HelloBonjourMessage;
import org.pcj.internal.message.hello.HelloCompletedMessage;
import org.pcj.internal.message.hello.HelloGoMessage;
import org.pcj.internal.message.hello.HelloInformMessage;
import org.pcj.internal.message.hello.HelloMessage;
import org.pcj.internal.message.join.GroupJoinConfirmMessage;
import org.pcj.internal.message.join.GroupJoinInformMessage;
import org.pcj.internal.message.join.GroupJoinRequestMessage;
import org.pcj.internal.message.join.GroupJoinResponseMessage;
import org.pcj.internal.message.join.GroupQueryAnswerMessage;
import org.pcj.internal.message.join.GroupQueryMessage;
import org.pcj.internal.message.peerbarrier.PeerBarrierMessage;
import org.pcj.internal.message.put.ValuePutRequestMessage;
import org.pcj.internal.message.put.ValuePutResponseMessage;
import org.pcj.internal.message.reduce.ReduceRequestMessage;
import org.pcj.internal.message.reduce.ReduceResponseMessage;
import org.pcj.internal.message.reduce.ReduceValueMessage;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public enum MessageType {

    HELLO((byte) 1, HelloMessage::new),
    HELLO_INFORM((byte) 2, HelloInformMessage::new),
    HELLO_BONJOUR((byte) 3, HelloBonjourMessage::new),
    HELLO_COMPLETED((byte) 4, HelloCompletedMessage::new),
    HELLO_GO((byte) 5, HelloGoMessage::new),
    ALIVE((byte) 6, AliveMessage::new),
    ABORT((byte) 7, AbortMessage::new),
    BYE((byte) 8, ByeNotifyMessage::new),
    BYE_COMPLETED((byte) 9, ByeCompletedMessage::new),
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
    VALUE_ACCUMULATE_REQUEST((byte) 34, ValueAccumulateRequestMessage::new),
    VALUE_ACCUMULATE_RESPONSE((byte) 35, ValueAccumulateResponseMessage::new),
    VALUE_BROADCAST_REQUEST((byte) 36, BroadcastRequestMessage::new),
    VALUE_BROADCAST_BYTES((byte) 37, BroadcastBytesMessage::new),
    VALUE_BROADCAST_INFORM((byte) 38, BroadcastInformMessage::new),
    VALUE_BROADCAST_RESPONSE((byte) 39, BroadcastResponseMessage::new),
    COLLECT_REQUEST((byte) 40, CollectRequestMessage::new),
    COLLECT_VALUE((byte) 41, CollectValueMessage::new),
    COLLECT_RESPONSE((byte) 42, CollectResponseMessage::new),
    REDUCE_REQUEST((byte) 43, ReduceRequestMessage::new),
    REDUCE_VALUE((byte) 44, ReduceValueMessage::new),
    REDUCE_RESPONSE((byte) 45, ReduceResponseMessage::new),
    ASYNC_AT_REQUEST((byte) 46, AsyncAtRequestMessage::new),
    ASYNC_AT_RESPONSE((byte) 47, AsyncAtResponseMessage::new),
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
