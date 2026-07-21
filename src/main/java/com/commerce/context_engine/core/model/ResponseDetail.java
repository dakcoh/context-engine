package com.commerce.context_engine.core.model;

import java.util.Locale;

public enum ResponseDetail {
    SUMMARY,
    FULL;

    public static ResponseDetail from(String value) {
        if (value == null || value.isBlank()) {
            return SUMMARY;
        }
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
