package com.asm.mja;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that top-level agent entrypoints contain unexpected failures.
 */
public class AgentBoundaryExceptionContainmentTest {

    @Test
    void premainContainsFailureWithinBoundary() {
        assertDoesNotThrow(() -> Agent.premain("configFile=C:\\does-not-exist\\m.yaml", null));
    }

    @Test
    void premainContainsMalformedRuleFailureWithinBoundary() throws Exception {
        Path logDir = Files.createTempDirectory("mja-agent-log");
        Path traceDir = Files.createTempDirectory("mja-agent-trace");
        Path config = Files.createTempFile("mja-invalid-rule", ".yaml");
        String yaml =
                "mode: instrumenter\n" +
                "instrumentation:\n" +
                "  enabled: true\n" +
                "  configRefreshInterval: 1000\n" +
                "  traceFileLocation: \"" + traceDir.toString().replace("\\", "\\\\") + "\"\n" +
                "  agentRules:\n" +
                "    - com.example.Test::run@INGRESS::NOT_A_REAL_ACTION\n" +
                "observer:\n" +
                "  enabled: false\n" +
                "alerts:\n" +
                "  enabled: false\n";
        Files.write(config, yaml.getBytes(StandardCharsets.UTF_8));

        String args = "configFile=" + config + ",agentLogFileDir=" + logDir + ",agentLogLevel=INFO";
        assertDoesNotThrow(() -> Agent.premain(args, null));
    }

    @Test
    void agentmainContainsFailureWithinBoundary() {
        PrintStream originalErr = System.err;
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errBuffer));
        try {
            assertDoesNotThrow(() -> Agent.agentmain(null, null));
            String errorOutput = new String(errBuffer.toByteArray(), StandardCharsets.UTF_8);
            assertTrue(errorOutput.contains("Failure in agentmain:"));
        } finally {
            System.setErr(originalErr);
        }
    }
}
