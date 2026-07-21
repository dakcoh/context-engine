package com.commerce.context_engine.application.search;

import com.commerce.context_engine.core.model.Applicability;
import com.commerce.context_engine.core.model.KnowledgeRule;
import com.commerce.context_engine.core.model.ResponseDetail;
import com.commerce.context_engine.core.model.RuleSeverity;
import com.commerce.context_engine.core.model.RuleStatus;
import com.commerce.context_engine.core.model.SearchQuery;
import com.commerce.context_engine.core.port.KnowledgeCatalog;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultKnowledgeSearchTest {

    @Test
    void blankQuery_respectsTopK() {
        var search = searchWith(
                rule("a-rule", "inventory", "concurrency", "재고 락", "동시성"),
                rule("b-rule", "inventory", "lifecycle", "재고 예약", "예약"),
                rule("c-rule", "payment", "webhook", "결제 웹훅", "웹훅")
        );

        var results = search.search(new SearchQuery("", Set.of(), Set.of(), Set.of(),
                2, ResponseDetail.SUMMARY, false));

        assertThat(results).hasSize(2);
    }

    @Test
    void categoryFilter_worksWithoutDomain() {
        var search = searchWith(
                rule("a-rule", "inventory", "concurrency", "재고 락", "동시성"),
                rule("b-rule", "spring-commerce", "concurrency", "Spring 락", "동시성"),
                rule("c-rule", "payment", "webhook", "결제 웹훅", "웹훅")
        );

        var results = search.search(new SearchQuery("", Set.of(), Set.of("concurrency"), Set.of(),
                5, ResponseDetail.SUMMARY, false));

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(result -> result.rule().category().equals("concurrency"));
    }

    @Test
    void multiWordQuery_ranksHigherCoverageFirst() {
        var search = searchWith(
                rule("webhook-only", "payment", "webhook", "결제 웹훅", "웹훅 처리"),
                rule("precise", "payment", "webhook", "결제 웹훅 중복 처리", "멱등성으로 중복 결제를 막는다")
        );

        var results = search.search(SearchQuery.of("웹훅 중복 결제"));

        assertThat(results.get(0).rule().id()).isEqualTo("precise");
        assertThat(results.get(0).queryCoverage()).isGreaterThan(results.get(1).queryCoverage());
    }

    @Test
    void scoreIncludesMatchedFieldsAndTerms() {
        var search = searchWith(rule("stock-rule", "inventory", "concurrency", "낙관락", "재고 충돌"));

        var result = search.search(SearchQuery.of("낙관락 재고")).get(0);

        assertThat(result.matchedFields()).contains("title", "body");
        assertThat(result.matchedTerms()).contains("낙관락", "재고");
        assertThat(result.confidence()).isNotNull();
    }

    @Test
    void weakPartialMatch_isNotReturned() {
        var search = searchWith(rule("stock-rule", "inventory", "concurrency", "낙관락", "재고 충돌"));

        assertThat(search.search(SearchQuery.of("낙관락 여행 날씨 음악"))).isEmpty();
    }

    @Test
    void queryRejectsInvalidTopK() {
        assertThatThrownBy(() -> new SearchQuery("재고", Set.of(), Set.of(), Set.of(),
                11, ResponseDetail.SUMMARY, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void knowledgeRule_isDeeplyImmutable() {
        Map<String, List<String>> sections = new LinkedHashMap<>();
        sections.put("guidance", new java.util.ArrayList<>(List.of("가이드")));
        KnowledgeRule rule = rule("immutable", "inventory", "lifecycle", "불변", "불변 규칙", sections);
        sections.get("guidance").add("변경");

        assertThat(rule.sections().get("guidance")).containsExactly("가이드");
        assertThatThrownBy(() -> rule.sections().put("x", List.of("y")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static DefaultKnowledgeSearch searchWith(KnowledgeRule... rules) {
        List<KnowledgeRule> values = List.of(rules);
        KnowledgeCatalog catalog = new KnowledgeCatalog() {
            @Override
            public List<KnowledgeRule> findAll() {
                return values;
            }

            @Override
            public Optional<KnowledgeRule> findById(String id) {
                return values.stream().filter(rule -> rule.id().equals(id)).findFirst();
            }

            @Override
            public List<KnowledgeRule> findByDomains(Set<String> domains) {
                return domains.isEmpty() ? values
                        : values.stream().filter(rule -> domains.contains(rule.domain())).toList();
            }

            @Override
            public Set<String> domains() {
                return values.stream().map(KnowledgeRule::domain).collect(java.util.stream.Collectors.toSet());
            }
        };
        return new DefaultKnowledgeSearch(catalog);
    }

    private static KnowledgeRule rule(String id, String domain, String category, String title, String summary) {
        return rule(id, domain, category, title, summary, Map.of("guidance", List.of(summary)));
    }

    private static KnowledgeRule rule(
            String id,
            String domain,
            String category,
            String title,
            String summary,
            Map<String, List<String>> sections
    ) {
        return new KnowledgeRule(
                id, domain, category, title, summary, RuleSeverity.MEDIUM,
                Applicability.defaultFor(domain, category), summary, "", sections,
                List.of("확인한다"), List.of(), List.of(domain, category), List.of(), List.of(),
                new com.commerce.context_engine.core.model.KnowledgeGovernance(
                        "test-maintainers", "test-reviewer", "test fixture"),
                LocalDate.of(2026, 7, 21), RuleStatus.ACTIVE
        );
    }
}
