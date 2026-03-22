package com.asm.mja.args;

/**
 * Immutable holder for parsed agent launch arguments.
 * @author ashut
 * @since 22-03-2026
 */
public class AgentLaunchOptions {

    private final String configFile;
    private final String agentLogFileDir;
    private final String agentLogLevel;
    private final String smtpProperties;
    private final String agentJarPath;

    public AgentLaunchOptions(String configFile,
                              String agentLogFileDir,
                              String agentLogLevel,
                              String smtpProperties,
                              String agentJarPath) {
        this.configFile = configFile;
        this.agentLogFileDir = agentLogFileDir;
        this.agentLogLevel = agentLogLevel;
        this.smtpProperties = smtpProperties;
        this.agentJarPath = agentJarPath;
    }

    public String getConfigFile() {
        return configFile;
    }

    public String getAgentLogFileDir() {
        return agentLogFileDir;
    }

    public String getAgentLogLevel() {
        return agentLogLevel;
    }

    public String getSmtpProperties() {
        return smtpProperties;
    }

    public String getAgentJarPath() {
        return agentJarPath;
    }
}
