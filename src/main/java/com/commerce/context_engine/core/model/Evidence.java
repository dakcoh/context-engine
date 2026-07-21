package com.commerce.context_engine.core.model;

import java.time.LocalDate;

public record Evidence(
        String title,
        String source,
        String type,
        String sourceVersion,
        LocalDate publishedAt,
        LocalDate reviewedAt,
        LocalDate accessedAt,
        String note
) {
    public Evidence {
        title = title == null ? "" : title.trim();
        source = source == null ? "" : source.trim();
        type = type == null ? "internal" : type.trim();
        sourceVersion = sourceVersion == null ? "" : sourceVersion.trim();
        note = note == null ? "" : note.trim();
    }
}
