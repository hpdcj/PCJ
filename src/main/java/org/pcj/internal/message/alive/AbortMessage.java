package org.pcj.internal.message.alive;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.MessageType;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

public class AbortMessage extends Message {
    public AbortMessage() {
        super(MessageType.ABORT);
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
    }

    @Override
    public void onReceive(SocketChannel sender, MessageDataInputStream in) throws IOException {
        NodeData nodeData = InternalPCJ.getNodeData();
        AliveState state = nodeData.getAliveState();
        state.nodeAborted(sender);
    }
}
