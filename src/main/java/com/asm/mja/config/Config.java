package com.asm.mja.config;

import java.util.HashSet;
import java.util.List;

/**
 * @author ashut
 * @since 11-04-2024
 */

public class Config {
    private AgentMode mode;
    private InstrumentationConfig instrumentation;
    private ObserverConfig observer;
    private AlertsConfig alerts;
    private LoggingConfig logging;

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
    private boolean exposeMetrics;
    private int metricsPort;
    private boolean sendAlertEmails;
    private int maxHeapDumps;
    private boolean shouldInstrument;
    private int configRefreshInterval;
    private List<String> emailRecipientList;

    public String getTraceFileLocation() {
        if (instrumentation != null && instrumentation.getTraceFileLocation() != null
                && !instrumentation.getTraceFileLocation().isEmpty()) {
            return instrumentation.getTraceFileLocation();
        }
        return traceFileLocation;
    }

    public void setTraceFileLocation(String traceFileLocation) {
        this.traceFileLocation = traceFileLocation;
    }

    public HashSet<String> getAgentRules() {
        if (instrumentation != null && instrumentation.getAgentRules() != null) {
            return instrumentation.getAgentRules();
        }
        return agentRules;
    }

    public void setAgentRules(HashSet<String> agentRules) {
        this.agentRules = agentRules;
    }

    public boolean isPrintClassLoaderTrace() {
        if (observer != null && observer.getPrintClassLoaderTrace() != null) {
            return observer.getPrintClassLoaderTrace();
        }
        return printClassLoaderTrace;
    }

    public void setPrintClassLoaderTrace(boolean printClassLoaderTrace) {
        this.printClassLoaderTrace = printClassLoaderTrace;
    }

    public boolean isPrintJVMHeapUsage() {
        if (observer != null && observer.getMetrics() != null && observer.getMetrics().getHeapUsage() != null) {
            return observer.getMetrics().getHeapUsage();
        }
        return printJVMHeapUsage;
    }

    public void setPrintJVMHeapUsage(boolean printJVMHeapUsage) {
        this.printJVMHeapUsage = printJVMHeapUsage;
    }

    public boolean isPrintJVMCpuUsage() {
        if (observer != null && observer.getMetrics() != null && observer.getMetrics().getCpuUsage() != null) {
            return observer.getMetrics().getCpuUsage();
        }
        return printJVMCpuUsage;
    }

    public void setPrintJVMCpuUsage(boolean printJVMCpuUsage) {
        this.printJVMCpuUsage = printJVMCpuUsage;
    }

    public boolean isPrintJVMThreadUsage() {
        if (observer != null && observer.getMetrics() != null && observer.getMetrics().getThreadUsage() != null) {
            return observer.getMetrics().getThreadUsage();
        }
        return printJVMThreadUsage;
    }

    public void setPrintJVMThreadUsage(boolean printJVMThreadUsage) {
        this.printJVMThreadUsage = printJVMThreadUsage;
    }

    public boolean isPrintJVMGCStats() {
        if (observer != null && observer.getMetrics() != null && observer.getMetrics().getGcStats() != null) {
            return observer.getMetrics().getGcStats();
        }
        return printJVMGCStats;
    }

    public void setPrintJVMGCStats(boolean printJVMGCStats) {
        this.printJVMGCStats = printJVMGCStats;
    }

    public boolean isPrintJVMClassLoaderStats() {
        if (observer != null && observer.getMetrics() != null && observer.getMetrics().getClassLoaderStats() != null) {
            return observer.getMetrics().getClassLoaderStats();
        }
        return printJVMClassLoaderStats;
    }

    public void setPrintJVMClassLoaderStats(boolean printJVMClassLoaderStats) {
        this.printJVMClassLoaderStats = printJVMClassLoaderStats;
    }

    public boolean isPrintJVMSystemProperties() {
        if (observer != null && observer.getPrintJVMSystemProperties() != null) {
            return observer.getPrintJVMSystemProperties();
        }
        return printJVMSystemProperties;
    }

    public void setPrintJVMSystemProperties(boolean printJVMSystemProperties) {
        this.printJVMSystemProperties = printJVMSystemProperties;
    }

    public boolean isPrintEnvironmentVariables() {
        if (observer != null && observer.getPrintEnvironmentVariables() != null) {
            return observer.getPrintEnvironmentVariables();
        }
        return printEnvironmentVariables;
    }

    public void setPrintEnvironmentVariables(boolean printEnvironmentVariables) {
        this.printEnvironmentVariables = printEnvironmentVariables;
    }

    public boolean isSendAlertEmails() {
        if (alerts != null && alerts.getEnabled() != null) {
            return alerts.getEnabled();
        }
        return sendAlertEmails;
    }

    public void setSendAlertEmails(boolean sendAlertEmails) {
        this.sendAlertEmails = sendAlertEmails;
    }

    public int getMaxHeapDumps() {
        if (alerts != null && alerts.getMaxHeapDumps() != null) {
            return alerts.getMaxHeapDumps();
        }
        return maxHeapDumps;
    }

    public void setMaxHeapDumps(int maxHeapDumps) {
        this.maxHeapDumps = maxHeapDumps;
    }

    public boolean isShouldInstrument() {
        if (instrumentation != null && instrumentation.getEnabled() != null) {
            return instrumentation.getEnabled();
        }
        return shouldInstrument;
    }

    public void setShouldInstrument(boolean shouldInstrument) {
        this.shouldInstrument = shouldInstrument;
    }

    public int getConfigRefreshInterval() {
        if (instrumentation != null && instrumentation.getConfigRefreshInterval() != null) {
            return instrumentation.getConfigRefreshInterval();
        }
        return configRefreshInterval;
    }

    public void setConfigRefreshInterval(int configRefreshInterval) {
        this.configRefreshInterval = configRefreshInterval;
    }

    public List<String> getEmailRecipientList() {
        if (alerts != null && alerts.getEmailRecipientList() != null) {
            return alerts.getEmailRecipientList();
        }
        return emailRecipientList;
    }

    public void setEmailRecipientList(List<String> emailRecipientList) {
        this.emailRecipientList = emailRecipientList;
    }

    public boolean isExposeMetrics() {
        if (observer != null && observer.getMetrics() != null && observer.getMetrics().getExposeHttp() != null) {
            return observer.getMetrics().getExposeHttp();
        }
        return exposeMetrics;
    }

    public void setExposeMetrics(boolean exposeMetrics) {
        this.exposeMetrics = exposeMetrics;
    }

    public int getMetricsPort() {
        if (observer != null && observer.getMetrics() != null && observer.getMetrics().getPort() != null) {
            return observer.getMetrics().getPort();
        }
        return metricsPort;
    }

    public void setMetricsPort(int metricsPort) {
        this.metricsPort = metricsPort;
    }

    public AgentMode getMode() {
        return mode;
    }

    public void setMode(AgentMode mode) {
        this.mode = mode;
    }

    public InstrumentationConfig getInstrumentation() {
        return instrumentation;
    }

    public void setInstrumentation(InstrumentationConfig instrumentation) {
        this.instrumentation = instrumentation;
    }

    public ObserverConfig getObserver() {
        return observer;
    }

    public void setObserver(ObserverConfig observer) {
        this.observer = observer;
    }

    public AlertsConfig getAlerts() {
        return alerts;
    }

    public void setAlerts(AlertsConfig alerts) {
        this.alerts = alerts;
    }

    public LoggingConfig getLogging() {
        return logging;
    }

    public void setLogging(LoggingConfig logging) {
        this.logging = logging;
    }

    public AgentMode getResolvedMode() {
        return mode != null ? mode : AgentMode.HYBRID;
    }

    public boolean isInstrumentationActive() {
        AgentMode resolvedMode = getResolvedMode();
        if (resolvedMode == AgentMode.OBSERVER) {
            return false;
        }
        return isShouldInstrument();
    }

    public boolean isObserverActive() {
        AgentMode resolvedMode = getResolvedMode();
        if (resolvedMode == AgentMode.INSTRUMENTER) {
            return false;
        }
        if (observer != null && observer.getEnabled() != null) {
            return observer.getEnabled();
        }
        return isPrintClassLoaderTrace()
                || isPrintJVMHeapUsage()
                || isPrintJVMCpuUsage()
                || isPrintJVMThreadUsage()
                || isPrintJVMGCStats()
                || isPrintJVMClassLoaderStats()
                || isPrintJVMSystemProperties()
                || isPrintEnvironmentVariables()
                || isExposeMetrics();
    }

    public boolean isAlertsActive() {
        if (alerts != null && alerts.getEnabled() != null) {
            return alerts.getEnabled();
        }
        return isSendAlertEmails();
    }

    @Override
    public String toString() {
        return "Config{" +
                "mode=" + mode +
                ", instrumentation=" + instrumentation +
                ", observer=" + observer +
                ", alerts=" + alerts +
                ", logging=" + logging +
                ", traceFileLocation='" + traceFileLocation + '\'' +
                ", agentRules=" + agentRules +
                ", printClassLoaderTrace=" + printClassLoaderTrace +
                ", printJVMHeapUsage=" + printJVMHeapUsage +
                ", printJVMCpuUsage=" + printJVMCpuUsage +
                ", printJVMThreadUsage=" + printJVMThreadUsage +
                ", printJVMGCStats=" + printJVMGCStats +
                ", printJVMClassLoaderStats=" + printJVMClassLoaderStats +
                ", printJVMSystemProperties=" + printJVMSystemProperties +
                ", printEnvironmentVariables=" + printEnvironmentVariables +
                ", exposeMetrics=" + exposeMetrics +
                ", metricsPort=" + metricsPort +
                ", sendAlertEmails=" + sendAlertEmails +
                ", maxHeapDumps=" + maxHeapDumps +
                ", shouldInstrument=" + shouldInstrument +
                ", configRefreshInterval=" + configRefreshInterval +
                ", emailRecipientList=" + emailRecipientList +
                '}';
    }

}
