package com.commerce.context_engine.core.model;

import java.util.Locale;

public enum RuleSeverity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
    INFO;

    public static RuleSeverity from(String value) {
        if (value == null || value.isBlank()) {
            return MEDIUM;
        }
        return valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
    }
}
