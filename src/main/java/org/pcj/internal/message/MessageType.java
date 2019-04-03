/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message;

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

    UNKNOWN(MessageUnknown::new),
    HELLO(HelloMessage::new),
    HELLO_INFORM(HelloInformMessage::new),
    HELLO_BONJOUR(HelloBonjourMessage::new),
    HELLO_COMPLETED(HelloCompletedMessage::new),
    HELLO_GO(HelloGoMessage::new),
    ALIVE(AliveMessage::new),
    ABORT(AbortMessage::new),
    BYE(ByeNotifyMessage::new),
    BYE_COMPLETED(ByeCompletedMessage::new),
    GROUP_BARRIER_WAITING(GroupBarrierWaitingMessage::new),
    GROUP_BARRIER_GO(GroupBarrierGoMessage::new),
    PEER_BARRIER(PeerBarrierMessage::new),
    GROUP_JOIN_QUERY(GroupQueryMessage::new),
    GROUP_JOIN_ANSWER(GroupQueryAnswerMessage::new),
    GROUP_JOIN_REQUEST(GroupJoinRequestMessage::new),
    GROUP_JOIN_INFORM(GroupJoinInformMessage::new),
    GROUP_JOIN_CONFIRM(GroupJoinConfirmMessage::new),
    GROUP_JOIN_RESPONSE(GroupJoinResponseMessage::new),
    VALUE_GET_REQUEST(ValueGetRequestMessage::new),
    VALUE_GET_RESPONSE(ValueGetResponseMessage::new),
    VALUE_PUT_REQUEST(ValuePutRequestMessage::new),
    VALUE_PUT_RESPONSE(ValuePutResponseMessage::new),
    VALUE_ACCUMULATE_REQUEST(ValueAccumulateRequestMessage::new),
    VALUE_ACCUMULATE_RESPONSE(ValueAccumulateResponseMessage::new),
    VALUE_BROADCAST_REQUEST(BroadcastRequestMessage::new),
    VALUE_BROADCAST_BYTES(BroadcastBytesMessage::new),
    VALUE_BROADCAST_INFORM(BroadcastInformMessage::new),
    VALUE_BROADCAST_RESPONSE(BroadcastResponseMessage::new),
    COLLECT_REQUEST(CollectRequestMessage::new),
    COLLECT_VALUE(CollectValueMessage::new),
    COLLECT_RESPONSE(CollectResponseMessage::new),
    REDUCE_REQUEST(ReduceRequestMessage::new),
    REDUCE_VALUE(ReduceValueMessage::new),
    REDUCE_RESPONSE(ReduceResponseMessage::new),
    ASYNC_AT_REQUEST(AsyncAtRequestMessage::new),
    ASYNC_AT_RESPONSE(AsyncAtResponseMessage::new),
    ;
    /* **************************************************** */
    private static final Map<Byte, MessageType> map;

    static {
        map = new HashMap<>(values().length, 1.0f);
        for (MessageType type : values()) {
            map.put((byte) type.ordinal(), type);
        }
        if (map.size() != values().length) {
            throw new InstantiationError("At least two MessageType items have the same value");
        }
    }

    private final Supplier<? extends Message> constructor;

    MessageType(Supplier<? extends Message> constructor) {
        this.constructor = constructor;
    }

    /**
     * Creates Message object that is associated with id
     *
     * @return Message object
     */
    final public static Message createMessage(byte messageTypeId) {
        MessageType type = map.get(messageTypeId);
        Supplier<? extends Message> constructor = (type == null ? UNKNOWN.constructor : type.constructor);
        return constructor.get();
    }

    final public byte getId() {
        return (byte) ordinal();
    }
}
