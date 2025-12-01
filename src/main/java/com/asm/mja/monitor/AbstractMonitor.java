package com.asm.mja.monitor;

import com.asm.mja.logging.TraceFileLogger;

/**
* @author ashut
* @since 06-10-2025
*/

public abstract class AbstractMonitor implements Runnable {
    protected TraceFileLogger logger;
    protected Thread thread = null;
    protected final String threadName;
    protected volatile boolean isExposeMetrics;

    protected AbstractMonitor(String threadName) {
        this.threadName = threadName;
    }

    public void setLogger(TraceFileLogger logger) {
        this.logger = logger;
    }

    public void setExposeMetrics(boolean isExposeMetrics) {
        this.isExposeMetrics = isExposeMetrics;
    }

    public void execute() {
        if (thread != null && thread.isAlive()) {
            logger.warn(getMonitorName() + " is already running");
            return;
        }
        logger.trace("Starting " + getMonitorName());
        thread = new Thread(this, threadName);
        thread.setDaemon(true);
        thread.start();
    }

    public void shutdown() {
        if (thread != null) {
            logger.trace("Shutting down " + getMonitorName());
            thread.interrupt();
            thread = null;
        }
    }

    public boolean isDown() {
        return thread == null;
    }

    protected abstract String getMonitorName();
}
