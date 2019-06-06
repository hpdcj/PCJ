package org.pcj.internal.network;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pcj.internal.Configuration;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.WorkerPoolExecutor;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.MessageType;

final public class MessageProc {
    private static final Logger LOGGER = Logger.getLogger(MessageProc.class.getName());
    private ExecutorService workers;

    public MessageProc() {
        BlockingQueue<Runnable> blockingQueue;
        if (Configuration.MESSAGE_WORKERS_QUEUE_SIZE > 0) {
            blockingQueue = new ArrayBlockingQueue<>(Configuration.MESSAGE_WORKERS_QUEUE_SIZE);
        } else if (Configuration.MESSAGE_WORKERS_QUEUE_SIZE == 0) {
            blockingQueue = new SynchronousQueue<>();
        } else {
            blockingQueue = new LinkedBlockingQueue<>();
        }

        ThreadGroup threadGroup = new ThreadGroup("MessageProc");

        workers = new WorkerPoolExecutor(
                Configuration.MESSAGE_WORKERS_COUNT,
                threadGroup, "MessageProc-Worker-",
                blockingQueue,
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    public void shutdown() {
        workers.shutdownNow();
    }

    public void process(SocketChannel socket, RemoteMessageInputBytes remoteMessageInputBytes) {
        if (remoteMessageInputBytes.tryProcessing()) {
            workers.execute(new MessageWorker(socket, remoteMessageInputBytes));
        }
    }

    @Deprecated
    public void executeFromLocal(SocketChannel socket, Message message, MessageDataInputStream messageDataInputStream) {
        workers.execute(() -> {
            try {
                message.onReceive(socket, messageDataInputStream);
                messageDataInputStream.close();
            } catch (Throwable throwable) {
                LOGGER.log(Level.SEVERE,
                        String.format("Exception while processing message %s by node(%d).", message, InternalPCJ.getNodeData().getCurrentNodePhysicalId()),
                        throwable);
            }
        });
    }

    private static class MessageWorker implements Runnable {

        private final RemoteMessageInputBytes messageBytes;
        private final SocketChannel socket;

        public MessageWorker(SocketChannel socket, RemoteMessageInputBytes messageBytes) {
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
