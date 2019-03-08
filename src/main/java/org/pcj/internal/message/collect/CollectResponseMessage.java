/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message.collect;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.channels.SocketChannel;
import java.util.Comparator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.PrimitiveTypes;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.MessageType;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class CollectResponseMessage<T> extends Message {
    private int groupId;
    private int requestNum;
    private int requesterThreadId;
    private Map<Integer, T> valueMap;
    private Queue<Exception> exceptions;

    public CollectResponseMessage() {
        super(MessageType.COLLECT_RESPONSE);
    }

    CollectResponseMessage(int groupId, Map<Integer, T> valueMap, int requestNum, int requesterThreadId, Queue<Exception> exceptions) {
        this();

        this.groupId = groupId;
        this.valueMap = valueMap;
        this.requestNum = requestNum;
        this.requesterThreadId = requesterThreadId;
        this.exceptions = exceptions;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(groupId);
        out.writeInt(requestNum);
        out.writeInt(requesterThreadId);
        boolean exceptionOccurred = ((exceptions != null) && (!exceptions.isEmpty()));
        out.writeBoolean(exceptionOccurred);
        if (exceptionOccurred) {
            out.writeObject(exceptions);
        } else {
            out.writeObject(valueMap);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onReceive(SocketChannel sender, MessageDataInputStream in) throws IOException {
        groupId = in.readInt();
        requestNum = in.readInt();
        requesterThreadId = in.readInt();

        boolean exceptionOccurred = in.readBoolean();
        try {
            if (!exceptionOccurred) {
                valueMap = (Map<Integer, T>) in.readObject();
            } else {
                exceptions = (Queue<Exception>) in.readObject();
            }
        } catch (Exception ex) {
            exceptions = new ConcurrentLinkedQueue<>();
            exceptions.add(ex);
        }

        NodeData nodeData = InternalPCJ.getNodeData();
        InternalCommonGroup commonGroup = nodeData.getCommonGroupById(groupId);

        CollectStates states = commonGroup.getCollectStates();
        CollectStates.State<T> state = (CollectStates.State<T>) states.remove(requestNum, requesterThreadId);

        Class<?> clazz = state.getValueClass();
        T valueArray = (T) convertMapToArray(clazz);

        state.signal(valueArray, exceptions);
    }

    private Object convertMapToArray(Class<?> clazz) {
        if (PrimitiveTypes.isPrimitiveClass(clazz)) {
            int size = valueMap.size();
            Object primitiveArray = Array.newInstance(clazz, size);

            for (int i = 0; i < size; i++) {
                T t = valueMap.get(i);

                if (clazz == double.class) Array.setDouble(primitiveArray, i, (Double) t);
                else if (clazz == float.class) Array.setFloat(primitiveArray, i, (Float) t);
                else if (clazz == long.class) Array.setLong(primitiveArray, i, (Long) t);
                else if (clazz == int.class) Array.setInt(primitiveArray, i, (Integer) t);
                else if (clazz == short.class) Array.setShort(primitiveArray, i, (Short) t);
                else if (clazz == char.class) Array.setChar(primitiveArray, i, (Character) t);
                else if (clazz == byte.class) Array.setByte(primitiveArray, i, (Byte) t);
                else if (clazz == boolean.class) Array.setBoolean(primitiveArray, i, (Boolean) t);
            }
            return primitiveArray;
        } else {
            return valueMap.entrySet()
                           .stream()
                           .sorted(Comparator.comparing(Map.Entry::getKey))
                           .map(Map.Entry::getValue)
                           .toArray(size -> (Object[]) Array.newInstance(clazz, size));
        }
    }


}
