package com.commerce.context_engine.core.model;

import java.util.List;

public record SearchResult(
        KnowledgeRule rule,
        double score,
        double queryCoverage,
        List<String> matchedFields,
        List<String> matchedTerms
) {
    public SearchResult {
        matchedFields = matchedFields == null ? List.of() : List.copyOf(matchedFields);
        matchedTerms = matchedTerms == null ? List.of() : List.copyOf(matchedTerms);
    }

    public SearchConfidence confidence() {
        if (queryCoverage >= 0.8 && score >= 20) {
            return SearchConfidence.HIGH;
        }
        if (queryCoverage >= 0.5 && score >= 12) {
            return SearchConfidence.MEDIUM;
        }
        return SearchConfidence.LOW;
    }
}
