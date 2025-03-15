package com.asm.mja.utils;

import com.asm.mja.logging.TraceFileLogger;
import com.sun.management.HotSpotDiagnosticMXBean;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;

/**
 * @author ashut
 * @since 05-05-2024
 */

public class HeapDumpUtils {

    private static final TraceFileLogger logger = TraceFileLogger.getInstance();
    private static int MAX_HEAP_COUNT = 2;
    private static int heapDumpsCollected = 0;
    private static final int JVMPID = JVMUtils.getJVMPID();

    public static void collectHeap() {
        if (heapDumpsCollected < MAX_HEAP_COUNT) {
            try {
                HotSpotDiagnosticMXBean bean = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
                bean.dumpHeap(logger.traceDir + File.separator +  "heapDump_" + JVMPID + System.currentTimeMillis() + ".hprof", true);
                heapDumpsCollected++;
            } catch (IOException e) {
                // Do nothing
            }
        } else {
            logger.trace("Hit the maximum heap dump creation limit - " + MAX_HEAP_COUNT);
        }
    }

    public static void setMaxHeapCount(int maxHeapCount) {
        MAX_HEAP_COUNT = maxHeapCount;
    }
}
