package com.asm.mja.monitor;

import com.asm.mja.utils.EmailUtils;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ashut
 * @since 06-10-2025
 */

public class JVMGCMonitor extends AbstractMonitor {

    private static final double GC_TIME_THRESHOLD_PERCENT = 10.0; // 10% time in GC
    private static volatile JVMGCMonitor instance = null;
    private static final long SLEEP_DURATION = 60 * 1000;

    private Map<String, Long> previousGCCount = new HashMap<>();
    private Map<String, Long> previousGCTime = new HashMap<>();
    private long previousCheckTime = System.currentTimeMillis();

    private JVMGCMonitor() {
        super("monarch-jvmgc");
    }

    public static JVMGCMonitor getInstance() {
        if(instance == null) {
            synchronized (JVMGCMonitor.class) {
                if (instance == null) {
                    instance = new JVMGCMonitor();
                }
            }
        }
        return instance;
    }

    @Override
    protected String getMonitorName() {
        return "JVM GC monitor";
    }

    @Override
    public void run() {
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

        while (true) {
            try {
                long currentTime = System.currentTimeMillis();
                long elapsedTime = currentTime - previousCheckTime;

                StringBuilder gcStats = new StringBuilder("GC Stats - ");
                long totalGCTime = 0;

                for (GarbageCollectorMXBean gcBean : gcBeans) {
                    String gcName = gcBean.getName();
                    long gcCount = gcBean.getCollectionCount();
                    long gcTime = gcBean.getCollectionTime();

                    long prevCount = previousGCCount.getOrDefault(gcName, 0L);
                    long prevTime = previousGCTime.getOrDefault(gcName, 0L);

                    long countDiff = gcCount - prevCount;
                    long timeDiff = gcTime - prevTime;

                    gcStats.append(String.format("%s: %d collections, %dms | ",
                            gcName, countDiff, timeDiff));

                    totalGCTime += timeDiff;

                    previousGCCount.put(gcName, gcCount);
                    previousGCTime.put(gcName, gcTime);
                }

                logger.trace(gcStats.toString());

                // Calculate percentage of time spent in GC
                double gcTimePercent = (totalGCTime * 100.0) / elapsedTime;
                if (gcTimePercent > GC_TIME_THRESHOLD_PERCENT) {
                    logger.warn(String.format("High GC activity: %.2f%% time spent in GC", gcTimePercent));
                    EmailUtils.sendGCAlert(gcTimePercent);
                }

                previousCheckTime = currentTime;
                Thread.sleep(SLEEP_DURATION);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                logger.error("Error in JVM GC Monitor: " + e.getMessage());
            }
        }
    }
}
