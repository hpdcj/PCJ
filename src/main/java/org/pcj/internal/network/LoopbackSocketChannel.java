/* 
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.network;

import java.io.IOException;
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
final public class LoopbackSocketChannel extends SocketChannel {

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
    public int read(ByteBuffer dst) throws IOException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public SocketChannel bind(SocketAddress local) throws IOException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public boolean connect(SocketAddress remote) throws IOException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public boolean isConnectionPending() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public boolean isConnected() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public boolean finishConnect() throws IOException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public SocketChannel shutdownInput() throws IOException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public SocketChannel shutdownOutput() throws IOException {
        return this;
    }

    @Override
    public Socket socket() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        return loopbackSocketAddress;
    }

    @Override
    public SocketAddress getRemoteAddress() throws IOException {
        return loopbackSocketAddress;
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
        return Collections.emptySet();
    }

    @Override
    public <T> T getOption(SocketOption<T> name) throws IOException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public <T> SocketChannel setOption(SocketOption<T> name, T value) throws IOException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    protected void implCloseSelectableChannel() throws IOException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    protected void implConfigureBlocking(boolean block) throws IOException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public String toString() {
        return "[loopback]";
    }

}
