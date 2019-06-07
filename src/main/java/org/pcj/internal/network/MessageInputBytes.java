/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.network;

import java.io.InputStream;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public interface MessageInputBytes {
    InputStream getInputStream();

    boolean tryProcessing();

    void finishedProcessing();

    boolean hasMoreData();
}
