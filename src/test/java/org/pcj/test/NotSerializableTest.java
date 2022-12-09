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
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pcj.AsyncTask;
import org.pcj.PCJ;
import org.pcj.PcjFuture;
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

        PCJ.executionBuilder(NotSerializableTest.class)
                .deploy();
    }

    private void check(String method, String exceptionMessage, Supplier<? extends PcjFuture<?>> r) {
        System.out.print(method + ": ");
        PcjFuture<?> future;
        try {
            future = r.get();
        } catch (Throwable ex) {
            System.out.print("ERROR when calling: ");
            ex.printStackTrace();
            return;
        }
        try {
            future.get();
            throw new IllegalStateException("Not throwing exception");
        } catch (PcjRuntimeException ex) {
            if (exceptionMessage.equals(ex.getMessage())) System.out.println("OK");
            else {
                System.out.println("ERROR: wrong exception message (" + ex.getMessage() + ")");
                ex.printStackTrace();
            }
        } catch (Throwable ex) {
            System.out.print("ERROR: ");
            ex.printStackTrace();
        }
    }

    @Override
    public void main() {
        check("PCJ.asyncGet", "Getting value failed",
                () -> {
                    object = new Object();
                    return PCJ.asyncGet(0, Shared.object);
                });

        check("PCJ.asyncPut", "Putting value failed",
                () -> PCJ.asyncPut(new Object(), 0, Shared.object));


        check("PCJ.asyncAccumulate", "Accumulating value failed",
                () -> PCJ.asyncAccumulate((a, b) -> false, new Object(), 0, Shared.object));

        check("PCJ.asyncBroadcast", "Broadcasting value failed",
                () -> PCJ.asyncBroadcast(new Object(), Shared.object));

        check("PCJ.asyncGather", "Gathering values failed",
                () -> PCJ.asyncGather(Shared.object));

        check("PCJ.asyncCollect", "Collecting values failed",
                () -> PCJ.asyncCollect(Collectors::toSet, Shared.object));

        check("PCJ.asyncReduce", "Reducing values failed",
                () -> PCJ.asyncReduce((a, b) -> false, Shared.object));

        check("PCJ.asyncAt(return)", "Asynchronous execution failed",
                () -> PCJ.asyncAt(0, (AsyncTask<Object>) Object::new));

        check("PCJ.asyncAt", "Asynchronous execution failed",
                () -> {
                    Object o = new Object();
                    return PCJ.asyncAt(0, () -> System.out.println(o.getClass()));
                });
    }
}
