/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import org.pcj.Group;
import org.pcj.internal.storage.InternalStorage;
import org.pcj.internal.utils.CloneObject;

/**
 * This class represents internal data for PCJ Thread.
 * 
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
class PcjThreadLocalData {

    final private InternalStorage storage;
    final private InternalGroup globalGroup;
    final private Deserializer deserializer;
    final private Map<Integer, InternalGroup> groups;
    final private Map<String, InternalGroup> groupsByName;

    PcjThreadLocalData(InternalStorage storage,
            Map<Integer, InternalGroup> groups, Map<String, InternalGroup> groupsByName) {
        this.storage = storage;
        this.groups = groups;
        this.groupsByName = groupsByName;

        this.globalGroup = groups.get(0);
        this.deserializer = new Deserializer(storage);
    }

    void addGroup(InternalGroup group) {
        groups.put(group.getGroupId(), group);
        groupsByName.put(group.getGroupName(), group);
    }

    InternalGroup createGroup(int groupNodeId, InternalGroup internalGroup) {
        try {
            Class<?> groupClass = getClass().getClassLoader().loadClass(Group.class.getCanonicalName());
            Constructor<?> constructor = groupClass.getDeclaredConstructor(int.class, InternalGroup.class);
            constructor.setAccessible(true);
            return (InternalGroup) constructor.newInstance(groupNodeId, internalGroup);
        } catch (final ClassNotFoundException | NoSuchMethodException |
                InstantiationException | IllegalAccessException |
                IllegalArgumentException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    Deserializer getDeserializer() {
        return deserializer;
    }

    Object deserializeObject(byte[] bytes) {
        try {
            return CloneObject.deserialize(bytes);
        } catch (final IOException | ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    InternalStorage getStorage() {
        return storage;
    }

    /**
     * Stores Group but because of ClassLoader says that holds InternalGroup
     *
     * @return
     */
    InternalGroup getGlobalGroup() {
        return globalGroup;
    }

    /**
     * Stores Group but because of ClassLoader says that holds InternalGroup
     *
     * @return the groups
     */
    Map<Integer, InternalGroup> getGroups() {
        return groups;
    }

    /**
     * Stores Group but because of ClassLoader says that holds InternalGroup
     *
     * @return the groupsByName
     */
    Map<String, InternalGroup> getGroupsByName() {
        return groupsByName;
    }
}
