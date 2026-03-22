package com.asm.mja.bootstrap;

import com.asm.mja.logging.AgentLogger;
import com.asm.mja.logging.LogLevel;

import java.io.File;

/**
 * Bootstraps the agent logger from launch arguments.
 * @author ashut
 * @since 22-03-2026
 */
public class LoggingBootstrap {
    private static final String DEFAULT_LOG_LEVEL = "INFO";
    private static final String DEFAULT_AGENT_LOG_DIR = System.getProperty("java.io.tmpdir");

    private LoggingBootstrap() {
    }

    public static void setupLogger(String agentArgs) {
        String agentLogFileDir;
        try {
            agentLogFileDir = fetchAgentLogFileDir(agentArgs);
            if (agentLogFileDir == null) {
                agentLogFileDir = DEFAULT_AGENT_LOG_DIR;
            }
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: " + e.getMessage());
            agentLogFileDir = DEFAULT_AGENT_LOG_DIR;
        }

        String agentLogFile = agentLogFileDir + File.separator + "monarchAgent.log";

        String agentLogLevel;
        try {
            agentLogLevel = fetchAgentLogLevel(agentArgs);
            if (agentLogLevel == null) {
                agentLogLevel = DEFAULT_LOG_LEVEL;
            }
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: " + e.getMessage());
            agentLogLevel = DEFAULT_LOG_LEVEL;
        }
        AgentLogger.init(agentLogFile, LogLevel.valueOf(agentLogLevel));
    }

    private static String fetchAgentLogFileDir(String agentArgs) {
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

    private static String fetchAgentLogLevel(String agentArgs) {
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

    private static boolean isValidLogLevel(String input) {
        for (LogLevel level : LogLevel.values()) {
            if (level.name().equals(input)) {
                return true;
            }
        }
        return false;
    }
}
