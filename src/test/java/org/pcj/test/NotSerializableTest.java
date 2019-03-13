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

//        PCJ.start(EasyTest.class, EasyTest.class,
        PCJ.deploy(NotSerializableTest.class, nodesDescription);
    }

    private void check(String method, String exceptionMessage, Runnable r) {
        try {
            System.out.print(method + ": ");
            r.run();
            System.out.println("ERROR");
        } catch (PcjRuntimeException ex) {
            if (exceptionMessage.equals(ex.getMessage())) System.out.println("OK");
            else {
                System.out.println("ERROR");
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void main() {
        check("PCJ.get", "Getting value failed", () -> {
            object = new Object();
            PCJ.get(0, Shared.object);
            throw new IllegalStateException("PCJ.get should throw an exception!");
        });

        try {
            PCJ.put(new Object(), 0, Shared.object);
            throw new IllegalStateException("PCJ.put should throw an exception!");
        } catch (PcjRuntimeException ex) {
            String message = ex.getMessage();
            if ("Putting value failed".equals(message)) System.out.println("OK: " + message);
            else throw new IllegalStateException(ex);
        }

        try {
            PCJ.broadcast(new Object(), Shared.object);
            throw new IllegalStateException("PCJ.broadcast should throw an exception!");
        } catch (PcjRuntimeException ex) {
            String message = ex.getMessage();
            if ("Broadcasting value failed".equals(message)) System.out.println("OK: " + message);
            else throw new IllegalStateException(ex);
        }

        try {
            PCJ.collect(Shared.object);
            throw new IllegalStateException("PCJ.collect should throw an exception!");
        } catch (PcjRuntimeException ex) {
            String message = ex.getMessage();
            if ("Collecting values failed".equals(message)) System.out.println("OK: " + message);
            else throw new IllegalStateException(ex);
        }

        try {
            PCJ.reduce((a, b) -> false, Shared.object);
            throw new IllegalStateException("PCJ.reduce should throw an exception!");
        } catch (PcjRuntimeException ex) {
            String message = ex.getMessage();
            if ("Reducing values failed".equals(message)) System.out.println("OK: " + message);
            else throw new IllegalStateException(ex);
        }

        try {
            PCJ.at(0, () -> {
                return new Object();
            });
            throw new IllegalStateException("PCJ.at(return) should throw an exception!");
        } catch (PcjRuntimeException ex) {
            String message = ex.getMessage();
            if ("Asynchronous execution failed".equals(message)) System.out.println("OK: " + message);
            else throw new IllegalStateException(ex);
        }

        try {
            Object o = new Object();
            PCJ.at(0, () -> {
                System.out.println(o.getClass());
            });
            throw new IllegalStateException("PCJ.at should throw an exception!");
        } catch (PcjRuntimeException ex) {
            String message = ex.getMessage();
            if ("Asynchronous execution failed".equals(message)) System.out.println("OK: " + message);
            else throw new IllegalStateException(ex);
        }
    }
}
