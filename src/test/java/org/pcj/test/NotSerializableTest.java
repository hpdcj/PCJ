/*
 * Copyright (c) 2019, Marek Nowicki
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
import org.pcj.AsyncTask;
import org.pcj.NodesDescription;
import org.pcj.PCJ;
import org.pcj.PcjRuntimeException;
import org.pcj.RegisterStorage;
import org.pcj.StartPoint;
import org.pcj.Storage;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
@RegisterStorage(NotSerializableTest.Shared.class)
public class NotSerializableTest implements StartPoint {


    @Storage(NotSerializableTest.class)
    enum Shared {
        object;
    }

    private Object object;

    public static void main(String[] args) {
        Level level = Level.INFO;
//        Level level = Level.CONFIG;
//        Level level = Level.FINEST;
        Logger logger = Logger.getLogger("");
        Arrays.stream(logger.getHandlers()).forEach(handler -> handler.setLevel(level));
        logger.setLevel(level);

        NodesDescription nodesDescription = new NodesDescription(new String[]{
                "localhost"});

        PCJ.deploy(NotSerializableTest.class, nodesDescription);
    }

    private void check(String method, String exceptionMessage, Runnable r) {
        try {
            System.out.print(method + ": ");
            r.run();
            throw new IllegalStateException("Not throwing exception");
        } catch (PcjRuntimeException ex) {
            if (exceptionMessage.equals(ex.getMessage())) System.out.println("OK");
            else {
                System.out.println("ERROR: wrong exception message");
                ex.printStackTrace();
            }
        } catch (Throwable ex) {
            System.out.print("ERROR: ");
            ex.printStackTrace();
        }
    }

    @Override
    public void main() {
        check("PCJ.get", "Getting value failed", () -> {
            object = new Object();
            PCJ.get(0, Shared.object);
        });

        check("PCJ.put", "Putting value failed", () -> {
            object = new Object();
            PCJ.put(new Object(), 0, Shared.object);
        });

        check("PCJ.broadcast", "Broadcasting value failed", () -> {
            PCJ.broadcast(new Object(), Shared.object);
        });

        check("PCJ.collect", "Collecting values failed", () -> {
            PCJ.collect(Shared.object);
        });

        check("PCJ.reduce", "Reducing values failed", () -> {
            PCJ.reduce((a, b) -> false, Shared.object);
        });

        check("PCJ.at(return)", "Asynchronous execution failed", () -> {
            PCJ.at(0, (AsyncTask<Object>) Object::new);
        });

        check("PCJ.at", "Asynchronous execution failed", () -> {
            Object o = new Object();
            PCJ.at(0, () -> System.out.println(o.getClass()));
        });
    }
}
