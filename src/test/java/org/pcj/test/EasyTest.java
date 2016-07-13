/*
 * Test app
 */
package org.pcj.test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pcj.NodesDescription;
import org.pcj.PCJ;
import org.pcj.PcjFuture;
import org.pcj.StartPoint;
import org.pcj.Storage;

/**
 *
 * @author faramir
 */
public class EasyTest extends Storage implements StartPoint {

    public static void main(String[] args) throws InterruptedException {
//        Level level = Level.INFO;
        Level level = Level.FINEST;
        Logger logger = Logger.getLogger("");
        Arrays.stream(logger.getHandlers()).forEach(handler -> handler.setLevel(level));
        logger.setLevel(level);

        NodesDescription nodesDescription = new NodesDescription(new String[]{
            "localhost:8091",
            "localhost:8091",
            "localhost:8091",
            "localhost:8002",
            "localhost:8003",
            "localhost:8004",
            "localhost:8003",
            "localhost:8005",
            "localhost:8006",
            "localhost:8007",
            "localhost:8009", //            "localhost:8010", // run.jvmargs=-Xmx64m
        //            "localhost:8011",
        //            "localhost:8012",
        //            "localhost:8013",
        //            "localhost:8014",
        //            "localhost:8015",
        //            "localhost:8016",
        //            "localhost:8017",
        //            "localhost:8018",
        //            "localhost:8019",
        //            "localhost:8020",
        //            "localhost:8021",
        //            "localhost:8022",
        //            "localhost:8023",
        //            "localhost:8024",
        //            "localhost:8025",
        });

//        PCJ.start(EasyTest.class, EasyTest.class,
        PCJ.deploy(EasyTest.class, EasyTest.class, nodesDescription);
    }

    @Override
    public void main() throws Throwable {
//        Level level = Level.FINEST;
//        Logger logger = Logger.getLogger("");
//        Arrays.stream(logger.getHandlers()).forEach(handler -> handler.setLevel(level));
//        logger.setLevel(level);

        System.out.println("before: " + PCJ.myId());
        PCJ.barrier();
        Thread.sleep(500 * PCJ.myId() + 500);
        PcjFuture<Void> f0 = PCJ.asyncBarrier();
        PcjFuture<Void> f1 = PCJ.asyncBarrier();
        Thread.sleep(500 * PCJ.myId() + 500);
        PcjFuture<Void> f = PCJ.asyncBarrier();
        f0.get();
        f1.get();

        System.out.println("middle 2: " + PCJ.myId());
        while (f.isDone() == false) {
            try {
                f.get(1, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                System.out.println(PCJ.myId() + ": Exception");
            }
        }
        System.out.println("after: " + PCJ.myId());
//        throw new RuntimeException("test");
    }
}
