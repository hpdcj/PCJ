/*
 * Copyright (c) 2011-2021, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.network;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.WorkerPoolExecutor;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.MessageType;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public final class MessageProc {
    private static final Logger LOGGER = Logger.getLogger(MessageProc.class.getName());
    private final ExecutorService workers;
    private final ExecutorService localWorkers;

    public MessageProc() {
        ThreadGroup threadGroup = new ThreadGroup("MessageProc");

        workers = new WorkerPoolExecutor(
                InternalPCJ.getConfiguration().MESSAGE_WORKERS_COUNT,
                threadGroup, "MessageProc-Worker-",
                new LinkedBlockingQueue<>(),
                new ThreadPoolExecutor.AbortPolicy());

        localWorkers = new WorkerPoolExecutor(
                InternalPCJ.getConfiguration().MESSAGE_WORKERS_COUNT,
                threadGroup, "MessageProc-LocalWorker-",
                new LinkedBlockingQueue<>(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    public void shutdown() {
        workers.shutdownNow();
        localWorkers.shutdownNow();
    }

    public void process(SocketChannel socket, MessageInputBytes messageInputBytes) {
        if (messageInputBytes.tryProcessing()) {
            workers.execute(new MessageWorker(socket, messageInputBytes));
        }
    }

    public void processLocal(SocketChannel socket, MessageInputBytes messageInputBytes) {
        localWorkers.execute(new MessageWorker(socket, messageInputBytes));
    }

    private static class MessageWorker implements Runnable {

        private final MessageInputBytes messageBytes;
        private final SocketChannel socket;

        public MessageWorker(SocketChannel socket, MessageInputBytes messageBytes) {
            this.socket = socket;
            this.messageBytes = messageBytes;
        }

        @Override
        public void run() {
            do {
                try (MessageDataInputStream messageDataInputStream = new MessageDataInputStream(messageBytes.getInputStream())) {
                    byte messageType = messageDataInputStream.readByte();
                    Message message = MessageType.createMessage(messageType);

                    processMessage(messageDataInputStream, message);
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE,
                            String.format("Exception while reading message type by node(%d).", InternalPCJ.getNodeData().getCurrentNodePhysicalId()),
                            e);
                }
                messageBytes.finishedProcessing();
            } while (messageBytes.hasMoreData() && messageBytes.tryProcessing());
        }

        private void processMessage(MessageDataInputStream messageDataInputStream, Message message) {
            try {
                message.onReceive(socket, messageDataInputStream);
            } catch (Throwable throwable) {
                LOGGER.log(Level.SEVERE,
                        String.format("Exception while processing message %s by node(%d).", message, InternalPCJ.getNodeData().getCurrentNodePhysicalId()),
                        throwable);
            }
        }
    }
}
