/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.network;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pcj.internal.InternalPCJ;

/**
 * Main Runnable class for process all incoming data from network in nonblocking
 * way using {@link java.nio.channels.Selector}.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class SelectorProc implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(SelectorProc.class.getName());
    private final ByteBufferPool byteBufferPool;
    private final Selector selector;
    private final ConcurrentMap<SocketChannel, RemoteMessageInputBytes> readMap;
    private final ConcurrentMap<SocketChannel, Queue<RemoteMessageOutputBytes>> writeMap;
    private final Queue<ServerSocketChannel> serverSocketChannels;
    private final ConcurrentMap<SelectableChannel, Integer> interestChanges;

    public SelectorProc() {
        try {
            this.selector = Selector.open();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

        this.byteBufferPool = new ByteBufferPool(
                InternalPCJ.getConfiguration().BUFFER_POOL_SIZE,
                InternalPCJ.getConfiguration().BUFFER_CHUNK_SIZE);
        this.readMap = new ConcurrentHashMap<>();
        this.writeMap = new ConcurrentHashMap<>();
        this.interestChanges = new ConcurrentHashMap<>();
        this.serverSocketChannels = new ConcurrentLinkedQueue<>();
    }

    private void changeInterestOps(SelectableChannel channel, int interestOps) {
        interestChanges.compute(channel, (k, v) -> (v == null) ? interestOps : (v | interestOps));
        selector.wakeup();
    }

    private void initializeSocketChannel(SocketChannel socketChannel) throws IOException {
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.log(Level.FINEST, "Initializing socketChannel: {0}", socketChannel);
        }

        socketChannel.configureBlocking(false);
        socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
        socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
        socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);

        readMap.put(socketChannel, new RemoteMessageInputBytes());
        writeMap.put(socketChannel, new ConcurrentLinkedQueue<>());
    }

    public ServerSocketChannel bind(InetAddress hostAddress, int port, int backlog) throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        serverSocketChannel.configureBlocking(false);

        InetSocketAddress isa;
        if (hostAddress == null) {
            isa = new InetSocketAddress(port);
        } else {
            isa = new InetSocketAddress(hostAddress, port);
        }

        serverSocketChannel.bind(isa, backlog);

        serverSocketChannels.add(serverSocketChannel);

        changeInterestOps(serverSocketChannel, SelectionKey.OP_ACCEPT);

        return serverSocketChannel;
    }

    public SocketChannel connectTo(InetAddress hostAddress, int port) throws IOException {
        SocketChannel socket = SocketChannel.open();

        initializeSocketChannel(socket);

        if (socket.connect(new InetSocketAddress(hostAddress, port))) {
            changeInterestOps(socket, SelectionKey.OP_READ);

            synchronized (socket) {
                socket.notifyAll();
            }
        } else {
            changeInterestOps(socket, SelectionKey.OP_CONNECT);
        }

        return socket;
    }

    public void addToWriteQueue(SocketChannel socket, RemoteMessageOutputBytes remoteMessageOutputBytes) throws ClosedChannelException {
        if (!socket.isConnected()) {
            throw new ClosedChannelException();
        }
        Queue<RemoteMessageOutputBytes> queue = writeMap.get(socket);
        queue.add(remoteMessageOutputBytes);
        changeInterestOps(socket, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }

    public void closeAllSockets() throws IOException {
        for (ServerSocketChannel serverSocket : serverSocketChannels) {
            if (serverSocket.isOpen()) {
                serverSocket.close();
            }
        }

        if (!interestChanges.isEmpty()) {
            throw new IOException("There is something in selector' interest changes queue.");
        }

        Set<SocketChannel> sockets = writeMap.keySet();
        for (SocketChannel socket : sockets) {
            if (socket.isConnected()) {
                if (writeMap.get(socket).isEmpty()) {
                    socket.close();
                } else {
                    throw new IOException("There is data to write");
                }
            }
        }
    }

    @Override
    public void run() {
        for (; ; ) {
            try {
                Iterator<Map.Entry<SelectableChannel, Integer>> it = interestChanges.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<SelectableChannel, Integer> entry = it.next();
                    it.remove();
                    SelectableChannel channel = entry.getKey();
                    int ops = entry.getValue();
                    channel.register(selector, ops);
                }

                if (Thread.interrupted()) {
                    return;
                }

                if (selector.select() <= 0) {
                    continue;
                }

                Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();

                while (selectedKeys.hasNext()) {
                    SelectionKey key = selectedKeys.next();
                    selectedKeys.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    int readyOps;
                    try {
                        readyOps = key.readyOps();
                    } catch (CancelledKeyException ex) {
                        LOGGER.log(Level.FINE, "Key has been cancelled", ex);
                        continue;
                    }

                    if ((readyOps & SelectionKey.OP_ACCEPT) != 0) {
                        ServerSocketChannel serverSocket = (ServerSocketChannel) key.channel();

                        opAccept(serverSocket);
                    }

                    if ((readyOps & SelectionKey.OP_CONNECT) != 0) {
                        SocketChannel socket = (SocketChannel) key.channel();

                        opConnect(socket);
                    }

                    if ((readyOps & SelectionKey.OP_READ) != 0) {
                        SocketChannel socket = (SocketChannel) key.channel();

                        if (!opRead(socket)) {
                            key.cancel();
                            socket.close();
                        }
                    }

                    if ((readyOps & SelectionKey.OP_WRITE) != 0) {
                        SocketChannel socket = (SocketChannel) key.channel();

                        if (!opWrite(socket)) {
                            key.interestOps(SelectionKey.OP_READ);
                        }
                    }

                }
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Exception in SelectorProc.", ex);
            }
        }
    }

    private void opAccept(ServerSocketChannel serverSocket) throws IOException {
        SocketChannel socket = serverSocket.accept();

        initializeSocketChannel(socket);

        socket.register(selector, SelectionKey.OP_READ);

        synchronized (socket) {
            socket.notifyAll();
        }

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.log(Level.FINER, "Accepted: {0}", socket);
        }
    }

    private void opConnect(SocketChannel socket) {
        synchronized (socket) {
            try {
                if (socket.finishConnect()) {
                    socket.register(selector, SelectionKey.OP_READ);

                    LOGGER.log(Level.FINER, "Connected: {0}", socket);
                }
            } catch (IOException ex) {
                LOGGER.log(Level.FINER, "Connection failed: {0}", ex.getLocalizedMessage());
            }
            socket.notifyAll();
        }
    }

    private boolean opRead(SocketChannel socket) {
        ByteBufferPool.PooledByteBuffer pooledByteBuffer = byteBufferPool.take();
        ByteBuffer readBuffer = pooledByteBuffer.getByteBuffer();

        try {
            int count = socket.read(readBuffer);
            if (count == -1) {
                pooledByteBuffer.returnToPool();
                return false;
            }
        } catch (IOException ex) {
            LOGGER.log(Level.FINER, "Exception while reading from {0}: {1}", new Object[]{socket, ex});
            pooledByteBuffer.returnToPool();
            return false;
        }
        readBuffer.flip();

        RemoteMessageInputBytes remoteMessageInputBytes = readMap.get(socket);
        remoteMessageInputBytes.offer(pooledByteBuffer);

        InternalPCJ.getMessageProc().process(socket, remoteMessageInputBytes);

        return true;
    }

    private boolean opWrite(SocketChannel socket) throws IOException {
        Queue<RemoteMessageOutputBytes> queue = writeMap.get(socket);

        RemoteMessageOutputBytes messageBytes = queue.peek();
        if (messageBytes == null || !socket.isOpen()) {
            return false;
        }

        RemoteMessageOutputBytes.ByteBufferArray byteBuffersArray = messageBytes.getByteBufferArray();

        ByteBuffer[] array = byteBuffersArray.getArray();
        int offset = byteBuffersArray.getOffset();
        int length = byteBuffersArray.getRemainingLength();

        socket.write(array, offset, length);

        byteBuffersArray.revalidate();

        if (!byteBuffersArray.hasMoreData()) {
            queue.poll();
            return !queue.isEmpty();
        }

        return true;
    }
}