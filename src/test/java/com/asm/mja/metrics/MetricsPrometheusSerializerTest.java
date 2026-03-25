package com.asm.mja.metrics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for Prometheus metrics serialization.
 * @author ashut
 * @since 24-03-2026
 */
public class MetricsPrometheusSerializerTest {

    @Test
    void toPrometheusIncludesGcCountersAndNormalizedRates() {
        MetricsSnapshot snapshot = MetricsSnapshot.getInstance();
        snapshot.updateGCMetrics(
                "G1 Young Generation",
                5,
                500,
                5.0,
                120,
                3456,
                0.5,
                0.05,
                10.0
        );

        String payload = MetricsPrometheusSerializer.toPrometheus(snapshot, false);

        assertTrue(payload.contains("# TYPE monarch_jvm_gc_collection_count_total counter"));
        assertTrue(payload.contains("# TYPE monarch_jvm_gc_collection_time_seconds_total counter"));
        assertTrue(payload.contains("monarch_jvm_gc_collection_count_total{gc=\"G1 Young Generation\"} 120.0"));
        assertTrue(payload.contains("monarch_jvm_gc_collection_time_seconds_total{gc=\"G1 Young Generation\"} 3.456"));
        assertTrue(payload.contains("monarch_jvm_gc_collection_count_per_second{gc=\"G1 Young Generation\"} 0.5"));
        assertTrue(payload.contains("monarch_jvm_gc_collection_time_seconds_per_second{gc=\"G1 Young Generation\"} 0.05"));
        assertTrue(payload.contains("monarch_jvm_gc_monitor_interval_seconds{gc=\"G1 Young Generation\"} 10.0"));
    }
}
