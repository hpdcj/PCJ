/*
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pcj.PcjRuntimeException;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.MessageType;
import org.pcj.internal.network.LoopbackMessageBytesStream;
import org.pcj.internal.network.LoopbackSocketChannel;
import org.pcj.internal.network.MessageBytesInputStream;
import org.pcj.internal.network.MessageBytesOutputStream;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.SelectorProc;

/**
 * This is intermediate class (between classes that want to send data (eg.
 * {@link org.pcj.internal.network.SelectorProc} classes) for sending data
 * across network. It is used for binding address, connecting to hosts and
 * sending data.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class Networker {

    private static final Logger LOGGER = Logger.getLogger(Networker.class.getName());
    private final SelectorProc selectorProc;
    private final Thread selectorProcThread;
    private final ExecutorService workers;
    private String currentHostName;

    protected Networker(ThreadGroup threadGroup, ExecutorService workers) {
        this.workers = workers;
        this.selectorProc = new SelectorProc();
        this.selectorProcThread = new Thread(threadGroup, selectorProc, "SelectorProc");
        this.selectorProcThread.setDaemon(true);
    }

    void setupAndBind(Queue<InetAddress> interfacesAddresses, int port) {
        selectorProcThread.start();

        currentHostName = String.format("%s:%d", guessCurrentHostName(interfacesAddresses), port);
        tryToBind(interfacesAddresses, port);
    }

    private static String guessCurrentHostName(Queue<InetAddress> interfacesAddresses) {
        InetAddress localHost = null;
        try {

            localHost = InetAddress.getLocalHost();
            if (!localHost.isLoopbackAddress()) {
                return localHost.getHostName();
            }
        } catch (UnknownHostException e) {
            // skip exception, try another method
        }
        for (InetAddress inetAddress : interfacesAddresses) {
            if (!inetAddress.isLoopbackAddress()) {
                return inetAddress.getHostName();
            }
        }
        return localHost == null ? "*unknown*" : localHost.getHostName();
    }

    private void tryToBind(Queue<InetAddress> interfacesAddresses, int port) {
        Queue<InetAddress> inetAddresses = new ArrayDeque<>(interfacesAddresses);
        for (int attempt = 0; attempt <= Configuration.INIT_RETRY_COUNT; ++attempt) {
            for (Iterator<InetAddress> it = inetAddresses.iterator(); it.hasNext(); ) {
                InetAddress inetAddress = it.next();

                try {
                    bind(inetAddress, port, Configuration.INIT_BACKLOG_COUNT);
                    it.remove();
                } catch (IOException ex) {
                    if (attempt < Configuration.INIT_RETRY_COUNT) {
                        LOGGER.log(Level.WARNING,
                                "[{0}] ({1,number,#} attempt of {2,number,#}) Binding on port {3,number,#} of {4} failed: {5}. Retrying.",
                                new Object[]{
                                        currentHostName,
                                        attempt + 1,
                                        Configuration.INIT_RETRY_COUNT + 1,
                                        port,
                                        inetAddress,
                                        ex.getMessage()});
                    } else {
                        throw new UncheckedIOException(String.format("[%s] Binding on port %s failed!", currentHostName, port), ex);
                    }
                }
            }

            if (inetAddresses.isEmpty()) {
                LOGGER.log(Level.FINE, "[{0}] Binding on all interfaces successfully completed.", currentHostName);
                return;
            } else {
                try {
                    Thread.sleep(Configuration.INIT_RETRY_DELAY * 1000 + (int) (Math.random() * 1000));
                } catch (InterruptedException ex) {
                    throw new PcjRuntimeException(String.format("[%s] Interruption occurred while waiting for binding retry.", currentHostName));
                }
            }
        }
        throw new IllegalStateException(String.format("[%s] Unreachable code.", currentHostName));
    }

    private ServerSocketChannel bind(InetAddress hostAddress, int port, int backlog) throws IOException {
        return selectorProc.bind(hostAddress, port, backlog);
    }

    public SocketChannel tryToConnectTo(String hostname, int port) {
        try {
            for (int attempt = 0; attempt <= Configuration.INIT_RETRY_COUNT; ++attempt) {
                try {
                    LOGGER.log(Level.FINE, "[{0}] Connecting to: {1}:{2,number,#}",
                            new Object[]{currentHostName, hostname, port});

                    InetAddress inetAddressNode0 = InetAddress.getByName(hostname);
                    SocketChannel socket = connectTo(inetAddressNode0, port);

                    LOGGER.log(Level.FINER, "[{0}] Connected to {1}:{2,number,#}: {3}",
                            new Object[]{currentHostName, hostname, port, Objects.toString(socket)});

                    return socket;
                } catch (IOException ex) {
                    if (attempt < Configuration.INIT_RETRY_COUNT) {
                        LOGGER.log(Level.WARNING,
                                "[{0}] ({1,number,#} attempt of {2,number,#}) Connecting to {3}:{4,number,#} failed: {5}. Retrying.",
                                new Object[]{
                                        currentHostName,
                                        attempt + 1,
                                        Configuration.INIT_RETRY_COUNT + 1,
                                        hostname,
                                        port,
                                        ex.getMessage()});

                        Thread.sleep(Configuration.INIT_RETRY_DELAY * 1000 + (int) (Math.random() * 1000));
                    } else {
                        throw new PcjRuntimeException(String.format("[%s] Connecting to %s:%d failed!", currentHostName, hostname, port), ex);
                    }
                }
            }
        } catch (InterruptedException ex) {
            throw new PcjRuntimeException(String.format("[%s] Connecting to %s:%d interrupted!", currentHostName, hostname, port));
        }
        throw new IllegalStateException(String.format("[%s] Unreachable code.", currentHostName));
    }

    private SocketChannel connectTo(InetAddress hostAddress, int port) throws IOException, InterruptedException {
        SocketChannel socket = selectorProc.connectTo(hostAddress, port);
        waitForConnectionEstablished(socket);
        return socket;
    }

    private void waitForConnectionEstablished(SocketChannel socket) throws InterruptedException, IOException {
        synchronized (socket) {
            while (!socket.isConnected()) {
                if (!socket.isConnectionPending()) {
                    throw new IOException(String.format("[%s] Unable to connect to %s", currentHostName, socket.getRemoteAddress()));
                }
                socket.wait(100);
            }
        }
    }

    void shutdown() {
        try {
            while (true) {
                try {
                    Thread.sleep(10);
                    selectorProc.closeAllSockets();
                    break;
                } catch (IOException ex) {
                    LOGGER.log(Level.FINEST, "[{0}] Exception while closing sockets: {1}",
                            new Object[]{currentHostName, ex.getMessage()});
                } catch (InterruptedException ex) {
                    LOGGER.log(Level.FINEST, "[{0}] Interrupted while shutting down. Force shutdown.", currentHostName);
                    break;
                }
            }
        } finally {
            selectorProcThread.interrupt();
            workers.shutdownNow();
        }
    }

    public void send(SocketChannel socket, Message message) {
        try {
            if (socket instanceof LoopbackSocketChannel) {
                LoopbackMessageBytesStream loopbackMessageBytesStream = new LoopbackMessageBytesStream(message);
                loopbackMessageBytesStream.writeMessage();
                loopbackMessageBytesStream.close();

                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(Level.FINEST, "[{0}] Locally processing message {1}",
                            new Object[]{currentHostName, message.getType()});
                }
                workers.execute(new WorkerTask(socket, message, loopbackMessageBytesStream.getMessageDataInputStream()));
            } else {
                MessageBytesOutputStream objectBytes = new MessageBytesOutputStream(message);
                objectBytes.writeMessage();
                objectBytes.close();

                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(Level.FINEST, "[{0}] Sending message {1} to {2}",
                            new Object[]{currentHostName, message.getType(), socket});
                }
                selectorProc.writeMessage(socket, objectBytes);
            }
        } catch (ClosedChannelException | NotSerializableException ex) {
            throw new PcjRuntimeException(ex);
        } catch (Throwable throwable) {
            LOGGER.log(Level.SEVERE,
                    String.format("[%s] Exception while sending message: {1} to {2}%s to %s", currentHostName, message, socket),
                    throwable);
        }
    }

    public void processMessageBytes(SocketChannel socket, MessageBytesInputStream messageBytes) {
        MessageDataInputStream messageDataInputStream = messageBytes.getMessageDataInputStream();
        Message message;
        try {
            byte messageType = messageDataInputStream.readByte();
            message = MessageType.createMessage(messageType);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "[{0}] Received message {1} from {2}",
                    new Object[]{currentHostName, message.getType(), socket});
        }

        workers.execute(new WorkerTask(socket, message, messageDataInputStream));
    }

    private static class WorkerTask implements Runnable {

        private final SocketChannel socket;
        private final Message message;
        private final MessageDataInputStream messageDataInputStream;

        public WorkerTask(SocketChannel socket, Message message, MessageDataInputStream messageDataInputStream) {
            this.socket = socket;
            this.message = message;
            this.messageDataInputStream = messageDataInputStream;
        }

        @Override
        public void run() {
            try {
                message.onReceive(socket, messageDataInputStream);
                messageDataInputStream.close();
            } catch (Throwable throwable) {
                LOGGER.log(Level.SEVERE,
                        String.format("Exception while processing message %s by node(%d).", message, InternalPCJ.getNodeData().getCurrentNodePhysicalId()),
                        throwable);
            }
        }
    }
}
