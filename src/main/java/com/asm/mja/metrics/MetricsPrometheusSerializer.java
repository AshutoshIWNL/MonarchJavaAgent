package com.asm.mja.metrics;

import java.util.Map;

/**
 * Serializes agent metrics to Prometheus/OpenMetrics text exposition format.
 * @author ashut
 * @since 22-03-2026
 */
public class MetricsPrometheusSerializer {

    public static String toPrometheus(MetricsSnapshot snapshot, boolean openMetrics) {
        StringBuilder sb = new StringBuilder(2048);

        Map<String, Object> heap = snapshot.getHeapMetrics();
        appendHelpType(sb, "monarch_jvm_heap_used_bytes", "Current JVM heap used in bytes", "gauge");
        appendMetric(sb, "monarch_jvm_heap_used_bytes", null, mbToBytes(getDouble(heap, "used (MB)")));
        appendHelpType(sb, "monarch_jvm_heap_max_bytes", "Current JVM max heap in bytes", "gauge");
        appendMetric(sb, "monarch_jvm_heap_max_bytes", null, mbToBytes(getDouble(heap, "max (MB)")));
        appendHelpType(sb, "monarch_jvm_heap_committed_bytes", "Current JVM committed heap in bytes", "gauge");
        appendMetric(sb, "monarch_jvm_heap_committed_bytes", null, mbToBytes(getDouble(heap, "committed (MB)")));
        appendHelpType(sb, "monarch_jvm_heap_usage_percent_of_max", "Heap usage as percentage of max", "gauge");
        appendMetric(sb, "monarch_jvm_heap_usage_percent_of_max", null, getDouble(heap, "usagePercentOfMax"));
        appendHelpType(sb, "monarch_jvm_heap_usage_percent_of_committed", "Heap usage as percentage of committed", "gauge");
        appendMetric(sb, "monarch_jvm_heap_usage_percent_of_committed", null, getDouble(heap, "usagePercentOfCommitted"));

        Map<String, Object> cpu = snapshot.getCPUMetrics();
        appendHelpType(sb, "monarch_jvm_cpu_process_percent", "Process CPU usage in percent", "gauge");
        appendMetric(sb, "monarch_jvm_cpu_process_percent", null, getDouble(cpu, "processCpuLoad"));
        appendHelpType(sb, "monarch_jvm_cpu_system_percent", "System CPU usage in percent", "gauge");
        appendMetric(sb, "monarch_jvm_cpu_system_percent", null, getDouble(cpu, "systemCpuLoad"));
        appendHelpType(sb, "monarch_jvm_cpu_cores", "Available CPU cores", "gauge");
        appendMetric(sb, "monarch_jvm_cpu_cores", null, getDouble(cpu, "cores"));

        Map<String, Object> threads = snapshot.getThreadMetrics();
        appendHelpType(sb, "monarch_jvm_threads_current", "Current JVM thread count", "gauge");
        appendMetric(sb, "monarch_jvm_threads_current", null, getDouble(threads, "current"));
        appendHelpType(sb, "monarch_jvm_threads_peak", "Peak JVM thread count", "gauge");
        appendMetric(sb, "monarch_jvm_threads_peak", null, getDouble(threads, "peak"));
        appendHelpType(sb, "monarch_jvm_threads_daemon", "Current JVM daemon thread count", "gauge");
        appendMetric(sb, "monarch_jvm_threads_daemon", null, getDouble(threads, "daemon"));
        appendHelpType(sb, "monarch_jvm_threads_total_started", "Total started JVM thread count", "counter");
        appendMetric(sb, "monarch_jvm_threads_total_started", null, getDouble(threads, "totalStarted"));
        appendHelpType(sb, "monarch_jvm_threads_deadlocked", "Current deadlocked thread count", "gauge");
        appendMetric(sb, "monarch_jvm_threads_deadlocked", null, getDouble(threads, "deadlocked"));

        Map<String, Object> classLoader = snapshot.getClassLoaderMetrics();
        appendHelpType(sb, "monarch_jvm_classloader_loaded", "Current loaded class count", "gauge");
        appendMetric(sb, "monarch_jvm_classloader_loaded", null, getDouble(classLoader, "loaded"));
        appendHelpType(sb, "monarch_jvm_classloader_total_loaded", "Total loaded class count", "counter");
        appendMetric(sb, "monarch_jvm_classloader_total_loaded", null, getDouble(classLoader, "totalLoaded"));
        appendHelpType(sb, "monarch_jvm_classloader_unloaded", "Total unloaded class count", "counter");
        appendMetric(sb, "monarch_jvm_classloader_unloaded", null, getDouble(classLoader, "unloaded"));

        Map<String, Object> gc = snapshot.getGCMetrics();
        appendHelpType(sb, "monarch_jvm_gc_collection_count_interval", "GC collections in the last monitor interval", "gauge");
        appendHelpType(sb, "monarch_jvm_gc_collection_time_millis_interval", "GC collection time in ms in the last monitor interval", "gauge");
        appendHelpType(sb, "monarch_jvm_gc_time_percent_interval", "GC time percent in the last monitor interval", "gauge");
        for (Map.Entry<String, Object> entry : gc.entrySet()) {
            if (!(entry.getValue() instanceof Map)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> gcData = (Map<String, Object>) entry.getValue();
            String labels = "gc=\"" + escapeLabelValue(entry.getKey()) + "\"";
            appendMetric(sb, "monarch_jvm_gc_collection_count_interval", labels, getDouble(gcData, "collectionCount"));
            appendMetric(sb, "monarch_jvm_gc_collection_time_millis_interval", labels, getDouble(gcData, "collectionTime"));
            appendMetric(sb, "monarch_jvm_gc_time_percent_interval", labels, getDouble(gcData, "gcTimePercent"));
        }

        appendHelpType(sb, "monarch_agent_info", "Agent info metric with static value 1", "gauge");
        appendMetric(sb, "monarch_agent_info", "agent=\"MonarchJavaAgent\"", 1);
        appendHelpType(sb, "monarch_scrape_timestamp_millis", "Current scrape timestamp in milliseconds", "gauge");
        appendMetric(sb, "monarch_scrape_timestamp_millis", null, System.currentTimeMillis());

        if (openMetrics) {
            sb.append("# EOF\n");
        }
        return sb.toString();
    }

    private static void appendHelpType(StringBuilder sb, String metric, String help, String type) {
        sb.append("# HELP ").append(metric).append(' ').append(help).append('\n');
        sb.append("# TYPE ").append(metric).append(' ').append(type).append('\n');
    }

    private static void appendMetric(StringBuilder sb, String metric, String labels, double value) {
        sb.append(metric);
        if (labels != null && !labels.isEmpty()) {
            sb.append('{').append(labels).append('}');
        }
        sb.append(' ').append(value).append('\n');
    }

    private static double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0;
    }

    private static double mbToBytes(double mb) {
        return mb * 1024 * 1024;
    }

    private static String escapeLabelValue(String input) {
        return input.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
