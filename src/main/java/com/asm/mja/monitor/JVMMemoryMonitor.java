package com.asm.mja.monitor;

import com.asm.mja.logging.TraceFileLogger;
import com.asm.mja.utils.EmailUtils;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.List;

import static com.asm.mja.utils.EmailUtils.sendHeapUsageAlert;

/**
 * @author ashut
 * @since 19-04-2024
 */

public class JVMMemoryMonitor implements Runnable {

    private TraceFileLogger logger;
    private Thread thread = null;

    private static final double MEMORY_THRESHOLD_PERCENT = 0.9;

    private static volatile JVMMemoryMonitor instance = null;

    private JVMMemoryMonitor() {

    }

    public static JVMMemoryMonitor getInstance() {
        if(instance == null) {
            synchronized (JVMMemoryMonitor.class) {
                if (instance == null) {
                    instance = new JVMMemoryMonitor();
                }
            }
        }
        return instance;
    }

    public void setLogger(TraceFileLogger logger) {
        this.logger = logger;
    }

    @Override
    public void run() {
        while(true) {
            try {
                MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
                long used = memoryMXBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
                long max = memoryMXBean.getHeapMemoryUsage().getMax() / (1024 * 1024);
                long committed = memoryMXBean.getHeapMemoryUsage().getCommitted() / (1024 * 1024);
                String memoryString = "{USED: " + used + "MB | COMMITTED: " + committed + "MB | MAX: " + max + "MB}";
                logger.trace(memoryString);
                long threshold = (long) (max * MEMORY_THRESHOLD_PERCENT);
                if(used > threshold) {
                    logger.warn("Memory usage exceeds 90% of max heap");
                    sendHeapUsageAlert(String.valueOf(used));
                }
                Thread.sleep(5 * 1000L);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                logger.error("Error in JVM Memory Monitor: " + e.getMessage());
            }
        }
    }

    public void execute() {
        if (thread != null && thread.isAlive()) {
            logger.warn("JVM memory monitor is already running");
            return;
        }
        logger.trace("Starting JVM memory monitor");
        thread = new Thread(this, "monarch-jvmmemory");
        thread.setDaemon(true);
        thread.start();
    }

    public void shutdown() {
        if(thread != null) {
            logger.trace("Shutting down JVM memory monitor");
            thread.interrupt();
            thread = null;
        }
    }

    public boolean isDown() {
        return thread == null;
    }
}
