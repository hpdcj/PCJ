/*
 * Test app
 */
package org.pcj.test;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pcj.NodesDescription;
import org.pcj.PCJ;
import org.pcj.Shared;
import org.pcj.StartPoint;
import org.pcj.internal.InternalStorage;

/**
 *
 * @author faramir
 */
public class EasyTest extends InternalStorage implements StartPoint {

    enum SharedEnum implements Shared {
        a(double.class),
        b(double[].class),
        c(Double.class);
        private final Class<?> clazz;

        private SharedEnum(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public Class<?> type() {
            return clazz;
        }

    }

    public static void main(String[] args) throws InterruptedException {
//        Level level = Level.INFO;
        Level level = Level.FINEST;
        Logger logger = Logger.getLogger("");
        Arrays.stream(logger.getHandlers()).forEach(handler -> handler.setLevel(level));
        logger.setLevel(level);

        NodesDescription nodesDescription = new NodesDescription(new String[]{
            "localhost:8091",
            "localhost:8002",
            "localhost:8003",
            "localhost:8004", //            "localhost:8005",
        //            "localhost:8006",
        //            "localhost:8007",
        //            "localhost:8008",
        //            "localhost:8009", // run.jvmargs=-Xmx64m
        //            "localhost:8010",//
        //            "localhost:8011",
        //            "localhost:8011",
        //            "localhost:8011",//
        //            "localhost:8012",
        //            "localhost:8012",//
        //            "localhost:8013",//
        //            "localhost:8014",
        //            "localhost:8014",//
        //            "localhost:8015",//
        //            "localhost:8016",
        //            "localhost:8016",
        //            "localhost:8016",//
        //            "localhost:8017",//
        //            "localhost:8018",
        //            "localhost:8018",//
        //            "localhost:8019",//
        });

        PCJ.deploy(EasyTest.class, nodesDescription, SharedEnum.class);
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
        if (PCJ.myId() == 0) {
            System.out.println("Broadcasting array");
            PCJ.broadcast(SharedEnum.b, new double[]{1.0});
        }
//        PCJ.barrier();
        System.out.println(PCJ.myId() + " -> " + Arrays.toString((double[]) PCJ.getLocal(SharedEnum.b)));
    }
}
