package com.commerce.context_engine.core.model;

import java.util.List;
import java.time.LocalDate;

public record ReviewFinding(
        String findingId,
        String ruleId,
        RuleSeverity severity,
        FindingStatus status,
        RuleStatus ruleStatus,
        EvidenceLevel evidenceLevel,
        String owner,
        String verifiedBy,
        LocalDate lastReviewedAt,
        String title,
        String reason,
        List<String> matchedEvidence,
        List<String> recommendations
) {
    public ReviewFinding {
        matchedEvidence = matchedEvidence == null ? List.of() : List.copyOf(matchedEvidence);
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
        owner = owner == null ? "" : owner.trim();
        verifiedBy = verifiedBy == null || verifiedBy.isBlank() ? null : verifiedBy.trim();
    }
}
