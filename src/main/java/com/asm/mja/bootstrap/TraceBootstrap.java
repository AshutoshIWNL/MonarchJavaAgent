package com.asm.mja.bootstrap;

import com.asm.mja.logging.TraceFileLogger;
import com.asm.mja.utils.DateUtils;
import com.asm.mja.utils.JVMUtils;

import java.io.File;

/**
 * Bootstraps trace file logger and trace directory.
 * @author ashut
 * @since 22-03-2026
 */
public class TraceBootstrap {

    private TraceBootstrap() {
    }

    public static TraceFileLogger setupTraceFileLogger(String traceFileLocation) {
        TraceFileLogger traceFileLogger;
        String traceDir = traceFileLocation + File.separator + "Monarch_" + JVMUtils.getJVMPID() + "_" + DateUtils.getFormattedTimestampForFileName();
        File traceDirObj = new File(traceDir);
        if (traceDirObj.mkdir()) {
            traceFileLogger = TraceFileLogger.getInstance();
            traceFileLogger.init(traceDirObj.getAbsolutePath());
        } else {
            traceFileLogger = TraceFileLogger.getInstance();
            traceFileLogger.init(traceFileLocation);
        }
        return traceFileLogger;
    }
}
