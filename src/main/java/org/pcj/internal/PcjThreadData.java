/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.pcj.Group;

/**
 * This class represents internal data for PCJ Thread.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class PcjThreadData {

    final private InternalStorage storage;
    final private InternalGroup globalGroup;
    final private ConcurrentMap<Integer, Group> groupById;
    final private ConcurrentMap<String, Group> groupByName;
//

    PcjThreadData(InternalGroup globalGroup) {
        this.globalGroup = globalGroup;

        this.storage = new InternalStorage();
        this.groupById = new ConcurrentHashMap<>();
        this.groupByName = new ConcurrentHashMap<>();

        this.addGroup(globalGroup);
    }

    void addGroup(InternalGroup group) {
        groupById.put(((InternalCommonGroup) group).getGroupId(), group);
        groupByName.put(group.getGroupName(), group);
    }

//    InternalCommonGroup createGroup(int groupNodeId, InternalCommonGroup internalGroup) {
//        try {
//            Class<?> groupClass = getClass().getClassLoader().loadClass(ThreadGroup.class.getCanonicalName());
//            Constructor<?> constructor = groupClass.getDeclaredConstructor(int.class, InternalCommonGroup.class);
//            constructor.setAccessible(true);
//            return (InternalCommonGroup) constructor.newInstance(groupNodeId, internalGroup);
//        } catch (final ClassNotFoundException | NoSuchMethodException |
//                InstantiationException | IllegalAccessException |
//                IllegalArgumentException | InvocationTargetException ex) {
//            throw new RuntimeException(ex);
//        }
//    }
//
//
//    InternalStorage getStorage() {
//        return storage;
//    }
//
    /**
     * Stores InternalGroup but because of ClassLoader says that holds InternalCommonGroup
     *
     * @return
     */
    Group getGlobalGroup() {
        return globalGroup;
    }

    public InternalStorage getStorage() {
        return storage;
    }

    public Group getGroupById(int groupId) {
        return groupById.get(groupId);
    }

    public Group getGroupByName(String name) {
        return groupByName.get(name);
    }

//    /**
//     * Stores ThreadGroup but because of ClassLoader says that holds InternalCommonGroup
//     *
//     * @return the groups
//     */
//    Map<Integer, InternalCommonGroup> getGroups() {
//        return groups;
//    }
//
//    /**
//     * Stores ThreadGroup but because of ClassLoader says that holds InternalCommonGroup
//     *
//     * @return the groupsByName
//     */
//    Map<String, InternalCommonGroup> getGroupsByName() {
//        return groupsByName;
//    }
}
