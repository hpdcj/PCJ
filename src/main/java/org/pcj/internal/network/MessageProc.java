package org.pcj.internal.network;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
    private final ConcurrentMap<SocketChannel, MessageBytesInputStream> readMap;
    private ExecutorService workers;

    public MessageProc() {
        this.readMap = new ConcurrentHashMap<>();

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

        initializeFor(LoopbackSocketChannel.getInstance());
    }

    public void initializeFor(SocketChannel socketChannel) {
        readMap.put(socketChannel, new MessageBytesInputStream());
    }

    public void shutdown() {
        workers.shutdownNow();
    }


    public void process(SocketChannel socket, ByteBufferPool.PooledByteBuffer pooledByteBuffer) {
        MessageBytesInputStream messageBytes = readMap.get(socket);

        ByteBuffer readBuffer = pooledByteBuffer.getByteBuffer();
        while (readBuffer.hasRemaining()) {
            messageBytes.offerNextBytes(readBuffer);
            if (messageBytes.hasAllData()) {
                MessageDataInputStream messageDataInputStream = messageBytes.getMessageDataInputStream();
                Message message;
                try {
                    byte messageType = messageDataInputStream.readByte();
                    message = MessageType.createMessage(messageType);
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }

                workers.execute(new WorkerTask(socket, message, messageDataInputStream));

                messageBytes.prepareForNewMessage();
            }
        }
        pooledByteBuffer.returnToPool();

        // dodaje messageBuffer do kolejki wiadomości związanej z sender
        // sprawdzi czy jest worker przetwarzający wiadomości z sender - jeśli nie ma to startuje -> execute(sender)
    }

    @Deprecated
    public void executeFromLocal(SocketChannel socket, Message message, MessageDataInputStream messageDataInputStream) {
        workers.execute(new WorkerTask(socket, message, messageDataInputStream));
    }

//    public void execute(SocketChannel sender) {
//        // w pętli:
//        //  sprawdza czy kolejka nie jest pusta
//        //    jeśli nie jest to odczytuje typ wiadomości (aktualnie byte)
//        //    tworzy wiadomość odpowiedniego typu
//        //    pozwala na przetwarzanie
//        //    po przetworzeniu powoduje przeczytanie wiadomości do końca
//    }

    private static final class WorkerTask implements Runnable {

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
