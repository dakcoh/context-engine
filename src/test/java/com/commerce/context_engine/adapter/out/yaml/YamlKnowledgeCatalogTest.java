package com.commerce.context_engine.adapter.out.yaml;

import com.commerce.context_engine.core.port.KnowledgeCatalog;
import com.commerce.context_engine.core.model.EvidenceLevel;
import com.commerce.context_engine.core.model.RuleStatus;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class YamlKnowledgeCatalogTest {

    @Autowired
    KnowledgeCatalog catalog;

    @Test
    void loadsAllYamlThroughCommonSchema() {
        assertThat(catalog.findAll()).hasSizeGreaterThanOrEqualTo(70);
        assertThat(catalog.domains()).hasSize(6);
    }

    @Test
    void findById_returnsNormalizedRule() {
        var rule = catalog.findById("available-stock-query").orElseThrow();
        assertThat(rule.domain()).isEqualTo("inventory");
        assertThat(rule.sections()).containsKey("guidance");
        assertThat(rule.evidence()).isNotEmpty();
        assertThat(rule.evidenceLevel()).isEqualTo(EvidenceLevel.PROJECT_GUIDANCE);
        assertThat(rule.lastReviewedAt()).isNull();
        assertThat(rule.governance().owner()).isEqualTo("commerce-context-maintainers");
        assertThat(rule.governance().verifiedBy()).isBlank();
    }

    @Test
    void findByDomains_filtersWithoutDomainSpecificJavaConfiguration() {
        var rules = catalog.findByDomains(Set.of("payment", "coupon"));
        assertThat(rules).isNotEmpty();
        assertThat(rules).allMatch(rule -> Set.of("payment", "coupon").contains(rule.domain()));
    }

    @Test
    void mapsOptionalGovernanceReviewAndEvidenceWithoutTrustBypass() throws Exception {
        String yaml = """
                schema-version: 1
                domain: payment
                governance:
                  owner: payment-maintainers
                rules:
                  - id: verified-rule
                    category: security
                    title: Verified rule
                    summary: Verified summary
                    severity: high
                    status: active
                    applicability:
                      applies-when: ["condition"]
                      jurisdiction: KR
                    tags: [payment, security]
                    review:
                      last-reviewed-at: 2026-07-21
                      verified-by: payment-reviewer
                      change-note: Initial verification
                    evidence:
                      - title: Official contract
                        url: https://official.example/contract
                        type: official-documentation
                        source-version: v2
                        accessed-at: 2026-07-21
                  - id: unverified-rule
                    category: security
                    title: Unverified rule
                    summary: Unverified summary
                    severity: critical
                    status: active
                    tags: [payment, security]
                """;
        var document = YAMLMapper.builder().findAndAddModules().build()
                .readValue(yaml, KnowledgeYamlDocument.class);
        var mapper = new YamlKnowledgeCatalog();

        var verified = mapper.map(document.domain(), document.governance(), document.rules().get(0));
        var unverified = mapper.map(document.domain(), document.governance(), document.rules().get(1));

        assertThat(verified.status()).isEqualTo(RuleStatus.ACTIVE);
        assertThat(verified.evidenceLevel()).isEqualTo(EvidenceLevel.EXTERNAL_SOURCE);
        assertThat(verified.governance().owner()).isEqualTo("payment-maintainers");
        assertThat(verified.governance().verifiedBy()).isEqualTo("payment-reviewer");
        assertThat(verified.evidence().get(0).sourceVersion()).isEqualTo("v2");
        assertThat(verified.evidence().get(0).accessedAt()).hasToString("2026-07-21");
        assertThat(unverified.status()).isEqualTo(RuleStatus.NEEDS_REVIEW);
    }
}
