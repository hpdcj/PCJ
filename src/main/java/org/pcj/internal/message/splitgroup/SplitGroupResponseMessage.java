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
import java.util.List;
import java.util.Map;
import org.pcj.PcjRuntimeException;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.MessageType;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public final class SplitGroupResponseMessage extends Message {

    private int groupId;
    private int round;
    private Map<Integer, List<Integer>> threadGroupIdMap;

    public SplitGroupResponseMessage() {
        super(MessageType.SPLIT_GROUP_RESPONSE);
    }

    public SplitGroupResponseMessage(int groupId, int round, Map<Integer, List<Integer>> threadGroupIdMap) {
        this();

        this.groupId = groupId;
        this.round = round;
        this.threadGroupIdMap = threadGroupIdMap;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(groupId);
        out.writeInt(round);
        out.writeObject(threadGroupIdMap);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onReceive(SocketChannel sender, MessageDataInputStream in) throws IOException {
        groupId = in.readInt();
        round = in.readInt();

        try {
            threadGroupIdMap = (Map<Integer, List<Integer>>) in.readObject();
        } catch (Exception ex) {
            throw new PcjRuntimeException("Unable to read nodeInfoByPhysicalId", ex);
        }

        NodeData nodeData = InternalPCJ.getNodeData();

        InternalCommonGroup commonGroup = nodeData.getCommonGroupById(groupId);

        SplitGroupStates states = commonGroup.getSplitGroupStates();
        SplitGroupStates.State state = states.getOrCreate(round, commonGroup);
        state.createGroups(commonGroup,  threadGroupIdMap);
    }
}
