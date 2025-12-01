package com.asm.mja.metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ashut
 * @since 01-12-2025
 * Thread-safe storage for all metrics that can be exposed via HTTP
 */
public class MetricsSnapshot {
    private static final MetricsSnapshot instance = new MetricsSnapshot();

    private final Map<String, Object> gcMetrics = new ConcurrentHashMap<>();
    private final Map<String, Object> heapMetrics = new ConcurrentHashMap<>();
    private final Map<String, Object> cpuMetrics = new ConcurrentHashMap<>();
    private final Map<String, Object> threadMetrics = new ConcurrentHashMap<>();
    private final Map<String, Object> classLoaderMetrics = new ConcurrentHashMap<>();

    private MetricsSnapshot() {}

    public static MetricsSnapshot getInstance() {
        return instance;
    }

    public void updateGCMetrics(String gcName, long collectionCount, long collectionTime, double gcTimePercent) {
        Map<String, Object> gcData = new HashMap<>();
        gcData.put("collectionCount", collectionCount);
        gcData.put("collectionTime", collectionTime);
        gcData.put("gcTimePercent", gcTimePercent);
        gcMetrics.put(gcName, gcData);
    }

    public Map<String, Object> getGCMetrics() {
        return new HashMap<>(gcMetrics);
    }

    public void updateHeapMetrics(long used, long max, long committed, double usagePercentOfMax, double usagePercentOfCommitted) {
        heapMetrics.put("used (MB)", used);
        heapMetrics.put("max (MB)", max);
        heapMetrics.put("committed (MB)", committed);
        heapMetrics.put("usagePercentOfMax", usagePercentOfMax);
        heapMetrics.put("usagePercentOfCommitted", usagePercentOfCommitted);
    }

    public Map<String, Object> getHeapMetrics() {
        return new HashMap<>(heapMetrics);
    }

    public void updateCPUMetrics(double processCpuLoad, double systemCpuLoad, long cores) {
        cpuMetrics.put("processCpuLoad", processCpuLoad);
        cpuMetrics.put("systemCpuLoad", systemCpuLoad);
        cpuMetrics.put("cores", cores);
    }

    public Map<String, Object> getCPUMetrics() {
        return new HashMap<>(cpuMetrics);
    }

    public void updateThreadMetrics(int current, int peak, int daemon, long totalStarted, int deadlocked) {
        threadMetrics.put("current", current);
        threadMetrics.put("peak", peak);
        threadMetrics.put("daemon", daemon);
        threadMetrics.put("totalStarted", totalStarted);
        threadMetrics.put("deadlocked", deadlocked);
    }

    public Map<String, Object> getThreadMetrics() {
        return new HashMap<>(threadMetrics);
    }

    public void updateClassLoaderMetrics(long loaded, long totalLoaded, long unloaded) {
        classLoaderMetrics.put("loaded", loaded);
        classLoaderMetrics.put("totalLoaded", totalLoaded);
        classLoaderMetrics.put("unloaded", unloaded);
    }

    public Map<String, Object> getClassLoaderMetrics() {
        return new HashMap<>(classLoaderMetrics);
    }

    public Map<String, Object> getAllMetrics() {
        Map<String, Object> allMetrics = new HashMap<>();
        allMetrics.put("gc", getGCMetrics());
        allMetrics.put("heap", getHeapMetrics());
        allMetrics.put("cpu", getCPUMetrics());
        allMetrics.put("threads", getThreadMetrics());
        allMetrics.put("classLoader", getClassLoaderMetrics());
        allMetrics.put("timestamp", System.currentTimeMillis());
        allMetrics.put("agent", "MonarchJavaAgent");
        return allMetrics;
    }
}
