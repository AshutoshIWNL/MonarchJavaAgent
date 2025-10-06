package com.asm.mja.config;

import java.util.HashSet;
import java.util.List;

/**
 * @author ashut
 * @since 11-04-2024
 */

public class Config {
    private String traceFileLocation;
    private HashSet<String> agentRules;
    private boolean printClassLoaderTrace;
    private boolean printJVMHeapUsage;
    private boolean printJVMCpuUsage;
    private boolean printJVMThreadUsage;
    private boolean printJVMGCStats;
    private boolean printJVMClassLoaderStats;
    private boolean printJVMSystemProperties;
    private boolean printEnvironmentVariables;
    private boolean sendAlertEmails;
    private int maxHeapDumps;
    private boolean shouldInstrument;
    private int configRefreshInterval;
    private List<String> emailRecipientList;

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

    public boolean isPrintClassLoaderTrace() {
        return printClassLoaderTrace;
    }

    public void setPrintClassLoaderTrace(boolean printClassLoaderTrace) {
        this.printClassLoaderTrace = printClassLoaderTrace;
    }

    public boolean isPrintJVMHeapUsage() {
        return printJVMHeapUsage;
    }

    public void setPrintJVMHeapUsage(boolean printJVMHeapUsage) {
        this.printJVMHeapUsage = printJVMHeapUsage;
    }

    public boolean isPrintJVMCpuUsage() {
        return printJVMCpuUsage;
    }

    public void setPrintJVMCpuUsage(boolean printJVMCpuUsage) {
        this.printJVMCpuUsage = printJVMCpuUsage;
    }

    public boolean isPrintJVMThreadUsage() {
        return printJVMThreadUsage;
    }

    public void setPrintJVMThreadUsage(boolean printJVMThreadUsage) {
        this.printJVMThreadUsage = printJVMThreadUsage;
    }

    public boolean isPrintJVMGCStats() {
        return printJVMGCStats;
    }

    public void setPrintJVMGCStats(boolean printJVMGCStats) {
        this.printJVMGCStats = printJVMGCStats;
    }

    public boolean isPrintJVMClassLoaderStats() {
        return printJVMClassLoaderStats;
    }

    public void setPrintJVMClassLoaderStats(boolean printJVMClassLoaderStats) {
        this.printJVMClassLoaderStats = printJVMClassLoaderStats;
    }

    public boolean isPrintJVMSystemProperties() {
        return printJVMSystemProperties;
    }

    public void setPrintJVMSystemProperties(boolean printJVMSystemProperties) {
        this.printJVMSystemProperties = printJVMSystemProperties;
    }

    public boolean isPrintEnvironmentVariables() {
        return printEnvironmentVariables;
    }

    public void setPrintEnvironmentVariables(boolean printEnvironmentVariables) {
        this.printEnvironmentVariables = printEnvironmentVariables;
    }

    public boolean isSendAlertEmails() {
        return sendAlertEmails;
    }

    public void setSendAlertEmails(boolean sendAlertEmails) {
        this.sendAlertEmails = sendAlertEmails;
    }

    public int getMaxHeapDumps() {
        return maxHeapDumps;
    }

    public void setMaxHeapDumps(int maxHeapDumps) {
        this.maxHeapDumps = maxHeapDumps;
    }

    public boolean isShouldInstrument() {
        return shouldInstrument;
    }

    public void setShouldInstrument(boolean shouldInstrument) {
        this.shouldInstrument = shouldInstrument;
    }

    public int getConfigRefreshInterval() {
        return configRefreshInterval;
    }

    public void setConfigRefreshInterval(int configRefreshInterval) {
        this.configRefreshInterval = configRefreshInterval;
    }

    public List<String> getEmailRecipientList() {
        return emailRecipientList;
    }

    public void setEmailRecipientList(List<String> emailRecipientList) {
        this.emailRecipientList = emailRecipientList;
    }

    @Override
    public String toString() {
        return "Config{" +
                "traceFileLocation='" + traceFileLocation + '\'' +
                ", agentRules=" + agentRules +
                ", printClassLoaderTrace=" + printClassLoaderTrace +
                ", printJVMHeapUsage=" + printJVMHeapUsage +
                ", printJVMCpuUsage=" + printJVMCpuUsage +
                ", printJVMThreadUsage=" + printJVMThreadUsage +
                ", printJVMGCStats=" + printJVMGCStats +
                ", printJVMClassLoaderStats=" + printJVMClassLoaderStats +
                ", printJVMSystemProperties=" + printJVMSystemProperties +
                ", printEnvironmentVariables=" + printEnvironmentVariables +
                ", sendAlertEmails=" + sendAlertEmails +
                ", maxHeapDumps=" + maxHeapDumps +
                ", shouldInstrument=" + shouldInstrument +
                ", configRefreshInterval=" + configRefreshInterval +
                ", emailRecipientList=" + emailRecipientList +
                '}';
    }

}
