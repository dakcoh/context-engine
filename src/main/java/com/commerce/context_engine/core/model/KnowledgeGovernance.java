package com.commerce.context_engine.core.model;

public record KnowledgeGovernance(
        String owner,
        String verifiedBy,
        String changeNote
) {
    public KnowledgeGovernance {
        owner = normalize(owner);
        verifiedBy = normalize(verifiedBy);
        changeNote = normalize(changeNote);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
