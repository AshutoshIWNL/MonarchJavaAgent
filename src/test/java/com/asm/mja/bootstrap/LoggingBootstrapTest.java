package com.asm.mja.bootstrap;

import com.asm.mja.logging.AgentLogger;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for logger bootstrap behavior.
 * @author ashut
 * @since 22-03-2026
 */
public class LoggingBootstrapTest {

    @Test
    void setupLoggerWritesToProvidedDirectory() throws Exception {
        Path logDir = Files.createTempDirectory("mja-log-bootstrap");
        String args = "agentLogFileDir=" + logDir + ",agentLogLevel=INFO";
        String marker = "test-marker-" + UUID.randomUUID();

        LoggingBootstrap.setupLogger(args);
        AgentLogger.info(marker);
        AgentLogger.deinit();

        Path logFile = logDir.resolve("monarchAgent.log");
        assertTrue(Files.exists(logFile));
        assertTrue(new String(Files.readAllBytes(logFile), StandardCharsets.UTF_8).contains(marker));
    }

    @Test
    void setupLoggerFallsBackToDefaultWhenDirectoryOrLevelInvalid() throws Exception {
        Path missingDir = Paths.get(System.getProperty("java.io.tmpdir"), "mja-missing-" + UUID.randomUUID());
        String args = "agentLogFileDir=" + missingDir + ",agentLogLevel=BAD_LEVEL";
        String marker = "fallback-marker-" + UUID.randomUUID();

        LoggingBootstrap.setupLogger(args);
        AgentLogger.info(marker);
        AgentLogger.deinit();

        Path defaultLog = Paths.get(System.getProperty("java.io.tmpdir"), "monarchAgent.log");
        assertTrue(Files.exists(defaultLog));
        assertTrue(new String(Files.readAllBytes(defaultLog), StandardCharsets.UTF_8).contains(marker));
    }
}
