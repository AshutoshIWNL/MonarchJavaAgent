package com.asm.mja.args;

import com.asm.mja.logging.LogLevel;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Parses agent launch arguments into typed options.
 *
 * Expected format:
 * key=value,key=value
 *
 * Example:
 * configFile=/tmp/config.yml,
 * agentLogLevel=DEBUG,
 * agentJarPath=/tmp/agent.jar
 *
 * Java 8 compatible implementation.
 *
 * @author anandh
 * @since 22-05-2026
 */
public final class AgentArgsParser {

    private static final Set<String> SUPPORTED_KEYS =
            new HashSet<>(Arrays.asList(
                    "configFile",
                    "agentLogFileDir",
                    "agentLogLevel",
                    "smtpProperties",
                    "agentJarPath"
            ));

    private AgentArgsParser() {
    }

    /**
     * Parses and validates agent launch options.
     */
    public static AgentLaunchOptions parse(String agentArgs) {

        Map<String, String> parsedArgs = parseArgs(agentArgs);

        return new AgentLaunchOptions(
                validateConfigFile(parsedArgs.get("configFile")),
                validateAgentLogFileDir(parsedArgs.get("agentLogFileDir")),
                validateAgentLogLevel(parsedArgs.get("agentLogLevel")),
                validateSmtpProperties(parsedArgs.get("smtpProperties")),
                validateAgentJarPath(parsedArgs.get("agentJarPath"))
        );
    }

    /**
     * Parses raw agent args into strict key=value pairs.
     */
    private static Map<String, String> parseArgs(String agentArgs) {

        Map<String, String> parsed = new HashMap<>();

        if (agentArgs == null || agentArgs.trim().isEmpty()) {
            return parsed;
        }

        String[] tokens = agentArgs.split(",");

        for (String token : tokens) {

            if (token == null || token.trim().isEmpty()) {
                continue;
            }

            token = token.trim();

            String[] parts = token.split("=", 2);

            // malformed token
            if (parts.length != 2) {
                warn("Ignoring malformed token: " + token);
                continue;
            }

            String key = parts[0].trim();
            String value = parts[1].trim();

            // empty key
            if (key.isEmpty()) {
                warn("Ignoring token with empty key: " + token);
                continue;
            }

            // unknown key
            if (!SUPPORTED_KEYS.contains(key)) {
                warn("Ignoring unknown key: " + key);
                continue;
            }

            // duplicate key
            if (parsed.containsKey(key)) {
                throw new IllegalArgumentException(
                        "Duplicate argument detected: " + key
                );
            }

            parsed.put(key, value);
        }

        return parsed;
    }

    /**
     * Validates agent log directory.
     */
    static String validateAgentLogFileDir(String path) {

        if (path == null || path.trim().isEmpty()) {
            return null;
        }

        File dir = new File(path);

        if (!dir.exists() || !dir.isDirectory()) {
            throw new IllegalArgumentException(
                    "Agent logging directory doesn't exist or isn't a directory: " + path
            );
        }

        return path;
    }

    /**
     * Validates agent log level.
     */
    static String validateAgentLogLevel(String level) {

        if (level == null || level.trim().isEmpty()) {
            return null;
        }

        if (!isValidLogLevel(level)) {
            throw new IllegalArgumentException(
                    "Invalid log level passed: " + level
            );
        }

        return level;
    }

    /**
     * Validates agent jar path.
     */
    static String validateAgentJarPath(String path) {

        if (path == null || path.trim().isEmpty()) {
            return null;
        }

        File jarFile = new File(path);

        if (!jarFile.exists() || !jarFile.isFile()) {
            throw new IllegalArgumentException(
                    "Agent jar doesn't exist or is invalid: " + path
            );
        }

        return path;
    }

    /**
     * Validates SMTP properties path.
     */
    static String validateSmtpProperties(String path) {

        if (path == null || path.trim().isEmpty()) {
            return null;
        }

        return path;
    }

    /**
     * Validates config file path.
     */
    static String validateConfigFile(String path) {

        if (path == null || path.trim().isEmpty()) {
            return null;
        }

        File configFile = new File(path);

        if (!configFile.exists() || !configFile.isFile()) {
            throw new IllegalArgumentException(
                    "Config file doesn't exist or is invalid: " + path
            );
        }

        return path;
    }

    /**
     * Validates log level enum.
     */
    public static boolean isValidLogLevel(String input) {

        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        for (LogLevel level : LogLevel.values()) {

            if (level.name().equalsIgnoreCase(input.trim())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Temporary warning logger.
     * Replace with proper logger later.
     */
    private static void warn(String message) {
        System.err.println("[AgentArgsParser] WARN: " + message);
    }
}