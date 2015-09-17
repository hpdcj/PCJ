/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.network;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.pcj.internal.Worker;
import org.pcj.internal.utils.Configuration;

/**
 * Main Runnable class for process all incoming data from network in nonblocking
 * way using {@link java.nio.channels.Selector}.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class SelectorProc implements Runnable {

    private final Selector selector;
    private final Queue<ChangeRequest> interestChanges;
    private final Map<SelectableChannel, ConcurrentLinkedQueue<ByteBuffer>> writeData;
    private final Set<SelectableChannel> connected;
    private final ByteBuffer readBuffer;
    private final Worker worker;

    public SelectorProc(Worker worker) throws IOException {
        this.worker = worker;

        readBuffer = ByteBuffer.allocateDirect(Configuration.BUFFER_SIZE).order(ByteOrder.nativeOrder());

        interestChanges = new ConcurrentLinkedQueue<>();
        writeData = new ConcurrentHashMap<>();
        connected = new HashSet<>();

        selector = Selector.open();
    }

    public void register(SelectableChannel channel, int ops) throws IOException {
        ChangeRequest changeRequest = new ChangeRequest(channel, ChangeRequestType.REGISTER, ops);

        interestChanges.add(changeRequest);

        selector.wakeup();
    }

    public void send(SocketChannel socket, ByteBuffer data) throws IOException {
        final Queue<ByteBuffer> queue = writeData.get(socket);
        synchronized (queue) {
            if (queue.isEmpty()) {
                socket.write(data);
            }

            if (data.hasRemaining()) {
                queue.add(data);
                ChangeRequest changeRequest = new ChangeRequest(socket, ChangeRequestType.CHANGEOPS, SelectionKey.OP_WRITE | SelectionKey.OP_READ);
                interestChanges.add(changeRequest);

                selector.wakeup();
            }
        }
    }

    public boolean isConnected(SelectableChannel socket) {
        return connected.contains(socket);
    }

    /**
     * Wait until all data is sent, then closes selector
     *
     * @throws IOException
     */
    public void close() throws IOException {
        try {
            boolean empty = false;
            while (empty == false) {
                empty = true;
                for (Queue<ByteBuffer> queue : writeData.values()) {
                    if (queue.isEmpty() == false) {
                        synchronized (queue) {
                            queue.wait();
                            empty = false;
                            break;
                        }
                    }
                }
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace(System.err);
        }
        selector.close();
    }

    @Override
    public void run() {
        int ready;
        for (;;) {
            try {
                for (ChangeRequest changeRequest = interestChanges.poll();
                        changeRequest != null;
                        changeRequest = interestChanges.poll()) {
                    switch (changeRequest.getType()) {
                        case CHANGEOPS:
                            SelectionKey key = changeRequest.getSocket().keyFor(selector);
                            if (key.isValid()) {
                                key.interestOps(changeRequest.getOps());
                            }
                            break;
                        case REGISTER:
                            changeRequest.getSocket().register(selector, changeRequest.getOps());
                            break;
                    }
                }

                if (selector.isOpen() == false) {
                    return;
                }

                ready = selector.select();
                if (ready > 0) {
                    Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
                    while (selectedKeys.hasNext()) {
                        SelectionKey key = selectedKeys.next();
                        selectedKeys.remove();

                        if (!key.isValid()) {
                            continue;
                        }

                        int readyOps = key.readyOps();
                        if ((readyOps & SelectionKey.OP_READ) != 0) {
                            if (read(key) == false) {
                                worker.channelClosed((SocketChannel) key.channel());
                            }
                        }
                        if ((readyOps & SelectionKey.OP_WRITE) != 0) {
                            write(key);
                        }
                        if ((readyOps & SelectionKey.OP_ACCEPT) != 0) {
                            acceptConnection(key);
                        }
                        if ((readyOps & SelectionKey.OP_CONNECT) != 0) {
                            finishConnecting(key);
                        }
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
        }
    }

    public void connected(SocketChannel socket) {
        synchronized (writeData) {
            connected.add(socket);
            writeData.put(socket, new ConcurrentLinkedQueue<ByteBuffer>());
            worker.connected(socket);
        }
    }

    private void acceptConnection(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
        socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
        socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);

        connected(socketChannel);

        socketChannel.register(selector, SelectionKey.OP_READ);
    }

    private void finishConnecting(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        socketChannel.finishConnect();

//        try {
//            socketChannel.configureBlocking(false);
//            socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
//            socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
//            socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
//        } catch (IOException ex) {
//            ex.printStackTrace(System.err);
//            key.cancel();
//            synchronized (socketChannel) {
//                socketChannel.notifyAll();
//            }
//            return;
//        }
        connected(socketChannel);

        key.interestOps(SelectionKey.OP_READ);

        /*
         * notify Networker thread that connection is completed
         * (waitForConnection)
         */
        synchronized (socketChannel) {
            socketChannel.notifyAll();
        }
    }

    private boolean read(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        readBuffer.clear();

        int count;
        try {
            count = socketChannel.read(readBuffer);
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
            key.cancel();
            socketChannel.close();
            return false;
        }

        if (count == -1) {
            socketChannel.close();
            key.cancel();
            return false;
        }

        readBuffer.flip();

        worker.parseRequest(socketChannel, readBuffer.asReadOnlyBuffer());

        return true;
    }

    private void write(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        Queue<ByteBuffer> queue = writeData.get(socketChannel);
        synchronized (queue) {
            try {
                while (!queue.isEmpty()) {
                    ByteBuffer buf = queue.peek();
                    socketChannel.write(buf);

                    if (buf.hasRemaining()) {
                        break;
                    }

                    queue.poll();
                }
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
                queue.poll();
            }

            if (queue.isEmpty()) {
                key.interestOps(SelectionKey.OP_READ); // invoked only from selector thread
            }

            queue.notifyAll();
        }
    }
}
