/* 
 * Copyright (c) 2016, Marek Nowicki
 * All rights reserved.
 * 
 * Licensed under New BSD License (3-clause license).
 * 
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.test;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import org.pcj.NodesDescription;
import org.pcj.PCJ;
import org.pcj.StartPoint;
import org.pcj.Storage;
import org.pcj.internal.InternalStorages;
import org.pcj.test.MyTest.SharedEnum;
import org.pcj.RegisterStorage;

/**
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
@RegisterStorage(SharedEnum.class)
public class MyTest extends InternalStorages implements StartPoint {

    @Storage(MyTest.class)
    enum SharedEnum {
        a, b, c;
    }
    double a, b;
    Double c;

    public static void main(String[] args) throws InterruptedException {
        Level level = Level.INFO;
//        Level level = Level.FINEST;
        Logger logger = Logger.getLogger("");
        Arrays.stream(logger.getHandlers()).forEach(handler -> handler.setLevel(level));
        logger.setLevel(level);

        NodesDescription nodesDescription = new NodesDescription(new String[]{
            "localhost:8091",
            "localhost:8091",
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
        //                    "localhost:8010", "localhost:8011", "localhost:8012", "localhost:8013", "localhost:8014",
        //                    "localhost:8015", "localhost:8016", "localhost:8017", "localhost:8018", "localhost:8019",
        //                    "localhost:8020", "localhost:8021", "localhost:8022", "localhost:8023", "localhost:8024",
        //                    "localhost:8025", "localhost:8026", "localhost:8027", "localhost:8028", "localhost:8029",
        //                    "localhost:8030", "localhost:8031", "localhost:8032", "localhost:8033", "localhost:8034",
        //                    "localhost:8035", "localhost:8036", "localhost:8037", "localhost:8038", "localhost:8039",
        //                    "localhost:8040", "localhost:8041", "localhost:8042", "localhost:8043", "localhost:8044",
        //                    "localhost:8045", "localhost:8046", "localhost:8047", "localhost:8048", "localhost:8049",
        });

//        PCJ.start(EasyTest.class, EasyTest.class,
        PCJ.deploy(MyTest.class, nodesDescription);
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
        a = PCJ.myId();
        b = 'b';
        c = 2.0;
        PCJ.barrier();
        if (PCJ.myId() == 0) {
            System.out.println("a@1 = " + PCJ.asyncGet(1, SharedEnum.a).get());
        } else if (PCJ.myId() == 1) {
            PCJ.asyncPut(1000, 0, SharedEnum.a).get();
        }

        PCJ.barrier();

        System.out.println(PCJ.myId() + "a=" + a);
        System.out.println(PCJ.myId() + "b=" + b);
        System.out.println(PCJ.myId() + "c=" + c);
        c = null;
        System.out.println(PCJ.myId() + "c=" + c);
    }
}