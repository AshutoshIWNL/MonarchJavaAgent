package com.asm.mja.config;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for configuration parsing compatibility behavior.
 * @author ashut
 * @since 22-03-2026
 */
public class ConfigParserTest {

    @Test
    void parseSupportsLegacyFlatConfig() throws Exception {
        Path configFile = writeTempYaml("" +
                "shouldInstrument: true\n" +
                "configRefreshInterval: 15\n" +
                "traceFileLocation: C:\\\\TraceFileDumps\n" +
                "agentRules:\n" +
                "  - com.example.Foo::bar@INGRESS::ARGS\n" +
                "printJVMHeapUsage: true\n" +
                "printJVMCpuUsage: true\n" +
                "printJVMThreadUsage: false\n" +
                "printJVMGCStats: true\n" +
                "printJVMClassLoaderStats: false\n" +
                "printJVMSystemProperties: true\n" +
                "printEnvironmentVariables: false\n" +
                "printClassLoaderTrace: true\n" +
                "exposeMetrics: true\n" +
                "metricsPort: 9090\n" +
                "sendAlertEmails: true\n" +
                "maxHeapDumps: 3\n" +
                "emailRecipientList:\n" +
                "  - a@example.com\n");

        Config config = ConfigParser.parse(configFile.toString());

        assertEquals(AgentMode.HYBRID, config.getResolvedMode());
        assertTrue(config.isShouldInstrument());
        assertEquals(15, config.getConfigRefreshInterval());
        assertEquals("C:\\\\TraceFileDumps", config.getTraceFileLocation());
        assertNotNull(config.getAgentRules());
        assertTrue(config.getAgentRules().contains("com.example.Foo::bar@INGRESS::ARGS"));
        assertTrue(config.isPrintJVMHeapUsage());
        assertTrue(config.isPrintJVMCpuUsage());
        assertFalse(config.isPrintJVMThreadUsage());
        assertTrue(config.isPrintJVMGCStats());
        assertFalse(config.isPrintJVMClassLoaderStats());
        assertTrue(config.isPrintJVMSystemProperties());
        assertFalse(config.isPrintEnvironmentVariables());
        assertTrue(config.isPrintClassLoaderTrace());
        assertTrue(config.isExposeMetrics());
        assertEquals(9090, config.getMetricsPort());
        assertTrue(config.isSendAlertEmails());
        assertEquals(3, config.getMaxHeapDumps());
        assertEquals(1, config.getEmailRecipientList().size());
    }

    @Test
    void parseSupportsNestedModeAwareConfig() throws Exception {
        Path configFile = writeTempYaml("" +
                "mode: observer\n" +
                "instrumentation:\n" +
                "  enabled: false\n" +
                "observer:\n" +
                "  enabled: true\n" +
                "  printClassLoaderTrace: true\n" +
                "  printJVMSystemProperties: true\n" +
                "  printEnvironmentVariables: false\n" +
                "  metrics:\n" +
                "    exposeHttp: true\n" +
                "    port: 9191\n" +
                "    heapUsage: true\n" +
                "    cpuUsage: false\n" +
                "    threadUsage: true\n" +
                "    gcStats: false\n" +
                "    classLoaderStats: true\n" +
                "alerts:\n" +
                "  enabled: true\n" +
                "  maxHeapDumps: 5\n" +
                "  emailRecipientList:\n" +
                "    - observer@example.com\n" +
                "logging:\n" +
                "  level: DEBUG\n");

        Config config = ConfigParser.parse(configFile.toString());

        assertEquals(AgentMode.OBSERVER, config.getResolvedMode());
        assertFalse(config.isInstrumentationActive());
        assertTrue(config.isObserverActive());
        assertTrue(config.isAlertsActive());
        assertTrue(config.isPrintClassLoaderTrace());
        assertTrue(config.isPrintJVMSystemProperties());
        assertFalse(config.isPrintEnvironmentVariables());
        assertTrue(config.isPrintJVMHeapUsage());
        assertFalse(config.isPrintJVMCpuUsage());
        assertTrue(config.isPrintJVMThreadUsage());
        assertFalse(config.isPrintJVMGCStats());
        assertTrue(config.isPrintJVMClassLoaderStats());
        assertTrue(config.isExposeMetrics());
        assertEquals(9191, config.getMetricsPort());
        assertEquals(5, config.getMaxHeapDumps());
        assertEquals("DEBUG", config.getLogging().getLevel());
    }

    @Test
    void parsePrefersNestedKeysOverLegacyFlatKeys() throws Exception {
        Path configFile = writeTempYaml("" +
                "mode: hybrid\n" +
                "shouldInstrument: true\n" +
                "configRefreshInterval: 30\n" +
                "traceFileLocation: C:\\\\LegacyTrace\n" +
                "agentRules:\n" +
                "  - com.example.Legacy::one@INGRESS::ARGS\n" +
                "printJVMHeapUsage: true\n" +
                "exposeMetrics: true\n" +
                "metricsPort: 9090\n" +
                "sendAlertEmails: false\n" +
                "maxHeapDumps: 2\n" +
                "instrumentation:\n" +
                "  enabled: false\n" +
                "  configRefreshInterval: 7\n" +
                "  traceFileLocation: C:\\\\NestedTrace\n" +
                "  agentRules:\n" +
                "    - com.example.Nested::two@INGRESS::RET\n" +
                "observer:\n" +
                "  metrics:\n" +
                "    exposeHttp: false\n" +
                "    port: 9292\n" +
                "    heapUsage: false\n" +
                "alerts:\n" +
                "  enabled: true\n" +
                "  maxHeapDumps: 6\n");

        Config config = ConfigParser.parse(configFile.toString());

        assertFalse(config.isShouldInstrument());
        assertEquals(7, config.getConfigRefreshInterval());
        assertEquals("C:\\\\NestedTrace", config.getTraceFileLocation());
        assertEquals(new HashSet<>(Arrays.asList("com.example.Nested::two@INGRESS::RET")), config.getAgentRules());
        assertFalse(config.isPrintJVMHeapUsage());
        assertFalse(config.isExposeMetrics());
        assertEquals(9292, config.getMetricsPort());
        assertTrue(config.isSendAlertEmails());
        assertEquals(6, config.getMaxHeapDumps());
    }

    @Test
    void findLegacyKeysDetectsLegacyTopLevelFields() throws Exception {
        Path configFile = writeTempYaml("" +
                "mode: hybrid\n" +
                "shouldInstrument: true\n" +
                "printJVMHeapUsage: true\n" +
                "metricsPort: 9090\n");

        com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());

        Set<String> legacyKeys = ConfigParser.findLegacyKeys(mapper.readTree(configFile.toFile()));

        assertEquals(
                new HashSet<>(Arrays.asList("shouldInstrument", "printJVMHeapUsage", "metricsPort")),
                legacyKeys
        );
    }

    private Path writeTempYaml(String yaml) throws Exception {
        Path path = Files.createTempFile("mja-config", ".yaml");
        Files.write(path, yaml.getBytes(StandardCharsets.UTF_8));
        return path;
    }
}
