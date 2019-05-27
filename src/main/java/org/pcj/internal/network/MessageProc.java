package org.pcj.internal.network;

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

    public void execute(SocketChannel socket, Message message, MessageDataInputStream messageDataInputStream) {
        workers.execute(new WorkerTask(socket, message, messageDataInputStream));
    }
//    public void process(SocketChannel sender, MessageBuffer messageBuffer) {
//        // dodaje messageBuffer do kolejki wiadomości związanej z sender
//        // sprawdzi czy jest worker przetwarzający wiadomości z sender - jeśli nie ma to startuje -> execute(sender)
//    }
//
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
