package com.asm.mja.config;

import com.asm.mja.logging.AgentLogger;

import java.io.File;

/**
 * The ConfigValidator class validates the configuration object.
 * It checks if the configuration is valid based on certain criteria.
 * If any of the criteria are not met, the validation fails.
 * @author ashut
 * @since 11-04-2024
 */

public class ConfigValidator {

    /**
     * Validates the given configuration object.
     *
     * @param config The configuration object to validate.
     * @return true if the configuration is valid, false otherwise.
     */
    public static boolean isValid(Config config) {
        AgentLogger.debug("Validating the config object");

        if (config == null) {
            AgentLogger.error("Config object is null");
            return false;
        }

        if (!isInstrumentationValid(config)) {
            return false;
        }

        if (!isObserverValid(config)) {
            return false;
        }

        if (!isAlertsValid(config)) {
            return false;
        }

        return true;
    }

    private static boolean isInstrumentationValid(Config config) {
        if (!config.isInstrumentationActive()) {
            return true;
        }

        String traceLocation = config.getTraceFileLocation();
        if (traceLocation == null || traceLocation.isEmpty() || !new File(traceLocation).isDirectory()) {
            AgentLogger.error("trace file directory doesn't exist or is not a directory");
            return false;
        }

        if (config.getAgentRules() == null || config.getAgentRules().isEmpty()) {
            AgentLogger.error("Rules are missing or empty");
            return false;
        }

        return true;
    }

    private static boolean isObserverValid(Config config) {
        if (!config.isObserverActive()) {
            return true;
        }

        if (config.isExposeMetrics() && config.getMetricsPort() <= 0) {
            AgentLogger.error("Metrics port must be greater than zero when metrics exposure is enabled");
            return false;
        }

        return true;
    }

    private static boolean isAlertsValid(Config config) {
        if (!config.isAlertsActive()) {
            return true;
        }

        if (config.getMaxHeapDumps() < 0) {
            AgentLogger.error("Max heap dumps cannot be negative");
            return false;
        }

        return true;
    }
}
