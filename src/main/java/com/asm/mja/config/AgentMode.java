package com.asm.mja.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Supported operating modes for the agent.
 * @author ashut
 * @since 22-03-2026
 */
public enum AgentMode {
    INSTRUMENTER("instrumenter"),
    OBSERVER("observer"),
    HYBRID("hybrid");

    private final String value;

    AgentMode(String value) {
        this.value = value;
    }

    @JsonCreator
    public static AgentMode fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (AgentMode mode : values()) {
            if (mode.value.equalsIgnoreCase(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unsupported agent mode - " + value);
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
