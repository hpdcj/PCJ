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
import org.pcj.NodesDescription;
import org.pcj.PCJ;
import org.pcj.Shared;
import org.pcj.StartPoint;

/**
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class SizesTest implements StartPoint {

    enum SharedEnum implements Shared {
        b(byte[].class);
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
            "localhost:8002",});

        PCJ.deploy(SizesTest.class, nodesDescription, SharedEnum.class);
    }

    @Override
    public void main() throws Throwable {
        for (int n = 0; n < 10000; ++n) {
            System.out.println("n=" + n);
            byte[] bytes = new byte[n];
            if (PCJ.myId() == 0) {
                PCJ.put(1, SharedEnum.b, bytes);
            }
            PCJ.barrier();
        }
        System.out.println("DONE");
    }
}
