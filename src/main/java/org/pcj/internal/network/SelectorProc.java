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
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pcj.internal.Configuration;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.WorkerPoolExecutor;

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
    private final ConcurrentMap<SocketChannel, MessageBytesInputStream> readMap;
    private final ConcurrentMap<SocketChannel, Queue<MessageBytesOutputStream>> writeMap;
    private final Queue<ServerSocketChannel> serverSocketChannels;
    private final Queue<InterestChange> interestChanges;
    private final ExecutorService byteBufferWorker;

    public SelectorProc(ThreadGroup threadGroup) {
        try {
            this.selector = Selector.open();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

        this.byteBufferPool = new ByteBufferPool(Configuration.BUFFER_POOL_SIZE, Configuration.BUFFER_CHUNK_SIZE);
        this.writeMap = new ConcurrentHashMap<>();
        this.readMap = new ConcurrentHashMap<>();
        this.interestChanges = new ConcurrentLinkedQueue<>();
        this.serverSocketChannels = new ConcurrentLinkedQueue<>();

        this.byteBufferWorker = new WorkerPoolExecutor(
                1,
                threadGroup, "SelectorProc-Worker-",
                new LinkedBlockingQueue<>(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    private void changeInterestOps(SelectableChannel channel, int interestOps) {
        interestChanges.add(new InterestChange(channel, interestOps));
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

        readMap.put(socketChannel, new MessageBytesInputStream());
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

    public void writeMessage(SocketChannel socket, MessageBytesOutputStream objectBytes) throws ClosedChannelException {
        if (!socket.isConnected()) {
            throw new ClosedChannelException();
        }
        Queue<MessageBytesOutputStream> queue = writeMap.get(socket);
        queue.add(objectBytes);
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
                InterestChange interestChange;
                while ((interestChange = interestChanges.poll()) != null) {
                    SelectableChannel channel = interestChange.channel;
                    channel.register(selector, interestChange.interestOps);
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
            pooledByteBuffer.returnToPool();
            LOGGER.log(Level.FINER, "Exception while reading from {0}: {1}", new Object[]{socket, ex});
            return false;
        }

        readBuffer.flip();

        MessageBytesInputStream messageBytes = readMap.get(socket);
        byteBufferWorker.submit(new ByteBufferWorker(pooledByteBuffer, messageBytes, socket));

        return true;
    }


    private boolean opWrite(SocketChannel socket) throws IOException {
        Queue<MessageBytesOutputStream> queue = writeMap.get(socket);

        MessageBytesOutputStream messageBytes = queue.peek();
        if (messageBytes == null || !socket.isOpen()) {
            return false;
        }

        MessageBytesOutputStream.ByteBufferArray byteBuffersArray = messageBytes.getByteBufferArray();

        socket.write(byteBuffersArray.getArray(), byteBuffersArray.getOffset(), byteBuffersArray.getRemainingLength());
        byteBuffersArray.revalidate();

        if (byteBuffersArray.getRemainingLength() == 0) {
            queue.poll();
        }

        return true;
    }

    public void shutdown() {
        byteBufferWorker.shutdownNow();
    }

    private static class InterestChange {

        private final SelectableChannel channel;
        private final int interestOps;

        private InterestChange(SelectableChannel channel, int interestOps) {
            this.channel = channel;
            this.interestOps = interestOps;
        }

    }

    private static class ByteBufferWorker implements Runnable {
        private final ByteBufferPool.PooledByteBuffer pooledByteBuffer;
        private final MessageBytesInputStream messageBytes;
        private final SocketChannel socket;

        public ByteBufferWorker(ByteBufferPool.PooledByteBuffer pooledByteBuffer, MessageBytesInputStream messageBytes, SocketChannel socket) {
            this.pooledByteBuffer = pooledByteBuffer;
            this.messageBytes = messageBytes;
            this.socket = socket;
        }

        @Override
        public void run() {
            ByteBuffer readBuffer = pooledByteBuffer.getByteBuffer();

            while (readBuffer.hasRemaining()) {
                messageBytes.offerNextBytes(readBuffer);
                if (messageBytes.hasAllData()) {
                    InternalPCJ.getNetworker().processMessageBytes(socket, messageBytes);
                    messageBytes.prepareForNewMessage();
                }
            }

            pooledByteBuffer.returnToPool();
        }
    }
}
