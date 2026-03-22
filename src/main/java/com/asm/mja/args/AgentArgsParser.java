package com.asm.mja.args;

import com.asm.mja.logging.LogLevel;

import java.io.File;

/**
 * Parses agent launch arguments into typed options.
 * @author ashut
 * @since 22-03-2026
 */
public class AgentArgsParser {

    public static AgentLaunchOptions parse(String agentArgs) {
        return new AgentLaunchOptions(
                parseConfigFile(agentArgs),
                parseAgentLogFileDir(agentArgs),
                parseAgentLogLevel(agentArgs),
                parseSmtpProperties(agentArgs),
                parseAgentJarPath(agentArgs)
        );
    }

    static String parseAgentLogFileDir(String agentArgs) {
        String agentLogFileDir = null;
        if (agentArgs != null) {
            String[] args = agentArgs.split(",");
            for (String arg : args) {
                if (arg.contains("agentLogFileDir")) {
                    String[] prop = arg.split("=");
                    if (prop.length < 2) {
                        throw new IllegalArgumentException("Invalid arguments passed - " + arg);
                    } else {
                        agentLogFileDir = prop[1];
                        File agentLogFileDirObj = new File(agentLogFileDir);
                        if (!agentLogFileDirObj.isDirectory() || !agentLogFileDirObj.exists()) {
                            throw new IllegalArgumentException("Agent logging doesn't exist or isn't a directory - " + agentLogFileDir);
                        }
                    }
                }
            }
        }
        return agentLogFileDir;
    }

    static String parseAgentLogLevel(String agentArgs) {
        String agentLogLevel = null;
        if (agentArgs != null) {
            String[] args = agentArgs.split(",");
            for (String arg : args) {
                if (arg.contains("agentLogLevel")) {
                    String[] prop = arg.split("=");
                    if (prop.length < 2) {
                        throw new IllegalArgumentException("Invalid arguments passed - " + arg);
                    } else {
                        agentLogLevel = prop[1];
                        if (!isValidLogLevel(agentLogLevel)) {
                            throw new IllegalArgumentException("Invalid log level passed - " + agentLogLevel);
                        }
                    }
                }
            }
        }
        return agentLogLevel;
    }

    static String parseAgentJarPath(String agentArgs) {
        String agentJarPath = null;
        if (agentArgs != null) {
            String[] args = agentArgs.split(",");
            for (String arg : args) {
                if (arg.contains("agentJarPath")) {
                    String[] prop = arg.split("=");
                    if (prop.length < 2) {
                        throw new IllegalArgumentException("Invalid arguments passed - " + arg);
                    } else {
                        agentJarPath = prop[1];
                        File agentJarPathObj = new File(agentJarPath);
                        if (agentJarPathObj.isDirectory() || !agentJarPathObj.exists()) {
                            throw new IllegalArgumentException("Agent jar doesn't exist or is a directory - " + agentJarPath);
                        }
                    }
                }
            }
        }
        return agentJarPath;
    }

    static String parseSmtpProperties(String agentArgs) {
        String smtpPropertiesFile = null;
        if (agentArgs != null) {
            String[] args = agentArgs.split(",");
            for (String arg : args) {
                if (arg.contains("smtpProperties")) {
                    String[] prop = arg.split("=");
                    if (prop.length < 2) {
                        throw new IllegalArgumentException("Invalid arguments passed - " + arg);
                    } else {
                        smtpPropertiesFile = prop[1];
                    }
                }
            }
        }
        return smtpPropertiesFile;
    }

    static String parseConfigFile(String agentArgs) {
        String configFile = null;
        if (agentArgs != null) {
            String[] args = agentArgs.split(",");
            for (String arg : args) {
                if (arg.contains("configFile")) {
                    String[] prop = arg.split("=");
                    if (prop.length < 2) {
                        throw new IllegalArgumentException("Invalid arguments passed - " + arg);
                    } else {
                        configFile = prop[1];
                        File configFileObj = new File(configFile);
                        if (!configFileObj.exists()) {
                            throw new IllegalArgumentException("Config file doesn't exist in the specified directory - " + configFile);
                        }
                    }
                }
            }
        }
        return configFile;
    }

    public static boolean isValidLogLevel(String input) {
        for (LogLevel level : LogLevel.values()) {
            if (level.name().equals(input)) {
                return true;
            }
        }
        return false;
    }
}
