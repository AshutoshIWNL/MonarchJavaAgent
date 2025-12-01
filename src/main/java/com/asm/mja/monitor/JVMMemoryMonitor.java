package com.asm.mja.monitor;


import com.asm.mja.metrics.MetricsSnapshot;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

import static com.asm.mja.utils.EmailUtils.sendHeapUsageAlert;

/**
 * @author ashut
 * @since 19-04-2024
 */

public class JVMMemoryMonitor extends AbstractMonitor {

    private static final double MEMORY_THRESHOLD_PERCENT = 0.9;
    private static volatile JVMMemoryMonitor instance = null;
    private static final long SLEEP_DURATION = 30 * 1000;

    private JVMMemoryMonitor() {
        super("monarch-jvmmemory");
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

    @Override
    protected String getMonitorName() {
        return "JVM Memory monitor";
    }

    @Override
    public void run() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        while(true) {
            try {
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

                if (isExposeMetrics) {
                    MetricsSnapshot.getInstance().updateHeapMetrics(
                            used,
                            max,
                            committed,
                            ((double) used /max) * 100,
                            ((double) used /committed) * 100
                    );
                }

                Thread.sleep(SLEEP_DURATION);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                logger.error("Error in JVM Memory Monitor: " + e.getMessage());
            }
        }
    }
}
