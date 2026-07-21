package com.commerce.context_engine;

import com.commerce.context_engine.core.model.RuleStatus;
import com.commerce.context_engine.core.port.KnowledgeCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class KnowledgeSchemaValidationTest {

    private static final Pattern RULE_ID = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    @Autowired
    KnowledgeCatalog catalog;

    @Test
    void allRules_haveGloballyUniqueValidIdsAndRequiredFields() {
        var rules = catalog.findAll();
        assertThat(rules).hasSizeGreaterThanOrEqualTo(70);
        assertThat(rules.stream().map(rule -> rule.id()).toList())
                .doesNotHaveDuplicates()
                .allMatch(id -> RULE_ID.matcher(id).matches());
        assertThat(rules).allSatisfy(rule -> {
            assertThat(rule.domain()).isNotBlank();
            assertThat(rule.category()).isNotBlank();
            assertThat(rule.title()).isNotBlank();
            assertThat(rule.summary()).isNotBlank();
            assertThat(rule.rationale()).isNotBlank();
            assertThat(rule.tags()).isNotEmpty().doesNotHaveDuplicates();
            assertThat(rule.governance().owner()).isEqualTo("commerce-context-maintainers");
            if (rule.lastReviewedAt() != null) {
                assertThat(rule.lastReviewedAt()).isBeforeOrEqualTo(LocalDate.now());
            }
        });
    }

    @Test
    void catalog_containsAllPublishedDomains() {
        assertThat(catalog.domains()).containsExactlyInAnyOrder(
                "inventory", "payment", "settlement", "coupon", "commerce", "spring-commerce");
    }

    @Test
    void everyRule_hasApplicabilityAndEvidence() {
        assertThat(catalog.findAll()).allSatisfy(rule -> {
            assertThat(rule.applicability().appliesWhen()).isNotEmpty();
            assertThat(rule.applicability().jurisdiction()).isEqualTo("KR");
            assertThat(rule.evidence()).isNotEmpty();
            assertThat(rule.evidenceLevel()).isNotNull();
            assertThat(rule.governance().owner()).isNotBlank();
            if (rule.lastReviewedAt() == null) {
                assertThat(rule.governance().verifiedBy()).isBlank();
            }
            else {
                assertThat(rule.governance().verifiedBy()).isNotBlank();
            }
            rule.evidence().stream()
                    .filter(evidence -> evidence.source().startsWith("https://")
                            || evidence.source().startsWith("http://"))
                    .forEach(evidence -> assertThat(evidence.accessedAt()).isNotNull());
        });
    }

    @Test
    void missingReviewDates_areNotFabricatedAndHighRiskRulesAreFlaggedForReview() {
        assertThat(catalog.findById("available-stock-query").orElseThrow().lastReviewedAt()).isNull();
        assertThat(catalog.findById("webhook-handling").orElseThrow().status()).isEqualTo(RuleStatus.NEEDS_REVIEW);
        assertThat(catalog.findAll().stream()
                .filter(rule -> rule.severity() == com.commerce.context_engine.core.model.RuleSeverity.CRITICAL
                        || rule.severity() == com.commerce.context_engine.core.model.RuleSeverity.HIGH))
                .isNotEmpty()
                .allSatisfy(rule -> assertThat(rule.status()).isEqualTo(RuleStatus.NEEDS_REVIEW));
        assertThat(catalog.findById("settlement-tax").orElseThrow().lastReviewedAt()).isNull();
    }

    @Test
    void deprecatedRules_referenceOnlyExistingRules() {
        Set<String> ids = catalog.findAll().stream()
                .map(rule -> rule.id())
                .collect(java.util.stream.Collectors.toSet());
        assertThat(catalog.findAll()).allSatisfy(rule ->
                assertThat(rule.relatedRuleIds()).allMatch(ids::contains));
        assertThat(catalog.findAll().stream().filter(rule -> rule.status() == RuleStatus.DEPRECATED))
                .allSatisfy(rule -> assertThat(rule.relatedRuleIds()).isNotEmpty());
    }
}
