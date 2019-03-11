/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.test;

import java.io.Serializable;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pcj.NodesDescription;
import org.pcj.PCJ;
import org.pcj.PcjFuture;
import org.pcj.RegisterStorage;
import org.pcj.StartPoint;
import org.pcj.Storage;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
@RegisterStorage(CollectTest.Communicable.class)
public class CollectTest implements StartPoint {

    @Storage(CollectTest.class)
    enum Communicable {
        intArray,
        nullObject
    }

    private int[] intArray = new int[1];
    private Serializable nullObject;

    public static void main(String[] args) {
        Level level = Level.INFO;
//        Level level = Level.FINEST;
        Logger logger = Logger.getLogger("");
        Arrays.stream(logger.getHandlers()).forEach(handler -> handler.setLevel(level));
        logger.setLevel(level);

        NodesDescription nodesDescription = new NodesDescription(new String[]{
                "localhost:8091",
                "localhost:8002",
                "localhost:8003",
                "localhost:8004",
                "localhost:8005",
                "localhost:8006",
                "localhost:8007",
                "localhost:8008",
        });

        PCJ.deploy(CollectTest.class, nodesDescription);
    }

    @Override
    public void main() {
        intArray[0] = PCJ.myId() + 1;
        PCJ.barrier();
        if (PCJ.myId() == 0) {
            int[] intSubArray = PCJ.collect(Communicable.intArray,0);
            System.out.println(Arrays.toString(intSubArray));

            int[][] intArray = PCJ.collect(Communicable.intArray);
            System.out.println(Arrays.deepToString(intArray));

            PcjFuture<Serializable[]> nullObjectsFuture = PCJ.asyncCollect(Communicable.nullObject);
            System.out.println(Arrays.deepToString(nullObjectsFuture.get()));
        }
    }
}
