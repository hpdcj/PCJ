/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.pcj.internal.storage.InternalStorage;
import org.pcj.internal.utils.CloneObject;

/**
 * Runnable that deserializes incoming data using specified
 * ClassLoader and stores the deserialized object in proper
 * Storage.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class Deserializer implements Runnable {

    private InternalStorage storage;
    final private BlockingQueue<Event> queue;

    private interface Event {

        void doAction();
    }

    private class ResponseEvent implements Event {

        final private byte[] variableValue;
        final private ResponseAttachment attachment;

        private ResponseEvent(byte[] variableValue, ResponseAttachment attachment) {
            this.variableValue = variableValue;
            this.attachment = attachment;
        }

        @Override
        public void doAction() {
            Object value = deserializeObject(variableValue);

            attachment.setObject(value);
            if (attachment.isDone()) {
                synchronized (attachment) {
                    attachment.notifyAll();
                }
            }
        }
    }

    private class StorageEvent implements Event {

        final private byte[] variableValue;
        final private String variableName;
        final private int[] variableIndexes;

        private StorageEvent(byte[] variableValue, String variableName, int[] variableIndexes) {
            this.variableValue = variableValue;
            this.variableName = variableName;
            this.variableIndexes = variableIndexes;
        }

        @Override
        public void doAction() {
            Object value = deserializeObject(variableValue);
            storage.put(variableName, value, variableIndexes);
        }
    }

    public Deserializer(InternalStorage storage) {
        this.storage = storage;
        queue = new LinkedBlockingQueue<>();
    }

    @Override
    public void run() {
        Event event;

        try {
            for (;;) {
                event = queue.take();
                event.doAction();
            }
        } catch (InterruptedException ex) {
            //ex.printStackTrace(System.err);
        }

    }

    private Object deserializeObject(byte[] bytes) {
        try {
            return CloneObject.deserialize(bytes);
        } catch (final IOException | ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void add(Event event) {
        try {
            queue.put(event);
        } catch (InterruptedException ex) {
            ex.printStackTrace(System.err);
        }
    }

    void deserialize(byte[] variableValue, ResponseAttachment attachment) {
        add(new ResponseEvent(variableValue, attachment));
    }

    void deserialize(byte[] variableValue, String variableName, int... indexes) {
        add(new StorageEvent(variableValue, variableName, indexes));
    }
}
