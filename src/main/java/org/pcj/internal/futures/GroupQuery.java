/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj.internal.futures;

/**
 *
 * @author faramir
 */
public class GroupQuery {

    private final WaitObject waitObject;
    private int groupId;
    private int groupMasterId;

    public GroupQuery() {
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
