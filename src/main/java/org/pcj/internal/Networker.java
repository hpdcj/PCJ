/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pcj.internal.message.Message;
import org.pcj.internal.network.LoopbackMessageBytesStream;
import org.pcj.internal.network.LoopbackSocketChannel;
import org.pcj.internal.network.MessageBytesInputStream;
import org.pcj.internal.network.SelectorProc;

/**
 * This is intermediate class (between classes that want to send data (eg.
 * {@link org.pcj.internal.Worker}) and
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

    private boolean shuttingDown = false;

    protected Networker() {
        this.workers = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(),
                new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Worker-" + counter.getAndIncrement());
            }
        });

        this.selectorProc = new SelectorProc();

        this.selectorProcThread = new Thread(selectorProc, "SelectorProc");
        this.selectorProcThread.setDaemon(true);
    }

    void startup() {
        selectorProcThread.start();

    }

    void shutdown() {
        if (shuttingDown == true) {
            return;
        }
        shuttingDown = true;

        while (true) {
            try {
                selectorProc.closeAllSockets();
                break;
            } catch (IOException ex) {
                LOGGER.log(Level.FINEST, "Exception while closing sockets: {0}", ex.getMessage());
            }
        }

        selectorProcThread.interrupt();
        workers.shutdownNow();
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
                socket.wait();
            }
        }
    }

//    private ByteBuffer prepareByteBuffer(final Message msg) {
//        MessageOutputStream bbos = new MessageOutputStream();
//        bbos.writeInt(0x00000000);
//
//        msg.writeToOutputStream(bbos);
//
//        ByteBuffer buf = bbos.getByteBuffer();
//
//        int length = buf.limit() - 4; // (Integer.SIZE / Byte.SIZE)
//        buf.putInt(length);
//        buf.rewind();
//
//        return buf;
//    }
//    void broadcast(SocketChannel left, SocketChannel right, Message message) {
//        if ((Configuration.DEBUG & 2) == 2) {
//            if ((Configuration.DEBUG & 4) == 4) {
//                System.err.println("" + worker.getData().physicalId + " broadcast: " + message + " to " + left + " and " + right);
//            } else {
//                System.err.println("" + worker.getData().physicalId + " broadcast: " + message.getType() + " to " + left + " and " + right);
//            }
//        }
//
//        try {
//            ByteBuffer mbuf = null;
//
//            if (left != null) {
//                if (left instanceof LoopbackSocketChannel) {
//                    worker.enqueueMessage(left, message);
//                } else {
//                    if (mbuf == null) {
//                        mbuf = prepareByteBuffer(message);
//                    }
//                    selectorProc.send(left, mbuf.duplicate());
//                }
//            }
//            if (right != null && right != left) {
//                if (right instanceof LoopbackSocketChannel) {
//                    worker.enqueueMessage(right, message);
//                } else {
//                    if (mbuf == null) {
//                        mbuf = prepareByteBuffer(message);
//                    }
//                    selectorProc.send(right, mbuf.duplicate());
//                }
//            }
////            for (SocketChannel child : left) {
////                if (child != null) {
////                    selectorProc.send(child, mbuf.duplicate());
////                }
////            }
//        } catch (IOException ex) {
//            throw new RuntimeException(ex);
//        }
//    }
    public void send(SocketChannel socket, Message message) throws UncheckedIOException {
//        try {
//            LOGGER.log(Level.INFO, "{0} send {1} to {2}", new Object[]{pcjData.physicalId, message, socket.getRemoteAddress()});
//        } catch (IOException ex) {
//            Logger.getLogger(Networker.class.getName()).log(Level.SEVERE, null, ex);
//        }
        try {
            if (socket instanceof LoopbackSocketChannel) {
                LoopbackMessageBytesStream loopbackMessageBytesStream = new LoopbackMessageBytesStream(message);
                loopbackMessageBytesStream.writeMessage();
                loopbackMessageBytesStream.close();

                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(Level.FINEST, "Locally processing message {0}", message.getType());
                }
                workers.submit(() -> {
                    try {
                        message.execute(socket, loopbackMessageBytesStream.getMessageData());
                    } catch (Throwable t) {
                        LOGGER.log(Level.SEVERE, "Exception while processing message.", t);
                    }
                });
            } else {
                LOGGER.log(Level.FINEST, "Sending message {0} to {1}", new Object[]{message.getType(), socket});
                selectorProc.writeMessage(socket, message);
            }
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, null, t);
        }
    }

//    <X> X sendWait(SocketChannel socket, Message msg) throws IOException {
//        int messageId = msg.getMessageId();
//
//        InternalResponseAttachment wr = new InternalResponseAttachment();
//        pcjNodeData.attachmentMap.put(messageId, wr);
//
//        synchronized (wr) {
//            send(socket, msg);
//
//            return wr.waitForResponse();
//        }
//    }
//
//    void send(InternalGroup group, Message msg) throws IOException {
////        Integer physicalRoot = group.getPhysicalMaster();
////
////        SocketChannel socket = null;
////        if (physicalRoot != null) {
////            socket = workerData.physicalNodes.getFutureObject(physicalRoot);
////        }
//        SocketChannel socket = pcjNodeData.physicalNodes.get(group.getPhysicalMaster());
//        send(socket, msg);
//    }
//
//    void send(int nodeId, Message msg) throws IOException {
//        SocketChannel socket = pcjNodeData.physicalNodes.get(pcjNodeData.virtualNodes.get(nodeId));
//        send(socket, msg);
//    }
//
//    <X> X sendWait(int nodeId, Message msg) throws IOException {
//        SocketChannel socket = pcjNodeData.physicalNodes.get(pcjNodeData.virtualNodes.get(nodeId));
//        return sendWait(socket, msg);
//    }
    public void processMessageBytes(SocketChannel socket, MessageBytesInputStream messageBytes) {
        Message message = messageBytes.readMessage();
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "Received message {0} from {1}", new Object[]{message.getType(), socket});
        }
        workers.submit(() -> {
            try {
                message.execute(socket, messageBytes.getMessageData());
            } catch (Throwable t) {
                LOGGER.log(Level.SEVERE, "Exception while processing message.", t);
            }
        });
    }
}
