package org.pcj.internal.network;

import java.io.InputStream;

public interface MessageInputBytes {
    InputStream getInputStream();

    boolean tryProcessing();

    void finishedProcessing();

    boolean hasMoreData();
}
