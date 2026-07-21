package com.commerce.context_engine.adapter.out.yaml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = false)
public record KnowledgeYamlDocument(
        @JsonProperty("schema-version") int schemaVersion,
        String domain,
        GovernanceDocument governance,
        List<RuleDocument> rules
) {
    @JsonIgnoreProperties(ignoreUnknown = false)
    public record GovernanceDocument(
            String owner
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    public record RuleDocument(
            String id,
            String category,
            String title,
            String summary,
            String severity,
            ApplicabilityDocument applicability,
            String rationale,
            String content,
            @JsonProperty("business-context") String businessContext,
            List<String> guidance,
            @JsonProperty("avoid-patterns") List<String> avoidPatterns,
            List<String> checklist,
            List<String> tags,
            List<String> aliases,
            @JsonProperty("related-rule-ids") List<String> relatedRuleIds,
            List<EvidenceDocument> evidence,
            ReviewDocument review,
            String status,
            List<String> invariants,
            List<String> workflow,
            @JsonProperty("technical-guidance") List<String> technicalGuidance,
            @JsonProperty("failure-scenarios") List<String> failureScenarios,
            @JsonProperty("spring-guidance") List<String> springGuidance
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    public record ReviewDocument(
            @JsonProperty("last-reviewed-at") String lastReviewedAt,
            @JsonProperty("verified-by") String verifiedBy,
            @JsonProperty("change-note") String changeNote
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    public record ApplicabilityDocument(
            @JsonProperty("applies-when") List<String> appliesWhen,
            @JsonProperty("does-not-apply-when") List<String> doesNotApplyWhen,
            List<String> assumptions,
            String jurisdiction
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    public record EvidenceDocument(
            String title,
            String source,
            String url,
            String type,
            @JsonProperty("source-version") String sourceVersion,
            @JsonProperty("published-at") String publishedAt,
            @JsonProperty("reviewed-at") String reviewedAt,
            @JsonProperty("accessed-at") String accessedAt,
            String note
    ) {
    }
}
