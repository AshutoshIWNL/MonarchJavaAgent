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
        String traceLocation = config.getTraceFileLocation();
        if (traceLocation == null || traceLocation.isEmpty() || !new File(traceLocation).isDirectory()) {
            AgentLogger.error("trace file directory doesn't exist or is not a directory");
            return false;
        }

        if (config.getAgentRules() == null || config.getAgentRules().isEmpty()) {
            AgentLogger.error("Rules are missing or empty");
            return false;
        }
        return config.getMaxHeapDumps() >= 0;
    }
}
