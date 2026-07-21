package com.commerce.context_engine.core.model;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record KnowledgeRule(
        String id,
        String domain,
        String category,
        String title,
        String summary,
        RuleSeverity severity,
        Applicability applicability,
        String rationale,
        String content,
        Map<String, List<String>> sections,
        List<String> checklist,
        List<Evidence> evidence,
        List<String> tags,
        List<String> aliases,
        List<String> relatedRuleIds,
        KnowledgeGovernance governance,
        LocalDate lastReviewedAt,
        RuleStatus status
) {
    public KnowledgeRule {
        id = required(id, "id");
        domain = required(domain, "domain");
        category = required(category, "category");
        title = required(title, "title");
        summary = required(summary, "summary");
        severity = Objects.requireNonNullElse(severity, RuleSeverity.MEDIUM);
        applicability = applicability == null ? Applicability.defaultFor(domain, category) : applicability;
        rationale = rationale == null || rationale.isBlank() ? summary : rationale.trim();
        content = content == null ? "" : content.trim();
        sections = immutableSections(sections);
        checklist = immutable(checklist);
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        tags = immutable(tags);
        aliases = immutable(aliases);
        relatedRuleIds = immutable(relatedRuleIds);
        governance = governance == null ? new KnowledgeGovernance("", "", "") : governance;
        status = Objects.requireNonNullElse(status, RuleStatus.ACTIVE);
    }

    public EvidenceLevel evidenceLevel() {
        if (evidence.isEmpty()) {
            return EvidenceLevel.UNSOURCED;
        }
        boolean hasExternalSource = evidence.stream().anyMatch(item ->
                (item.source().startsWith("https://") || item.source().startsWith("http://"))
                        && !item.type().equalsIgnoreCase("project-guidance")
                        && !item.type().equalsIgnoreCase("internal-reference"));
        return hasExternalSource ? EvidenceLevel.EXTERNAL_SOURCE : EvidenceLevel.PROJECT_GUIDANCE;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("KnowledgeRule " + field + " must not be blank");
        }
        return value.trim();
    }

    private static List<String> immutable(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private static Map<String, List<String>> immutableSections(Map<String, List<String>> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> copy = new LinkedHashMap<>();
        values.forEach((key, items) -> {
            List<String> immutableItems = immutable(items);
            if (key != null && !key.isBlank() && !immutableItems.isEmpty()) {
                copy.put(key.trim(), immutableItems);
            }
        });
        return java.util.Collections.unmodifiableMap(copy);
    }
}
