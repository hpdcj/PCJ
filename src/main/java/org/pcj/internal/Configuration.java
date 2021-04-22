/*
 * Copyright (c) 2011-2021, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pcj.PcjRuntimeException;

/**
 * Configuration for PCJ.
 *
 * <p>
 * Configuration reads System properties.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public final class Configuration {

    private final Logger LOGGER = Logger.getLogger(Configuration.class.getName());
    private final Properties properties;

    @Config(value = "pcj.port",
            type = int.class, defaultValue = "8091",
            description = "default PCJ port")
    private int defaultPort;

    @Config(value = "pcj.init.backlog",
            type = int.class, defaultValue = "4096",
            description = "socket backlog size")
    private int initBacklogCount;

    @Config(value = "pcj.init.retry.count",
            type = int.class, defaultValue = "3",
            description = "retry count")
    private int initRetryCount;

    @Config(value = "pcj.init.retry.delay",
            type = int.class, defaultValue = "5",
            description = "retry delay in seconds")
    private int initRetryDelay;

    @Config(value = "pcj.init.maxtime",
            type = int.class, defaultValue = "30",
            description = "maxtime in seconds")
    private int initMaxtime;

    @Config(value = "pcj.buffer.chunksize",
            type = int.class, defaultValue = "8192",
            description = "chunksize of buffer in bytes")
    private int bufferChunkSize;

    @Config(value = "pcj.buffer.poolsize",
            type = int.class, defaultValue = "1024",
            description = "number of buffers in pool")
    private int bufferPoolSize;

    @Config(value = "pcj.msg.workers.count",
            type = int.class, defaultValue = "#procs",
            description = "#procs evaluated on runtime")
    private int messageWorkersCount;

    @Config(value = "pcj.msg.workers.keepalive",
            type = int.class, defaultValue = "60",
            description = "keepalive in seconds")
    private int messageWorkersKeepalive;

    @Config(value = "pcj.async.workers.count",
            type = int.class, defaultValue = "#procs",
            description = "#procs evaluated on runtime")
    private int asyncWorkersCount;

    @Config(value = "pcj.async.workers.keepalive",
            type = int.class, defaultValue = "60",
            description = "keepalive in seconds")
    private int asyncWorkersKeepalive;

    @Config(value = "pcj.async.workers.queuesize",
            type = int.class, defaultValue = "-1",
            description = " = 0 - synchronous queue; > 0 - bounded queue; < 0 - unbounded queue")
    private int asyncWorkersQueueSize;

    @Config(value = "pcj.alive.heartbeat",
            type = int.class, defaultValue = "20",
            description = "heartbeat in seconds")
    private int aliveHeartbeat;

    @Config(value = "pcj.alive.timeout",
            type = int.class, defaultValue = "60",
            description = "timeout in seconds")
    private int aliveTimeout;

    Configuration(Properties properties) {
        this.properties = properties;

        for (Field field : Configuration.class.getDeclaredFields()) {
            Config configAnnotation = field.getAnnotation(Config.class);
            if (configAnnotation == null) {
                continue;
            }

            if (configAnnotation.type().equals(int.class)) {
                int defaultValue;
                if ("#procs".equals(configAnnotation.defaultValue())) {
                    defaultValue = Runtime.getRuntime().availableProcessors();
                } else {
                    defaultValue = Integer.parseInt(configAnnotation.defaultValue());
                }
                try {
                    field.setInt(this, getPropertyInt(configAnnotation.value(), defaultValue));
                    LOGGER.log(Level.CONFIG, "Configuration: " + configAnnotation.value() + " = {0,number,#}", field.get(this));
                } catch (IllegalAccessException e) {
                    throw new PcjRuntimeException("Unable to parse configuration: " + configAnnotation.value(), e);
                }
            }
        }
    }

    private int getPropertyInt(String name, int defaultValue) {
        try {
            return Integer.parseInt(getProperty(name, String.valueOf(defaultValue)));
        } catch (NumberFormatException ex) {
            LOGGER.log(Level.CONFIG, "Unable to parse to int: " + name, ex);
        }
        return defaultValue;
    }

    private String getProperty(String name, String defaultValue) {
        return properties.getProperty(name, System.getProperty(name, defaultValue));
    }

    public Properties getProperties() {
        return properties;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    private @interface Config {

        String value();

        Class<?> type();

        String defaultValue();

        String description();
    }

    public int getDefaultPort() {
        return defaultPort;
    }

    public int getInitBacklogCount() {
        return initBacklogCount;
    }

    public int getInitRetryCount() {
        return initRetryCount;
    }

    public int getInitRetryDelay() {
        return initRetryDelay;
    }

    public int getInitMaxtime() {
        return initMaxtime;
    }

    public int getBufferChunkSize() {
        return bufferChunkSize;
    }

    public int getBufferPoolSize() {
        return bufferPoolSize;
    }

    public int getMessageWorkersCount() {
        return messageWorkersCount;
    }

    public int getMessageWorkersKeepalive() {
        return messageWorkersKeepalive;
    }

    public int getAsyncWorkersCount() {
        return asyncWorkersCount;
    }

    public int getAsyncWorkersKeepalive() {
        return asyncWorkersKeepalive;
    }

    public int getAsyncWorkersQueueSize() {
        return asyncWorkersQueueSize;
    }

    public int getAliveHeartbeat() {
        return aliveHeartbeat;
    }

    public int getAliveTimeout() {
        return aliveTimeout;
    }
}
