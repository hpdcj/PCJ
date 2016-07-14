/*
 * Test app
 */
package org.pcj.test;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pcj.NodesDescription;
import org.pcj.PCJ;
import org.pcj.StartPoint;
import org.pcj.Storage;

/**
 *
 * @author faramir
 */
public class EasyTest extends Storage implements StartPoint {

    public static void main(String[] args) throws InterruptedException {
        Level level = Level.INFO;
//        Level level = Level.FINEST;
        Logger logger = Logger.getLogger("");
        Arrays.stream(logger.getHandlers()).forEach(handler -> handler.setLevel(level));
        logger.setLevel(level);

        NodesDescription nodesDescription = new NodesDescription(new String[]{
            "localhost:8091", //            "localhost:8091",
        //            "localhost:8091",
        //            "localhost:8002",
        //            "localhost:8003",
        //            "localhost:8004",
        //            "localhost:8003",
        //            "localhost:8005",
        //            "localhost:8006",
        //            "localhost:8007",
        //            "localhost:8008",
        //            "localhost:8008",
        //            "localhost:8008",
        //            "localhost:8008",
        //            "localhost:8009", // run.jvmargs=-Xmx64m
        //            "localhost:8010",
        //            "localhost:8011",
        //            "localhost:8012",
        //            "localhost:8013",
        //            "localhost:8014",
        //            "localhost:8015",
        //            "localhost:8016",
        //            "localhost:8017",
        //            "localhost:8018",
        //            "localhost:8019",
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

        for (int i = 0; i < PCJ.threadCount(); ++i) {
            if (PCJ.myId() == i) {
                System.out.println("Starting as " + PCJ.myId());
            }
            PCJ.barrier();
        }

        PCJ.createShared("a", Long[].class);
        PCJ.putLocal("a", new Long[1]);
        PCJ.putLocal("a", 2L, 0);

        System.out.println("a=" +Arrays.toString((Long[])PCJ.getLocal("a")));
    }
}
