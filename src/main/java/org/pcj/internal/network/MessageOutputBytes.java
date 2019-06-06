package org.pcj.internal.network;

import java.io.IOException;
import org.pcj.internal.message.Message;

public interface MessageOutputBytes {
    void writeMessage(Message message) throws IOException;
}
