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
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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

    protected Networker(ThreadGroup threadGroup, ExecutorService workers) {
        this.workers = workers;
        this.selectorProc = new SelectorProc();
        this.selectorProcThread = new Thread(threadGroup, selectorProc, "SelectorProc");
        this.selectorProcThread.setDaemon(true);
    }

    void startup() {
        selectorProcThread.start();

    }

    ServerSocketChannel bind(InetAddress hostAddress, int port, int backlog) throws IOException {
        return selectorProc.bind(hostAddress, port, backlog);
    }

    public SocketChannel connectTo(InetAddress hostAddress, int port) throws IOException, InterruptedException {
        SocketChannel socket = selectorProc.connectTo(hostAddress, port);
        waitForConnectionEstablished(socket);
        return socket;
    }

    private void waitForConnectionEstablished(SocketChannel socket) throws InterruptedException, IOException {
        synchronized (socket) {
            while (socket.isConnected() == false) {
                if (socket.isConnectionPending() == false) {
                    throw new IOException("Unable to connect to " + socket.getRemoteAddress());
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
                    LOGGER.log(Level.FINEST, "Exception while closing sockets: {0}", ex.getMessage());
                } catch (InterruptedException ex) {
                    LOGGER.log(Level.FINEST, "Interrupted while shutting down. Force shutdown.");
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
                    LOGGER.log(Level.FINEST, "Locally processing message {0}", message.getType());
                }
                workers.submit(new WorkerTask(socket, message, loopbackMessageBytesStream.getMessageDataInputStream()));
            } else {
                MessageBytesOutputStream objectBytes = new MessageBytesOutputStream(message);
                objectBytes.writeMessage();
                objectBytes.close();

                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(Level.FINEST, "Sending message {0} to {1}", new Object[]{message.getType(), socket});
                }
                selectorProc.writeMessage(socket, objectBytes);
            }
        } catch (NotSerializableException ex) {
            String stack = Arrays.stream(ex.getStackTrace())
                                   .filter(st -> st.getClassName().startsWith("org.pcj."))
                                   .map(StackTraceElement::toString)
                                   .collect(Collectors.joining("\n\tat "));
            LOGGER.log(Level.SEVERE, "Unable to send message " + message + " to " + socket + " - contains not serializable type: " + ex.getMessage() + "\n\t" + stack);
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Exception while sending message: " + message + " to " + socket, t);
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
            LOGGER.log(Level.FINEST, "Received message {0} from {1}", new Object[]{message.getType(), socket});
        }

        workers.submit(new WorkerTask(socket, message, messageDataInputStream));
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
            } catch (Throwable t) {
                LOGGER.log(Level.SEVERE, "Exception while processing message " + message
                                                 + " by node(" + InternalPCJ.getNodeData().getCurrentNodePhysicalId() + ").", t);
            }
        }
    }
}
