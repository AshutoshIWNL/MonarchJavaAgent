package com.asm.mja.monitor;

import com.asm.mja.utils.EmailUtils;

import java.lang.management.ManagementFactory;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import com.sun.management.OperatingSystemMXBean;

/**
 * @author ashut
 * @since 09-02-2025
 */

public class JVMCPUMonitor extends AbstractMonitor  {

    private static final double CPU_THRESHOLD_PERCENT = 0.9;
    private static volatile JVMCPUMonitor instance = null;
    private static final long SLEEP_DURATION = 10 * 1000;

    private JVMCPUMonitor() {
        super("monarch-jvmcpu");
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

    @Override
    protected String getMonitorName() {
        return "JVM CPU monitor";
    }

    @Override
    public void run() {
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(
                OperatingSystemMXBean.class);
        NumberFormat formatter = new DecimalFormat("#0.00");

        while (true) {
            try {
                double processCpuLoad = osBean.getProcessCpuLoad();
                double processCpuLoadPercent = processCpuLoad * 100;
                String cpuLoadPercent = formatter.format(processCpuLoadPercent);
                logger.trace("Current JVM CPU Load: " + cpuLoadPercent + "%");

                if (processCpuLoadPercent > CPU_THRESHOLD_PERCENT * 100) {
                    logger.warn("JVM CPU usage exceeds " + (CPU_THRESHOLD_PERCENT * 100) + "%");
                    EmailUtils.sendCPULoadAlert(cpuLoadPercent);
                }

                Thread.sleep(SLEEP_DURATION);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                logger.error("Error in JVM CPU Monitor: " + e.getMessage());
            }
        }
    }
}
