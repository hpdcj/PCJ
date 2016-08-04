/*
 * Test app
 */
package org.pcj.test;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pcj.Group;
import org.pcj.NodesDescription;
import org.pcj.PCJ;
import org.pcj.Shared;
import org.pcj.StartPoint;

/**
 *
 * @author faramir
 */
public class EasyTest implements StartPoint {

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
        Level level = Level.INFO;
//        Level level = Level.FINEST;
        Logger logger = Logger.getLogger("");
        Arrays.stream(logger.getHandlers()).forEach(handler -> handler.setLevel(level));
        logger.setLevel(level);

        NodesDescription nodesDescription = new NodesDescription(new String[]{
            "localhost:8091",
            "localhost:8002",//
            "localhost:8003",
            "localhost:8004",
            "localhost:8005", //
            "localhost:8006",
            "localhost:8007",
            "localhost:8008",
            "localhost:8009", // run.jvmargs=-Xmx64m
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
//        Thread.sleep(PCJ.myId() * 500);
        Group g = PCJ.join("test");
        System.out.println("globalId: "+PCJ.myId() + " groupId:" + g.myId());
        PCJ.barrier();

        for (int i = 0; i < 500; ++i) {
            System.out.println(PCJ.myId() + "> joining to test" + i);
            Thread.sleep((long) (Math.random() * 100));
            PCJ.join("test" + i);
        }

        PCJ.barrier();
        System.out.println(PCJ.myId()+"> DONE");
        
        
//        Level level = Level.FINEST;
//        Logger logger = Logger.getLogger("");
//        Arrays.stream(logger.getHandlers()).forEach(handler -> handler.setLevel(level));
//        logger.setLevel(level);
//        for (int i = 0; i < PCJ.threadCount(); ++i) {
//            if (PCJ.myId() == i) {
//                System.out.println("Starting as " + PCJ.myId());
//            }
//            PCJ.barrier();
//        }
//
//        int n = 4*8*8192;
////        int n = 4096;
//
////        if (PCJ.myId() == 1) {
////            double[] b = new double[n];
////            for (int i = 0; i < n; i++) {
////                b[i] = (double) i + 1;
////            }
////
////            PCJ.putLocal(SharedEnum.b, b);
////        }
////        PCJ.barrier();
////        if (PCJ.myId() == 0) {
////            PCJ.get(1, SharedEnum.b);
////        }
//        double[] b = new double[n];
//        for (int i = 0; i < n; i++) {
//            b[i] = (double) i + 1;
//        }
//        PCJ.monitor(SharedEnum.b);
//
//        PCJ.barrier();
//
//        int ntimes = 100;
//
//        for (int i = 0; i < ntimes; i++) {
//            if (PCJ.myId() == 0) {
//                try {
//                    PCJ.broadcast(SharedEnum.b, b);
//                } catch (Exception ex) {
//                    System.out.println(ex.getMessage());
//                }
//            }
////            PCJ.waitFor(SharedEnum.b);
//            PCJ.barrier();
//        }
//        PCJ.barrier();
//        System.out.println(PCJ.myId() + " -> " + Arrays.toString((double[]) PCJ.getLocal(SharedEnum.b)));
    }
}
