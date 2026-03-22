package com.asm.mja.bootstrap;

import com.asm.mja.logging.TraceFileLogger;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for trace logger bootstrap behavior.
 * @author ashut
 * @since 22-03-2026
 */
public class TraceBootstrapTest {

    @Test
    void setupTraceFileLoggerInitializesTraceDirectory() throws Exception {
        Path traceRoot = Files.createTempDirectory("mja-trace-root");

        TraceFileLogger logger = TraceBootstrap.setupTraceFileLogger(traceRoot.toString());
        String traceDir = logger.getTraceDir();

        assertNotNull(traceDir);
        assertTrue(traceDir.startsWith(traceRoot.toString()));
        assertTrue(new File(traceDir).exists());

        logger.close();
    }
}
