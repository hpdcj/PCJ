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

    PcjThreadData(InternalGroup globalGroup) {
        this.globalGroup = globalGroup;

        this.storage = new InternalStorage();
        this.groupById = new ConcurrentHashMap<>();

        this.addGroup(globalGroup);
    }

    void addGroup(InternalGroup group) {
        groupById.put(((InternalCommonGroup) group).getGroupId(), group);
    }

    public Group getGlobalGroup() {
        return globalGroup;
    }

    public InternalStorage getStorage() {
        return storage;
    }

    public Group getGroupById(int groupId) {
        return groupById.get(groupId);
    }

    public Group getGroupByName(String name) {
        return groupById.values().stream()
                .filter(groups -> name.equals(groups.getGroupName()))
                .findFirst().orElse(null);
    }
}
