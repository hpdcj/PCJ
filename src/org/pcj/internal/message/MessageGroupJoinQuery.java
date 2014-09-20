/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import org.pcj.internal.network.MessageOutputStream;
import org.pcj.internal.network.MessageInputStream;
import java.io.IOException;

/**
 * @see MessageTypes#GROUP_JOIN_QUERY
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageGroupJoinQuery extends Message {

    private String groupName;

    public MessageGroupJoinQuery() {
        super(MessageTypes.GROUP_JOIN_QUERY);
    }

    @Override
    void writeObjects(MessageOutputStream bbos) {
        bbos.writeString(groupName);
    }

    @Override
    void readObjects(MessageInputStream bbis) {
        groupName = bbis.readString();
    }

    @Override
    public String paramsToString() {
        return "groupName:" + groupName;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
