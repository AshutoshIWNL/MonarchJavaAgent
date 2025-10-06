package com.asm.mja.monitor;

import com.asm.mja.utils.EmailUtils;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * @author ashut
 * @since 06-10-2025
 */

public class JVMClassLoaderMonitor extends AbstractMonitor {

    private static final double METASPACE_THRESHOLD_PERCENT = 0.85;
    private static final int CLASS_LOAD_RATE_THRESHOLD = 100; // classes per minute
    private static final long SLEEP_DURATION = 60 * 1000;
    private static final long WARMUP_PERIOD_MS = 2 * 60 * 1000; // ignore first 2 mins
    private static final int ROLLING_WINDOW = 5; // number of intervals to average

    private static volatile JVMClassLoaderMonitor instance = null;

    private long previousLoadedClassCount = 0;
    private long previousCheckTime = System.currentTimeMillis();
    private final long startTime = System.currentTimeMillis();

    private final Deque<Double> recentLoadRates = new ArrayDeque<>();

    private JVMClassLoaderMonitor() {
        super("monarch-jvmclassloader");
    }

    public static JVMClassLoaderMonitor getInstance() {
        if (instance == null) {
            synchronized (JVMClassLoaderMonitor.class) {
                if (instance == null) {
                    instance = new JVMClassLoaderMonitor();
                }
            }
        }
        return instance;
    }

    @Override
    protected String getMonitorName() {
        return "JVM ClassLoader monitor";
    }

    @Override
    public void run() {
        ClassLoadingMXBean classLoadingBean = ManagementFactory.getClassLoadingMXBean();
        NumberFormat formatter = new DecimalFormat("#0.00");

        while (true) {
            try {
                long currentTime = System.currentTimeMillis();
                long elapsedTime = currentTime - previousCheckTime;

                long loadedClassCount = classLoadingBean.getLoadedClassCount();
                long totalLoadedClassCount = classLoadingBean.getTotalLoadedClassCount();
                long unloadedClassCount = classLoadingBean.getUnloadedClassCount();

                // calculate class load rate
                long classesLoadedSinceLastCheck = totalLoadedClassCount - previousLoadedClassCount;
                double classLoadRatePerMinute = (classesLoadedSinceLastCheck * 60000.0) / elapsedTime;

                // add to rolling window
                if (recentLoadRates.size() == ROLLING_WINDOW) recentLoadRates.removeFirst();
                recentLoadRates.addLast(classLoadRatePerMinute);

                double averageRate = recentLoadRates.stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0);

                logger.trace(String.format(
                        "ClassLoader Stats - Current: %d, Total Loaded: %d, Unloaded: %d, Load Rate: %.1f/min, Avg Rate: %.1f/min",
                        loadedClassCount, totalLoadedClassCount, unloadedClassCount,
                        classLoadRatePerMinute, averageRate
                ));

                // Metaspace usage
                MemoryUsage metaspaceUsage = getMetaspaceUsage();
                if (metaspaceUsage != null) {
                    long metaspaceUsed = metaspaceUsage.getUsed() / (1024 * 1024);
                    long metaspaceMax = metaspaceUsage.getMax() / (1024 * 1024);

                    if (metaspaceMax > 0) {
                        double metaspaceUsedPercent = (metaspaceUsage.getUsed() * 100.0) / metaspaceUsage.getMax();
                        logger.trace(String.format("Metaspace - Used: %dMB, Max: %dMB (%.2f%%)", metaspaceUsed, metaspaceMax, metaspaceUsedPercent));

                        if (metaspaceUsedPercent > METASPACE_THRESHOLD_PERCENT * 100) {
                            logger.warn(String.format("Metaspace usage exceeds %.0f%%: %.2f%%",
                                    METASPACE_THRESHOLD_PERCENT * 100, metaspaceUsedPercent));
                            EmailUtils.sendMetaspaceAlert(formatter.format(metaspaceUsedPercent), METASPACE_THRESHOLD_PERCENT);
                        }
                    }
                }

                // only alert after warm-up
                if (currentTime - startTime > WARMUP_PERIOD_MS && averageRate > CLASS_LOAD_RATE_THRESHOLD) {
                    logger.warn(String.format("High sustained class loading rate: %.1f classes/min (threshold: %d)",
                            averageRate, CLASS_LOAD_RATE_THRESHOLD));
                    EmailUtils.sendClassLoadRateAlert(averageRate, CLASS_LOAD_RATE_THRESHOLD);
                }

                // possible classloader leak detection
                if (loadedClassCount > 10000 && unloadedClassCount == 0) {
                    logger.warn(String.format("No classes unloaded with %d loaded classes - possible classloader leak", loadedClassCount));
                }

                previousLoadedClassCount = totalLoadedClassCount;
                previousCheckTime = currentTime;

                Thread.sleep(SLEEP_DURATION);

            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                logger.error("Error in JVM ClassLoader Monitor: " + e.getMessage());
            }
        }
    }

    private MemoryUsage getMetaspaceUsage() {
        List<MemoryPoolMXBean> memoryPools = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean pool : memoryPools) {
            if ("Metaspace".equals(pool.getName())) {
                return pool.getUsage();
            }
        }
        return null;
    }
}