package com.asm.mja.config;

/**
 * Nested observer configuration section.
 * @author ashut
 * @since 22-03-2026
 */
public class ObserverConfig {
    private Boolean enabled;
    private Boolean printClassLoaderTrace;
    private Boolean printJVMSystemProperties;
    private Boolean printEnvironmentVariables;
    private ObserverMetricsConfig metrics;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getPrintClassLoaderTrace() {
        return printClassLoaderTrace;
    }

    public void setPrintClassLoaderTrace(Boolean printClassLoaderTrace) {
        this.printClassLoaderTrace = printClassLoaderTrace;
    }

    public Boolean getPrintJVMSystemProperties() {
        return printJVMSystemProperties;
    }

    public void setPrintJVMSystemProperties(Boolean printJVMSystemProperties) {
        this.printJVMSystemProperties = printJVMSystemProperties;
    }

    public Boolean getPrintEnvironmentVariables() {
        return printEnvironmentVariables;
    }

    public void setPrintEnvironmentVariables(Boolean printEnvironmentVariables) {
        this.printEnvironmentVariables = printEnvironmentVariables;
    }

    public ObserverMetricsConfig getMetrics() {
        return metrics;
    }

    public void setMetrics(ObserverMetricsConfig metrics) {
        this.metrics = metrics;
    }
}
