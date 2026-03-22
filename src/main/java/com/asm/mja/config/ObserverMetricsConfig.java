package com.asm.mja.config;

/**
 * Nested observer metrics configuration section.
 * @author ashut
 * @since 22-03-2026
 */
public class ObserverMetricsConfig {
    private Boolean exposeHttp;
    private Integer port;
    private Boolean heapUsage;
    private Boolean cpuUsage;
    private Boolean threadUsage;
    private Boolean gcStats;
    private Boolean classLoaderStats;

    public Boolean getExposeHttp() {
        return exposeHttp;
    }

    public void setExposeHttp(Boolean exposeHttp) {
        this.exposeHttp = exposeHttp;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Boolean getHeapUsage() {
        return heapUsage;
    }

    public void setHeapUsage(Boolean heapUsage) {
        this.heapUsage = heapUsage;
    }

    public Boolean getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(Boolean cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public Boolean getThreadUsage() {
        return threadUsage;
    }

    public void setThreadUsage(Boolean threadUsage) {
        this.threadUsage = threadUsage;
    }

    public Boolean getGcStats() {
        return gcStats;
    }

    public void setGcStats(Boolean gcStats) {
        this.gcStats = gcStats;
    }

    public Boolean getClassLoaderStats() {
        return classLoaderStats;
    }

    public void setClassLoaderStats(Boolean classLoaderStats) {
        this.classLoaderStats = classLoaderStats;
    }
}
