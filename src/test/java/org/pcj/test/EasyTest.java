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
        b(double.class),
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
            "localhost:8001",
            "localhost:8002",
            "localhost:8003",
            "localhost:8003",
            "localhost:8003",
            "localhost:8004",
            "localhost:8004",
            "localhost:8004",
            "localhost:8004",
            "localhost:8003",
            "localhost:8003",
            "localhost:8005",
            "localhost:8006",
            "localhost:8006",
            "localhost:8006",
            "localhost:8007",
            "localhost:8008",
            "localhost:8008",
            "localhost:8008",
            "localhost:8008",
            "localhost:8008",
            "localhost:8008",
            "localhost:8009", // run.jvmargs=-Xmx64m
            "localhost:8010",//
            "localhost:8011",
            "localhost:8011",
            "localhost:8011",//
            "localhost:8012",
            "localhost:8012",//
            "localhost:8013",//
            "localhost:8014",
            "localhost:8014",//
            "localhost:8015",//
            "localhost:8016",
            "localhost:8016",
            "localhost:8016",//
            "localhost:8017",//
            "localhost:8018",
            "localhost:8018",//
            "localhost:8019",//
        });

        PCJ.deploy(EasyTest.class, nodesDescription, SharedEnum.class);
    }

    @Override
    public void main() throws Throwable {
//        Level level = Level.FINEST;
//        Logger logger = Logger.getLogger("");
//        Arrays.stream(logger.getHandlers()).forEach(handler -> handler.setLevel(level));
//        logger.setLevel(level);
        for (int j = 0; j < 5; ++j) {
            for (int i = 0; i < PCJ.threadCount(); ++i) {
                if (PCJ.myId() == i) {
                    System.out.println("Starting as " + PCJ.myId());
                }
                PCJ.barrier();
            }
        }

        int number_of_tests = 10;
        int ntimes = 1000;

        PCJ.barrier();

        double tmin = Double.MAX_VALUE;
        for (int k = 0; k < number_of_tests; k++) {
            long rTime = System.nanoTime();

            for (int i = 0; i < ntimes; i++) {
                PCJ.barrier();
            }

            rTime = System.nanoTime() - rTime;
            double dtime = (rTime / (double) ntimes) * 1e-9;

            if (tmin > dtime) {
                tmin = dtime;
            }

//            System.out.println(PCJ.threadCount() + " " + t);
            PCJ.barrier();
        }

        if (PCJ.myId() == 0) {
            System.out.format("Barrier\t%5d\ttime\t%12.7f\n",
                    PCJ.threadCount(), tmin);
        }

        PCJ.barrier();
        System.out.println("END");
    }
}
