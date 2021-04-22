/*
 * Copyright (c) 2011-2021, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.splitgroup;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Map;
import org.pcj.PcjRuntimeException;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.MessageType;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public final class SplitGroupRequestMessage extends Message {

    private int groupId;
    private int round;
    private Map<Integer, Integer> splitMap;
    private Map<Integer, Integer> orderingMap;


    public SplitGroupRequestMessage() {
        super(MessageType.SPLIT_GROUP_REQUEST);
    }

    public SplitGroupRequestMessage(int groupId, int round, Map<Integer, Integer> splitMap, Map<Integer, Integer> orderingMap) {
        this();

        this.groupId = groupId;
        this.round = round;
        this.splitMap = splitMap;
        this.orderingMap = orderingMap;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(groupId);
        out.writeInt(round);
        out.writeObject(splitMap);
        out.writeObject(orderingMap);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onReceive(SocketChannel sender, MessageDataInputStream in) throws IOException {
        groupId = in.readInt();
        round = in.readInt();
        try {
            splitMap = (Map<Integer, Integer>) in.readObject();
        } catch (Exception ex) {
            throw new PcjRuntimeException("Unable to read splitMap", ex);
        }
        try {
            orderingMap = (Map<Integer, Integer>) in.readObject();
        } catch (Exception ex) {
            throw new PcjRuntimeException("Unable to read orderingMap", ex);
        }

        InternalCommonGroup commonGroup = InternalPCJ.getNodeData().getCommonGroupById(groupId);

        SplitGroupStates states = commonGroup.getSplitGroupStates();
        SplitGroupStates.State state = states.getOrCreate(round, commonGroup);
        state.processPhysical(commonGroup, splitMap, orderingMap);
    }
}
