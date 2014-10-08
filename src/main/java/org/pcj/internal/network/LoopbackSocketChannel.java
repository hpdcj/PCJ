/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj.internal.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Set;
import org.pcj.internal.utils.Configuration;

/**
 * Fake SocketChannel that represents Loopback address
 * (instead of using null).
 *
 * @author faramir
 */
final public class LoopbackSocketChannel extends SocketChannel {

    private static final SocketAddress loopback = new InetSocketAddress(InetAddress.getLoopbackAddress(), Configuration.DEFAULT_PORT);

    private static class LoopbackSocketChannelHolder {

        private static final SocketChannel INSTANCE = new LoopbackSocketChannel();
    }

    private LoopbackSocketChannel() {
        super(null);
    }

    public static SocketChannel getInstance() {
        return LoopbackSocketChannelHolder.INSTANCE;
    }

    @Override
    public SocketChannel bind(SocketAddress local) throws IOException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public <T> SocketChannel setOption(SocketOption<T> name, T value) throws IOException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public SocketChannel shutdownInput() throws IOException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public SocketChannel shutdownOutput() throws IOException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public Socket socket() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean isConnected() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean isConnectionPending() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean connect(SocketAddress remote) throws IOException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean finishConnect() throws IOException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public SocketAddress getRemoteAddress() throws IOException {
        return loopback;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    protected void implCloseSelectableChannel() throws IOException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    protected void implConfigureBlocking(boolean block) throws IOException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public <T> T getOption(SocketOption<T> name) throws IOException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public String toString() {
        return "[loopback]";
    }
}
