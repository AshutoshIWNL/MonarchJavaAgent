package com.asm.mja.config;

import java.util.HashSet;

/**
 * Nested instrumentation configuration section.
 * @author ashut
 * @since 22-03-2026
 */
public class InstrumentationConfig {
    private Boolean enabled;
    private Integer configRefreshInterval;
    private String traceFileLocation;
    private HashSet<String> agentRules;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getConfigRefreshInterval() {
        return configRefreshInterval;
    }

    public void setConfigRefreshInterval(Integer configRefreshInterval) {
        this.configRefreshInterval = configRefreshInterval;
    }

    public String getTraceFileLocation() {
        return traceFileLocation;
    }

    public void setTraceFileLocation(String traceFileLocation) {
        this.traceFileLocation = traceFileLocation;
    }

    public HashSet<String> getAgentRules() {
        return agentRules;
    }

    public void setAgentRules(HashSet<String> agentRules) {
        this.agentRules = agentRules;
    }
}
