package com.asm.mja.monitor;

import com.asm.mja.utils.EmailUtils;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

/**
 * @author ashut
 * @since 06-10-2025
 */

public class JVMClassLoaderMonitor extends AbstractMonitor {

    private static final double METASPACE_THRESHOLD_PERCENT = 0.85;
    private static final int CLASS_LOAD_RATE_THRESHOLD = 100; // classes per minute
    private static final long SLEEP_DURATION = 60 * 1000;

    private static volatile JVMClassLoaderMonitor instance = null;

    private long previousLoadedClassCount = 0;
    private long previousCheckTime = System.currentTimeMillis();

    private JVMClassLoaderMonitor() {
        super("monarch-jvmclassloader");
    }

    public static JVMClassLoaderMonitor getInstance() {
        if(instance == null) {
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

                // Class loading stats
                long loadedClassCount = classLoadingBean.getLoadedClassCount();
                long totalLoadedClassCount = classLoadingBean.getTotalLoadedClassCount();
                long unloadedClassCount = classLoadingBean.getUnloadedClassCount();

                // Calculate class load rate
                long classesLoadedSinceLastCheck = totalLoadedClassCount - previousLoadedClassCount;
                double classLoadRatePerMinute = (classesLoadedSinceLastCheck * 60000.0) / elapsedTime;

                logger.trace(String.format(
                        "ClassLoader Stats - Current: %d, Total Loaded: %d, Unloaded: %d, Load Rate: %.1f/min",
                        loadedClassCount, totalLoadedClassCount, unloadedClassCount, classLoadRatePerMinute
                ));

                // Check Metaspace usage (Java 8+)
                MemoryUsage metaspaceUsage = getMetaspaceUsage();
                if (metaspaceUsage != null) {
                    long metaspaceUsed = metaspaceUsage.getUsed() / (1024 * 1024);
                    long metaspaceMax = metaspaceUsage.getMax() / (1024 * 1024);

                    if (metaspaceMax > 0) {
                        double metaspaceUsedPercent = (metaspaceUsage.getUsed() * 100.0) / metaspaceUsage.getMax();

                        logger.trace(String.format(
                                "Metaspace - Used: %dMB, Max: %dMB (%.2f%%)",
                                metaspaceUsed, metaspaceMax, metaspaceUsedPercent
                        ));

                        if (metaspaceUsedPercent > METASPACE_THRESHOLD_PERCENT * 100) {
                            logger.warn(String.format(
                                    "Metaspace usage exceeds %.0f%%: %.2f%%",
                                    METASPACE_THRESHOLD_PERCENT * 100, metaspaceUsedPercent
                            ));
                            EmailUtils.sendMetaspaceAlert(formatter.format(metaspaceUsedPercent),
                                    METASPACE_THRESHOLD_PERCENT);
                        }
                    } else {
                        logger.trace(String.format("Metaspace - Used: %dMB (no max limit)", metaspaceUsed));
                    }
                }

                // Check for excessive class loading (potential classloader leak)
                if (classLoadRatePerMinute > CLASS_LOAD_RATE_THRESHOLD) {
                    logger.warn(String.format(
                            "High class loading rate detected: %.1f classes/min (threshold: %d)",
                            classLoadRatePerMinute, CLASS_LOAD_RATE_THRESHOLD
                    ));
                    EmailUtils.sendClassLoadRateAlert(classLoadRatePerMinute, CLASS_LOAD_RATE_THRESHOLD);
                }

                // Check if classes are being unloaded (good - means classloaders are being GC'd)
                // If loaded count keeps growing but unloaded count is 0, might indicate leak
                if (loadedClassCount > 10000 && unloadedClassCount == 0) {
                    logger.warn(String.format(
                            "No classes unloaded with %d loaded classes - possible classloader leak",
                            loadedClassCount
                    ));
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
