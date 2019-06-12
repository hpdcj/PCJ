/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
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

    private final InternalStorages storages;
    private final InternalGroup globalGroup;
    private final ConcurrentMap<Integer, InternalGroup> groupById;

    PcjThreadData(InternalGroup globalGroup) {
        this.globalGroup = globalGroup;

        this.storages = new InternalStorages();
        this.groupById = new ConcurrentHashMap<>();

        this.addGroup(globalGroup);
    }

    public void addGroup(InternalGroup group) {
        groupById.put(group.getGroupId(), group);
    }

    public Group getGlobalGroup() {
        return globalGroup;
    }

    public InternalStorages getStorages() {
        return storages;
    }

    public InternalGroup getGroupById(int groupId) {
        return groupById.get(groupId);
    }

    InternalGroup getInternalGroupByName(String name) {
        return groupById.values()
                       .stream()
                       .filter(groups -> name.equals(groups.getName()))
                       .findFirst().orElse(null);
    }
}
