/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.utils;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Helper class for halting JVM after specified time.
 *
 * @author faramir
 */
public class ExitTimer extends TimerTask {

    final private Timer timer;
    private String message;

    public ExitTimer() {
        timer = new Timer();
    }

    /**
     * @param delay in seconds
     */
    public void schedule(int delay, String message) {
        this.message = message;
        timer.schedule(this, delay);
    }

    @Override
    public void run() {
        System.err.println(message);
        System.exit(1);
    }

    public void stop() {
        timer.cancel();
        super.cancel();
    }
}
