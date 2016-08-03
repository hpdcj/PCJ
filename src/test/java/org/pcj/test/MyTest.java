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
public class MyTest extends InternalStorage implements StartPoint {

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
            "localhost:8001",
            "localhost:8002",
            "localhost:8003",
            "localhost:8004",
            "localhost:8003",
            "localhost:8005",
            "localhost:8006",
            "localhost:8007",
            "localhost:8008",
            "localhost:8008",
            "localhost:8008",
            "localhost:8008",
            "localhost:8009", // run.jvmargs=-Xmx64m
        //            "localhost:8010", "localhost:8011", "localhost:8012", "localhost:8013", "localhost:8014",
        //            "localhost:8015", "localhost:8016", "localhost:8017", "localhost:8018", "localhost:8019",
        //            "localhost:8020", "localhost:8021", "localhost:8022", "localhost:8023", "localhost:8024",
        //            "localhost:8025", "localhost:8026", "localhost:8027", "localhost:8028", "localhost:8029",
        //            "localhost:8030", "localhost:8031", "localhost:8032", "localhost:8033", "localhost:8034",
        //            "localhost:8035", "localhost:8036", "localhost:8037", "localhost:8038", "localhost:8039",
        //            "localhost:8040", "localhost:8041", "localhost:8042", "localhost:8043", "localhost:8044",
        //            "localhost:8045", "localhost:8046", "localhost:8047", "localhost:8048", "localhost:8049",
        });

//        PCJ.start(EasyTest.class, EasyTest.class,
        PCJ.deploy(MyTest.class, nodesDescription, SharedEnum.class);
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

        PCJ.putLocal(SharedEnum.a, PCJ.myId());
        PCJ.putLocal(SharedEnum.b, 'b');
        PCJ.putLocal(SharedEnum.c, 2);
        PCJ.barrier();
        if (PCJ.myId() == 0) {
            System.out.println("a@1 = " + PCJ.asyncGet(1, SharedEnum.a).get());
        } else if (PCJ.myId() == 1) {
            PCJ.asyncPut(0, SharedEnum.a, 1000).get();
        }

        PCJ.barrier();

        System.out.println(PCJ.myId() + "a=" + PCJ.getLocal(SharedEnum.a) + " " + PCJ.getLocal(SharedEnum.a).getClass());
        System.out.println(PCJ.myId() + "b=" + PCJ.getLocal(SharedEnum.b) + " " + PCJ.getLocal(SharedEnum.b).getClass());
        System.out.println(PCJ.myId() + "c=" + PCJ.getLocal(SharedEnum.c));
        PCJ.putLocal(SharedEnum.c, null);
        System.out.println(PCJ.myId() + "c=" + PCJ.getLocal(SharedEnum.c));
    }
}
