package com.commerce.context_engine.core.model;

import java.util.Locale;

public enum RuleStatus {
    ACTIVE,
    NEEDS_REVIEW,
    DEPRECATED;

    public static RuleStatus from(String value) {
        if (value == null || value.isBlank()) {
            return ACTIVE;
        }
        return valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
    }
}
