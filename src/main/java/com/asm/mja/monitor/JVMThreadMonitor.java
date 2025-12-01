package com.asm.mja.monitor;

import com.asm.mja.metrics.MetricsSnapshot;
import com.asm.mja.utils.EmailUtils;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;

/**
 * @author ashut
 * @since 06-10-2025
 */

public class JVMThreadMonitor extends AbstractMonitor {

    private static final int THREAD_THRESHOLD = 5000;
    private static volatile JVMThreadMonitor instance = null;
    private static final long SLEEP_DURATION = 60 * 1000;

    private JVMThreadMonitor() {
        super("monarch-jvmthread");
    }

    public static JVMThreadMonitor getInstance() {
        if(instance == null) {
            synchronized (JVMThreadMonitor.class) {
                if (instance == null) {
                    instance = new JVMThreadMonitor();
                }
            }
        }
        return instance;
    }

    @Override
    protected String getMonitorName() {
        return "JVM Thread monitor";
    }

    @Override
    public void run() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

        while (true) {
            try {
                int threadCount = threadBean.getThreadCount();
                int peakThreadCount = threadBean.getPeakThreadCount();
                int daemonThreadCount = threadBean.getDaemonThreadCount();

                logger.trace(String.format("Thread Stats - Current: %d, Peak: %d, Daemon: %d",
                        threadCount, peakThreadCount, daemonThreadCount));

                // Check for deadlocks
                long[] deadlockedThreads = threadBean.findDeadlockedThreads();
                if (deadlockedThreads != null && deadlockedThreads.length > 0) {
                    logger.error("Deadlock detected! Thread IDs: " + Arrays.toString(deadlockedThreads));
                    EmailUtils.sendDeadlockAlert(deadlockedThreads.length);
                }

                if (threadCount > THREAD_THRESHOLD) {
                    logger.warn("Thread count exceeds threshold: " + threadCount);
                    EmailUtils.sendThreadCountAlert(threadCount);
                }

                if (isExposeMetrics) {
                    MetricsSnapshot.getInstance().updateThreadMetrics(
                            threadCount,
                            peakThreadCount,
                            daemonThreadCount,
                            threadBean.getTotalStartedThreadCount(),
                            deadlockedThreads != null ? deadlockedThreads.length : 0
                    );
                }

                Thread.sleep(SLEEP_DURATION);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                logger.error("Error in JVM Thread Monitor: " + e.getMessage());
            }
        }
    }
}