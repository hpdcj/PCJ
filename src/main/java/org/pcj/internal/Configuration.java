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
final public class Configuration {

    private static final Logger LOGGER = Logger.getLogger(Configuration.class.getName());
    /**
     * pcj.port (int) default: 8091
     */
    final public static int DEFAULT_PORT;
    /**
     * pcj.init.backlog (int) default: 4096
     */
    final public static int INIT_BACKLOG_COUNT;
    /**
     * pcj.init.retry.count (int) default: 3
     */
    final public static int INIT_RETRY_COUNT;
    /**
     * pcj.init.retry.delay (int in seconds) default: 5
     */
    final public static int INIT_RETRY_DELAY;
    /**
     * pcj.init.maxtime (int in seconds) default: max(30, (pcj.init.retry.count+1) *
     * pcj.init.retry.delay)
     */
    final public static int INIT_MAXTIME;
    /**
     * pcj.buffer.chunksize (int) default: 8*1024
     */
    final public static int BUFFER_CHUNK_SIZE;
    /**
     * pcj.buffer.poolsize (int) default: 1024
     */
    final public static int BUFFER_POOL_SIZE;
    /**
     * pcj.workers.min  (int) default: node.threadCount + 1
     */
    final public static int WORKERS_MIN_COUNT;
    /**
     * pcj.workers.max (int in seconds) default: node.threadCount + 1
     */
    final public static int WORKERS_MAX_COUNT;
    /**
     * pcj.workers.keepalive (int) default: 60
     */
    final public static int WORKERS_KEEPALIVE;
    /**
     * pcj.workers.queuesize (int) default: 1024
     */
    final public static int WORKERS_QUEUE_SIZE;

    static {
        DEFAULT_PORT = getPropertyInt("pcj.port", 8091);
        INIT_BACKLOG_COUNT = getPropertyInt("pcj.init.backlog", 4096);
        INIT_RETRY_COUNT = getPropertyInt("pcj.init.retry.count", 3);
        INIT_RETRY_DELAY = getPropertyInt("pcj.init.retry.delay", 5);
        INIT_MAXTIME = getPropertyInt("pcj.init.maxtime", Math.max(30, (INIT_RETRY_COUNT + 1) * INIT_RETRY_DELAY));
        BUFFER_CHUNK_SIZE = getPropertyInt("pcj.buffer.chunksize", 8 * 1024);
        BUFFER_POOL_SIZE = getPropertyInt("pcj.buffer.poolsize", 1024);
        WORKERS_MIN_COUNT = getPropertyInt("pcj.workers.min", -1);
        WORKERS_MAX_COUNT = getPropertyInt("pcj.workers.max", -1);
        WORKERS_KEEPALIVE = getPropertyInt("pcj.workers.keepalive", 60);
        WORKERS_QUEUE_SIZE = getPropertyInt("pcj.workers.queuesize", 1024);

        LOGGER.log(Level.CONFIG, "pcj.port:              {0,number,#}", DEFAULT_PORT);
        LOGGER.log(Level.CONFIG, "pcj.init.backlog:      {0,number,#}", INIT_BACKLOG_COUNT);
        LOGGER.log(Level.CONFIG, "pcj.init.retry.count:  {0,number,#}", INIT_RETRY_COUNT);
        LOGGER.log(Level.CONFIG, "pcj.init.retry.delay:  {0,number,#}", INIT_RETRY_DELAY);
        LOGGER.log(Level.CONFIG, "pcj.init.maxtime:      {0,number,#}", INIT_MAXTIME);
        LOGGER.log(Level.CONFIG, "pcj.buffer.chunksize:  {0,number,#}", BUFFER_CHUNK_SIZE);
        LOGGER.log(Level.CONFIG, "pcj.buffer.poolsize:   {0,number,#}", BUFFER_POOL_SIZE);
        LOGGER.log(Level.CONFIG, "pcj.workers.min:       {0,number,#}", WORKERS_MIN_COUNT);
        LOGGER.log(Level.CONFIG, "pcj.workers.max:       {0,number,#}", WORKERS_MAX_COUNT);
        LOGGER.log(Level.CONFIG, "pcj.workers.keepalive: {0,number,#}", WORKERS_KEEPALIVE);
        LOGGER.log(Level.CONFIG, "pcj.workers.queuesize: {0,number,#}", WORKERS_QUEUE_SIZE);
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
