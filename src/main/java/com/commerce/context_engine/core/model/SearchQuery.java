package com.commerce.context_engine.core.model;

import java.util.Set;
import java.util.Locale;

public record SearchQuery(
        String query,
        Set<String> domains,
        Set<String> categories,
        Set<RuleSeverity> severities,
        int topK,
        ResponseDetail detail,
        boolean includeDeprecated
) {
    public static final int DEFAULT_TOP_K = 5;
    public static final int MAX_TOP_K = 10;

    public SearchQuery {
        query = query == null ? "" : query.trim();
        domains = immutable(domains);
        categories = immutable(categories);
        severities = severities == null ? Set.of() : Set.copyOf(severities);
        if (topK <= 0 || topK > MAX_TOP_K) {
            throw new IllegalArgumentException("topK must be between 1 and " + MAX_TOP_K);
        }
        detail = detail == null ? ResponseDetail.SUMMARY : detail;
    }

    public static SearchQuery of(String query) {
        return new SearchQuery(query, Set.of(), Set.of(), Set.of(), DEFAULT_TOP_K,
                ResponseDetail.SUMMARY, false);
    }

    public static SearchQuery of(String query, String domain, int topK, ResponseDetail detail) {
        return new SearchQuery(query,
                domain == null || domain.isBlank() ? Set.of() : Set.of(domain.trim()),
                Set.of(), Set.of(), topK, detail, false);
    }

    private static Set<String> immutable(Set<String> values) {
        return values == null ? Set.of() : values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
