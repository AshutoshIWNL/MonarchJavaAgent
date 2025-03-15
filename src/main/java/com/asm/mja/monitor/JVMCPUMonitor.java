package com.asm.mja.monitor;

import com.asm.mja.logging.TraceFileLogger;
import com.asm.mja.utils.EmailUtils;

import java.lang.management.ManagementFactory;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import com.sun.management.OperatingSystemMXBean;

/**
 * @author ashut
 * @since 09-02-2025
 */

public class JVMCPUMonitor implements Runnable {

    private TraceFileLogger logger;
    private Thread thread = null;

    private static final double CPU_THRESHOLD_PERCENT = 0.9;

    private static volatile JVMCPUMonitor instance = null;

    private JVMCPUMonitor() {
    }

    public static JVMCPUMonitor getInstance() {
        if(instance == null) {
            synchronized (JVMCPUMonitor.class) {
                if (instance == null) {
                    instance = new JVMCPUMonitor();
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
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(
                OperatingSystemMXBean.class);

        while (true) {
            try {
                double processCpuLoad = osBean.getProcessCpuLoad();
                double processCpuLoadPercent = processCpuLoad * 100;
                NumberFormat formatter = new DecimalFormat("#0.00");
                String cpuLoadPercent = formatter.format(processCpuLoadPercent);
                logger.trace("Current JVM CPU Load: " + cpuLoadPercent + "%");

                if (processCpuLoadPercent > CPU_THRESHOLD_PERCENT * 100) {
                    logger.warn("JVM CPU usage exceeds " + (CPU_THRESHOLD_PERCENT * 100) + "%");
                    EmailUtils.sendCPULoadAlert(cpuLoadPercent);
                }

                Thread.sleep(5 * 1000L);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                logger.error("Error in JVM CPU Monitor: " + e.getMessage());
            }
        }
    }

    public void execute() {
        if (thread != null && thread.isAlive()) {
            logger.warn("JVM CPU monitor is already running");
            return;
        }
        logger.trace("Starting JVM CPU monitor");
        thread = new Thread(this, "monarch-jvmcpu");
        thread.setDaemon(true);
        thread.start();
    }

    public void shutdown() {
        if (thread != null) {
            logger.trace("Shutting down JVM CPU monitor");
            thread.interrupt();
            thread = null;
        }
    }

    public boolean isDown() {
        return thread == null;
    }
}
