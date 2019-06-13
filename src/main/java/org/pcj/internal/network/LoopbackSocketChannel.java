/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.network;

import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.Set;

/**
 * Fake SocketChannel that represents Loopback address
 * (instead of using null).
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public final class LoopbackSocketChannel extends SocketChannel {

    private static class LoopbackSocketChannelHolder {

        private static final SocketChannel INSTANCE = new LoopbackSocketChannel();
    }

    private static class LoopbackSocketAddress extends SocketAddress {

        @Override
        public String toString() {
            return "[loopback]";
        }
    }

    private final SocketAddress loopbackSocketAddress = new LoopbackSocketAddress();

    private LoopbackSocketChannel() {
        super(null);
    }

    public static SocketChannel getInstance() {
        return LoopbackSocketChannelHolder.INSTANCE;
    }

    @Override
    public int read(ByteBuffer dst) throws IllegalStateException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IllegalStateException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public int write(ByteBuffer src) throws IllegalStateException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IllegalStateException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public SocketChannel bind(SocketAddress local) throws IllegalStateException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public boolean connect(SocketAddress remote) throws IllegalStateException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public boolean isConnectionPending() throws IllegalStateException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public boolean isConnected() throws IllegalStateException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public boolean finishConnect() throws IllegalStateException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public SocketChannel shutdownInput() throws IllegalStateException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public SocketChannel shutdownOutput() {
        return this;
    }

    @Override
    public Socket socket() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public SocketAddress getLocalAddress() {
        return loopbackSocketAddress;
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return loopbackSocketAddress;
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
        return Collections.emptySet();
    }

    @Override
    public <T> T getOption(SocketOption<T> name) throws IllegalStateException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public <T> SocketChannel setOption(SocketOption<T> name, T value) throws IllegalStateException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    protected void implCloseSelectableChannel() throws IllegalStateException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    protected void implConfigureBlocking(boolean block) throws IllegalStateException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public String toString() {
        return "[loopback]";
    }

}
