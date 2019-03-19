/*
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration for PCJ.
 *
 * <p>
 * Configuration reads System properties.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public final class Configuration {

    private static final Logger LOGGER = Logger.getLogger(Configuration.class.getName());
    /**
     * pcj.port (int) default: 8091
     */
    public static final int DEFAULT_PORT;
    /**
     * pcj.init.backlog (int) default: 4096
     */
    public static final int INIT_BACKLOG_COUNT;
    /**
     * pcj.init.retry.count (int) default: 3
     */
    public static final int INIT_RETRY_COUNT;
    /**
     * pcj.init.retry.delay (int in seconds) default: 5
     */
    public static final int INIT_RETRY_DELAY;
    /**
     * pcj.init.maxtime (int in seconds) default:
     * <p>
     * <tt>max(30, (pcj.init.retry.count + 1) * pcj.init.retry.delay)</tt>
     */
    public static final int INIT_MAXTIME;
    /**
     * pcj.buffer.chunksize (int) default: 8*1024
     */
    public static final int BUFFER_CHUNK_SIZE;
    /**
     * pcj.buffer.poolsize (int) default: 1024
     */
    public static final int BUFFER_POOL_SIZE;
    /**
     * pcj.net.workers.count (int) default: available processors
     */
    public static final int NET_WORKERS_COUNT;
    /**
     * pcj.net.workers.keepalive (int in seconds) default: 60
     */
    public static final int NET_WORKERS_KEEPALIVE;
    /**
     * pcj.net.workers.queuesize (int) default: 0
     * <ul>
     * <li> = 0 - synchronous queue</li>
     * <li> &gt; 0 - bounded queue</li>
     * <li> &lt; 0 - unbounded queue</li>
     * </ul>
     */
    public static final int NET_WORKERS_QUEUE_SIZE;
    /**
     * pcj.async.workers.count (int) default: available processors
     */
    public static final int ASYNC_WORKERS_COUNT;
    /**
     * pcj.async.workers.keepalive (int in seconds) default: 60
     */
    public static final int ASYNC_WORKERS_KEEPALIVE;
    /**
     * pcj.async.workers.queuesize (int) default: -1
     * <ul>
     * <li> = 0 - synchronous queue</li>
     * <li> &gt; 0 - bounded queue</li>
     * <li> &lt; 0 - unbounded queue</li>
     * </ul>
     */
    public static final int ASYNC_WORKERS_QUEUE_SIZE;

    /**
     * pcj.alive.heartbeat (int in seconds) default: 20
     */
    public static final int ALIVE_HEARTBEAT;
    /**
     * pcj.alive.timeout (int in seconds) default: 60
     */
    public static final int ALIVE_TIMEOUT;

    static {
        DEFAULT_PORT = getPropertyInt("pcj.port", 8091);
        INIT_BACKLOG_COUNT = getPropertyInt("pcj.init.backlog", 4096);
        INIT_RETRY_COUNT = getPropertyInt("pcj.init.retry.count", 3);
        INIT_RETRY_DELAY = getPropertyInt("pcj.init.retry.delay", 5);
        INIT_MAXTIME = getPropertyInt("pcj.init.maxtime", Math.max(30, (INIT_RETRY_COUNT + 1) * INIT_RETRY_DELAY));
        BUFFER_CHUNK_SIZE = getPropertyInt("pcj.buffer.chunksize", 8 * 1024);
        BUFFER_POOL_SIZE = getPropertyInt("pcj.buffer.poolsize", 1024);
        NET_WORKERS_COUNT = getPropertyInt("pcj.net.workers.count", Runtime.getRuntime().availableProcessors());
        NET_WORKERS_KEEPALIVE = getPropertyInt("pcj.net.workers.keepalive", 60);
        NET_WORKERS_QUEUE_SIZE = getPropertyInt("pcj.net.workers.queuesize", 0);
        ASYNC_WORKERS_COUNT = getPropertyInt("pcj.async.workers.count", Runtime.getRuntime().availableProcessors());
        ASYNC_WORKERS_KEEPALIVE = getPropertyInt("pcj.async.workers.keepalive", 60);
        ASYNC_WORKERS_QUEUE_SIZE = getPropertyInt("pcj.async.workers.queuesize", -1);
        ALIVE_HEARTBEAT = getPropertyInt("pcj.alive.heartbeat", 20);
        ALIVE_TIMEOUT = getPropertyInt("pcj.alive.timeout", 60);

        LOGGER.log(Level.CONFIG, "pcj.port:                     {0,number,#}", DEFAULT_PORT);
        LOGGER.log(Level.CONFIG, "pcj.init.backlog:             {0,number,#}", INIT_BACKLOG_COUNT);
        LOGGER.log(Level.CONFIG, "pcj.init.retry.count:         {0,number,#}", INIT_RETRY_COUNT);
        LOGGER.log(Level.CONFIG, "pcj.init.retry.delay:         {0,number,#}", INIT_RETRY_DELAY);
        LOGGER.log(Level.CONFIG, "pcj.init.maxtime:             {0,number,#}", INIT_MAXTIME);
        LOGGER.log(Level.CONFIG, "pcj.buffer.chunksize:         {0,number,#}", BUFFER_CHUNK_SIZE);
        LOGGER.log(Level.CONFIG, "pcj.buffer.poolsize:          {0,number,#}", BUFFER_POOL_SIZE);
        LOGGER.log(Level.CONFIG, "pcj.net.workers.count:        {0,number,#}", NET_WORKERS_COUNT);
        LOGGER.log(Level.CONFIG, "pcj.net.workers.keepalive:    {0,number,#}", NET_WORKERS_KEEPALIVE);
        LOGGER.log(Level.CONFIG, "pcj.net.workers.queuesize:    {0,number,#}", NET_WORKERS_QUEUE_SIZE);
        LOGGER.log(Level.CONFIG, "pcj.async.workers.count:      {0,number,#}", ASYNC_WORKERS_COUNT);
        LOGGER.log(Level.CONFIG, "pcj.async.workers.keepalive:  {0,number,#}", ASYNC_WORKERS_KEEPALIVE);
        LOGGER.log(Level.CONFIG, "pcj.async.workers.queuesize:  {0,number,#}", ASYNC_WORKERS_QUEUE_SIZE);
        LOGGER.log(Level.CONFIG, "pcj.alive.heartbeat:          {0,number,#}", ALIVE_HEARTBEAT);
        LOGGER.log(Level.CONFIG, "pcj.alive.timeout:            {0,number,#}", ALIVE_TIMEOUT);
    }

    private static int getPropertyInt(String name, int defaultValue) {
        try {
            return Integer.parseInt(System.getProperty(name, String.valueOf(defaultValue)));
        } catch (NumberFormatException ex) {
            LOGGER.log(Level.CONFIG, "Unable to parse to int: " + name, ex);
        }
        return defaultValue;
    }
}
