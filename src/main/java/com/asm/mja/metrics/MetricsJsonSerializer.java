package com.asm.mja.metrics;

import java.util.Map;

/**
 * @author ashut
 * @since 01-12-2025
 */

public class MetricsJsonSerializer {

    public static String toJson(Map<String, Object> map) {
        return toJsonObject(map);
    }

    @SuppressWarnings("unchecked")
    private static String toJsonObject(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;

            sb.append("\"").append(entry.getKey()).append("\":");
            sb.append(toJsonValue(entry.getValue()));
        }
        sb.append("}");
        return sb.toString();
    }

    private static String toJsonValue(Object value) {
        if (value instanceof Map) {
            return toJsonObject((Map<String, Object>) value);
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Boolean) {
            return value.toString();
        }
        return "\"" + value.toString().replace("\"", "\\\"") + "\"";
    }
}
