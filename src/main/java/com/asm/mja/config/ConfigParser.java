package com.asm.mja.config;

import com.asm.mja.AgentConfigurator;
import com.asm.mja.logging.AgentLogger;
import com.asm.mja.logging.TraceFileLogger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The ConfigParser class parses a configuration file into a Config object.
 * It uses Jackson ObjectMapper for parsing YAML files into Java objects.
 *
 * This class provides a static method parse() to parse the configuration file.
 *
 * @author ashut
 * @since 11-04-2024
 */

public class ConfigParser {
    private static final Set<String> LEGACY_KEYS = new LinkedHashSet<>(Arrays.asList(
            "traceFileLocation",
            "agentRules",
            "printClassLoaderTrace",
            "printJVMHeapUsage",
            "printJVMCpuUsage",
            "printJVMThreadUsage",
            "printJVMGCStats",
            "printJVMClassLoaderStats",
            "printJVMSystemProperties",
            "printEnvironmentVariables",
            "exposeMetrics",
            "metricsPort",
            "sendAlertEmails",
            "maxHeapDumps",
            "shouldInstrument",
            "configRefreshInterval",
            "emailRecipientList"
    ));

    /**
     * Parses the given configuration file into a Config object.
     *
     * @param configFile The path to the configuration file.
     * @return The Config object parsed from the configuration file.
     * @throws RuntimeException if parsing fails due to IO error or invalid YAML format.
     */
    public static Config parse(String configFile) {
        AgentLogger.debug("Parsing config file - " + configFile);
        ObjectMapper om = new ObjectMapper(new YAMLFactory());
        try {
            File configFileObj = new File(configFile);
            JsonNode root = om.readTree(configFileObj);
            warnIfLegacyKeysPresent(findLegacyKeys(root));
            Config config = om.treeToValue(root, Config.class);
            AgentLogger.debug("Config file parsed and config object built");
            AgentLogger.debug(config.toString());
            return config;
        } catch (IOException e) {
            AgentLogger.error(e.getMessage(), e);
            throw new RuntimeException("Config file parsing failed");
        }
    }

    public static Config parse(String configFile, TraceFileLogger logger) throws IOException {
        logger.trace("Parsing config file - " + configFile);
        ObjectMapper om = new ObjectMapper(new YAMLFactory());
        File configFileObj = new File(configFile);
        JsonNode root = om.readTree(configFileObj);
        warnIfLegacyKeysPresent(findLegacyKeys(root), logger);
        Config config = om.treeToValue(root, Config.class);
        logger.trace("Config file parsed and config object built");
        logger.trace(config.toString());
        return config;
    }

    static Set<String> findLegacyKeys(JsonNode root) {
        LinkedHashSet<String> legacyKeys = new LinkedHashSet<>();
        if (root == null || !root.isObject()) {
            return legacyKeys;
        }

        for (String legacyKey : LEGACY_KEYS) {
            if (root.has(legacyKey)) {
                legacyKeys.add(legacyKey);
            }
        }
        return legacyKeys;
    }

    private static void warnIfLegacyKeysPresent(Set<String> legacyKeys) {
        if (legacyKeys.isEmpty()) {
            return;
        }
        String message = buildLegacyWarningMessage(legacyKeys);
        if (AgentLogger.isInitialized()) {
            AgentLogger.warning(message);
        } else {
            System.err.println("[WARN] " + message);
        }
    }

    private static void warnIfLegacyKeysPresent(Set<String> legacyKeys, TraceFileLogger logger) {
        if (legacyKeys.isEmpty()) {
            return;
        }
        logger.warn(buildLegacyWarningMessage(legacyKeys));
    }

    private static String buildLegacyWarningMessage(Set<String> legacyKeys) {
        return "Legacy flat config keys are deprecated and will be removed in a future release: " + legacyKeys;
    }
}
