package com.asm.mja.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for mode-aware config validation behavior.
 * @author ashut
 * @since 22-03-2026
 */
public class ConfigValidatorTest {

    @Test
    void observerModeDoesNotRequireTraceDirectoryOrRules() {
        Config config = new Config();
        config.setMode(AgentMode.OBSERVER);

        ObserverConfig observer = new ObserverConfig();
        observer.setEnabled(true);

        ObserverMetricsConfig metrics = new ObserverMetricsConfig();
        metrics.setExposeHttp(true);
        metrics.setPort(9090);
        metrics.setHeapUsage(true);
        observer.setMetrics(metrics);

        config.setObserver(observer);

        assertTrue(ConfigValidator.isValid(config));
    }

    @Test
    void instrumenterModeRequiresTraceDirectoryAndRules() throws Exception {
        Config config = new Config();
        config.setMode(AgentMode.INSTRUMENTER);

        InstrumentationConfig instrumentation = new InstrumentationConfig();
        instrumentation.setEnabled(true);
        config.setInstrumentation(instrumentation);

        assertFalse(ConfigValidator.isValid(config));

        Path traceDir = Files.createTempDirectory("mja-trace");
        instrumentation.setTraceFileLocation(traceDir.toString());
        instrumentation.setAgentRules(new HashSet<>(Arrays.asList("com.example.Foo::bar@INGRESS::ARGS")));

        assertTrue(ConfigValidator.isValid(config));
    }

    @Test
    void observerModeRejectsMetricsExposureWithoutValidPort() {
        Config config = new Config();
        config.setMode(AgentMode.OBSERVER);

        ObserverConfig observer = new ObserverConfig();
        observer.setEnabled(true);

        ObserverMetricsConfig metrics = new ObserverMetricsConfig();
        metrics.setExposeHttp(true);
        metrics.setPort(0);
        observer.setMetrics(metrics);

        config.setObserver(observer);

        assertFalse(ConfigValidator.isValid(config));
    }

    @Test
    void hybridModeRequiresInstrumentationAndObserverSectionsToBeValid() throws Exception {
        Config config = new Config();
        config.setMode(AgentMode.HYBRID);

        InstrumentationConfig instrumentation = new InstrumentationConfig();
        instrumentation.setEnabled(true);
        Path traceDir = Files.createTempDirectory("mja-trace");
        instrumentation.setTraceFileLocation(traceDir.toString());
        instrumentation.setAgentRules(new HashSet<>(Arrays.asList("com.example.Foo::bar@INGRESS::ARGS")));
        config.setInstrumentation(instrumentation);

        ObserverConfig observer = new ObserverConfig();
        observer.setEnabled(true);
        ObserverMetricsConfig metrics = new ObserverMetricsConfig();
        metrics.setExposeHttp(true);
        metrics.setPort(9091);
        observer.setMetrics(metrics);
        config.setObserver(observer);

        AlertsConfig alerts = new AlertsConfig();
        alerts.setEnabled(true);
        alerts.setMaxHeapDumps(2);
        config.setAlerts(alerts);

        assertTrue(ConfigValidator.isValid(config));

        alerts.setMaxHeapDumps(-1);
        assertFalse(ConfigValidator.isValid(config));
    }
}
