/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.pcj.Group;
import org.pcj.Storage;

/**
 * This class represents internal data for PCJ Thread.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class PcjThreadData {

    final private Storage storage;
    final private Group globalGroup;
    final private ConcurrentMap<Integer, Group> groupById;
    final private ConcurrentMap<String, Group> groupByName;
//

    PcjThreadData(Group globalGroup) {
        this.globalGroup = globalGroup;

        this.storage = new Storage();
        this.groupById = new ConcurrentHashMap<>();
        this.groupByName = new ConcurrentHashMap<>();

        this.addGroup(globalGroup);
    }

    void addGroup(Group group) {
        groupById.put(((InternalGroup) group).getGroupId(), group);
        groupByName.put(group.getGroupName(), group);
    }

//    InternalGroup createGroup(int groupNodeId, InternalGroup internalGroup) {
//        try {
//            Class<?> groupClass = getClass().getClassLoader().loadClass(Group.class.getCanonicalName());
//            Constructor<?> constructor = groupClass.getDeclaredConstructor(int.class, InternalGroup.class);
//            constructor.setAccessible(true);
//            return (InternalGroup) constructor.newInstance(groupNodeId, internalGroup);
//        } catch (final ClassNotFoundException | NoSuchMethodException |
//                InstantiationException | IllegalAccessException |
//                IllegalArgumentException | InvocationTargetException ex) {
//            throw new RuntimeException(ex);
//        }
//    }
//
//
//    Storage getStorage() {
//        return storage;
//    }
//
    /**
     * Stores Group but because of ClassLoader says that holds InternalGroup
     *
     * @return
     */
    InternalGroup getGlobalGroup() {
        return globalGroup;
    }

    public Storage getStorage() {
        return storage;
    }
//
//    /**
//     * Stores Group but because of ClassLoader says that holds InternalGroup
//     *
//     * @return the groups
//     */
//    Map<Integer, InternalGroup> getGroups() {
//        return groups;
//    }
//
//    /**
//     * Stores Group but because of ClassLoader says that holds InternalGroup
//     *
//     * @return the groupsByName
//     */
//    Map<String, InternalGroup> getGroupsByName() {
//        return groupsByName;
//    }

}
