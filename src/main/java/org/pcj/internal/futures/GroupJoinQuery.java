/* 
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.futures;

/**
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class GroupJoinQuery {

    private final WaitObject waitObject;
    private int groupId;
    private int groupMasterId;

    public GroupJoinQuery() {
        waitObject = new WaitObject();
    }

    public WaitObject getWaitObject() {
        return waitObject;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public int getGroupMasterId() {
        return groupMasterId;
    }

    public void setGroupMasterId(int groupMasterId) {
        this.groupMasterId = groupMasterId;
    }
}
