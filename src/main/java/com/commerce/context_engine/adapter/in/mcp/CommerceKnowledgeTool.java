package com.commerce.context_engine.adapter.in.mcp;

import com.commerce.context_engine.application.checklist.ChecklistService;
import com.commerce.context_engine.core.model.KnowledgeRule;
import com.commerce.context_engine.core.model.ResponseDetail;
import com.commerce.context_engine.core.model.ReviewReport;
import com.commerce.context_engine.core.model.ReviewRequest;
import com.commerce.context_engine.core.model.SearchQuery;
import com.commerce.context_engine.core.model.SearchResult;
import com.commerce.context_engine.core.port.KnowledgeRenderer;
import com.commerce.context_engine.core.port.KnowledgeReviewer;
import com.commerce.context_engine.core.port.KnowledgeSearch;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.time.LocalDate;

@Component
public class CommerceKnowledgeTool {

    private static final String TRUST_NOTICE = "프로젝트 관리 설계 지침입니다. evidenceLevel, owner, verifiedBy와 lastReviewedAt을 확인하고, 결제·세무·보안·법률 결정은 최신 공식 문서와 전문가 검토로 확정하세요.";

    private final KnowledgeSearch search;
    private final KnowledgeReviewer reviewer;
    private final ChecklistService checklistService;
    private final KnowledgeRenderer renderer;

    public CommerceKnowledgeTool(
            KnowledgeSearch search,
            KnowledgeReviewer reviewer,
            ChecklistService checklistService,
            KnowledgeRenderer renderer
    ) {
        this.search = search;
        this.reviewer = reviewer;
        this.checklistService = checklistService;
        this.renderer = renderer;
    }

    @Tool(name = "search_knowledge",
          description = """
                  한국 커머스와 Java/Spring 구현 규칙을 통합 검색합니다.
                  관련도, 질의 커버리지, 매칭 필드와 규칙 상태를 구조화해 반환합니다.
                  특정 규칙 ID를 모르는 일반적인 설계·구현 질문에서 우선 사용하세요.
                  """)
    public KnowledgeSearchResponse searchKnowledge(
            @ToolParam(description = "검색 질의", required = true) String query,
            @ToolParam(description = "선택 domain: inventory, payment, settlement, coupon, commerce, spring-commerce", required = false) String domain,
            @ToolParam(description = "선택 category", required = false) String category,
            @ToolParam(description = "결과 수 1~10, 기본 5", required = false) Integer topK,
            @ToolParam(description = "summary 또는 full, 기본 summary", required = false) String detail
    ) {
        if (query == null || query.isBlank()) {
            return new KnowledgeSearchResponse(
                    query == null ? "" : query,
                    0,
                    List.of(),
                    false,
                    "검색 질의를 입력하세요. domain, 기능, 실패 상황을 함께 적으면 더 정확하게 찾을 수 있습니다.",
                    TRUST_NOTICE,
                    ""
            );
        }
        int limit = topK == null ? SearchQuery.DEFAULT_TOP_K : topK;
        ResponseDetail responseDetail = ResponseDetail.from(detail);
        SearchQuery searchQuery = new SearchQuery(
                query,
                values(domain),
                values(category),
                Set.of(),
                limit,
                responseDetail,
                false
        );
        List<SearchResult> results = search.search(searchQuery);
        List<RuleMatch> matches = results.stream().map(RuleMatch::from).toList();
        return new KnowledgeSearchResponse(
                query,
                results.size(),
                matches,
                !results.isEmpty(),
                results.isEmpty()
                        ? "관련성이 충분한 규칙을 찾지 못했습니다. domain, 기능, 실패 상황을 포함해 질문을 구체화하세요."
                        : "관련 규칙 후보입니다. matchConfidence와 queryCoverage를 확인하세요.",
                TRUST_NOTICE,
                renderer.renderSearchResults(results, responseDetail)
        );
    }

    @Tool(name = "get_rule",
          description = "knowledge rule ID로 단일 규칙의 적용 조건, 예외, 가이드, 체크리스트와 출처를 조회합니다.")
    public RuleResponse getRule(
            @ToolParam(description = "정확한 knowledge rule ID", required = true) String ruleId
    ) {
        KnowledgeRule rule = search.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown knowledge rule: " + ruleId));
        return new RuleResponse(
                RuleView.from(rule),
                TRUST_NOTICE,
                renderer.renderRules(List.of(rule), ResponseDetail.FULL));
    }

    @Tool(name = "get_checklist",
          description = "domain과 선택적인 시나리오에 맞는 중복 제거 체크리스트를 반환합니다.")
    public ChecklistResponse getChecklist(
            @ToolParam(description = "domain", required = true) String domain,
            @ToolParam(description = "선택 시나리오 또는 기능 설명", required = false) String scenario
    ) {
        List<KnowledgeRule> rules = checklistService.find(domain, scenario);
        return new ChecklistResponse(
                domain,
                rules.stream().map(rule -> new ChecklistRule(
                        rule.id(), rule.title(), value(rule.severity()), value(rule.status()),
                        value(rule.evidenceLevel()), rule.governance().owner(),
                        emptyToNull(rule.governance().verifiedBy()), rule.lastReviewedAt(), rule.checklist())).toList(),
                TRUST_NOTICE,
                renderer.renderChecklist(rules)
        );
    }

    @Tool(name = "review_commerce_design",
          description = """
                  설계안, API 설명, DDL 또는 코드 조각에서 적용 가능성이 높은 커머스 규칙을 찾습니다.
                  결과는 확정 버그가 아니라 검토가 필요한 규칙 기반 후보입니다.
                  """)
    public ReviewResponse reviewCommerceDesign(
            @ToolParam(description = "검토할 설계안, API, DDL 또는 코드", required = true) String artifact,
            @ToolParam(description = "design, api, ddl, code 중 하나", required = false) String artifactType,
            @ToolParam(description = "선택 domain", required = false) String domain,
            @ToolParam(description = "선택 시나리오", required = false) String scenario,
            @ToolParam(description = "후보 수 1~10, 기본 5", required = false) Integer maxFindings
    ) {
        ReviewReport report = reviewer.review(new ReviewRequest(
                artifact,
                artifactType,
                values(domain),
                scenario,
                maxFindings == null ? SearchQuery.DEFAULT_TOP_K : maxFindings
        ));
        return ReviewResponse.from(report);
    }

    private static Set<String> values(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .map(item -> item.toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String value(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    public record KnowledgeSearchResponse(
            String query,
            int count,
            List<RuleMatch> results,
            boolean answerable,
            String message,
            String notice,
            String markdown
    ) {
    }

    public record RuleMatch(
            String ruleId,
            String domain,
            String category,
            String title,
            String summary,
            String severity,
            String status,
            String evidenceLevel,
            String owner,
            String verifiedBy,
            LocalDate lastReviewedAt,
            String matchConfidence,
            double score,
            double queryCoverage,
            List<String> matchedFields,
            List<String> matchedTerms
    ) {
        static RuleMatch from(SearchResult result) {
            KnowledgeRule rule = result.rule();
            return new RuleMatch(rule.id(), rule.domain(), rule.category(), rule.title(), rule.summary(),
                    rule.severity().name().toLowerCase(), rule.status().name().toLowerCase().replace('_', '-'),
                    rule.evidenceLevel().name().toLowerCase().replace('_', '-'),
                    rule.governance().owner(), emptyToNull(rule.governance().verifiedBy()), rule.lastReviewedAt(),
                    result.confidence().name().toLowerCase(),
                    result.score(), result.queryCoverage(), result.matchedFields(), result.matchedTerms());
        }
    }

    public record RuleResponse(
            RuleView rule,
            String notice,
            String markdown
    ) {
    }

    public record RuleView(
            String id,
            String domain,
            String category,
            String title,
            String summary,
            String severity,
            String status,
            String evidenceLevel,
            String owner,
            String verifiedBy,
            LocalDate lastReviewedAt,
            String changeNote,
            com.commerce.context_engine.core.model.Applicability applicability,
            String rationale,
            String content,
            Map<String, List<String>> sections,
            List<String> checklist,
            List<com.commerce.context_engine.core.model.Evidence> evidence,
            List<String> tags,
            List<String> aliases,
            List<String> relatedRuleIds
    ) {
        static RuleView from(KnowledgeRule rule) {
            return new RuleView(
                    rule.id(), rule.domain(), rule.category(), rule.title(), rule.summary(),
                    value(rule.severity()), value(rule.status()), value(rule.evidenceLevel()),
                    rule.governance().owner(), emptyToNull(rule.governance().verifiedBy()),
                    rule.lastReviewedAt(), emptyToNull(rule.governance().changeNote()),
                    rule.applicability(), rule.rationale(), rule.content(), rule.sections(),
                    rule.checklist(), rule.evidence(), rule.tags(), rule.aliases(), rule.relatedRuleIds()
            );
        }
    }

    public record ChecklistResponse(String domain, List<ChecklistRule> rules, String notice, String markdown) {
    }

    public record ChecklistRule(
            String ruleId,
            String title,
            String severity,
            String status,
            String evidenceLevel,
            String owner,
            String verifiedBy,
            LocalDate lastReviewedAt,
            List<String> items
    ) {
    }

    public record ReviewResponse(
            String artifactType,
            Set<String> detectedDomains,
            List<ReviewFindingView> findings,
            String notice
    ) {
        static ReviewResponse from(ReviewReport report) {
            return new ReviewResponse(
                    report.artifactType(),
                    report.detectedDomains(),
                    report.findings().stream().map(ReviewFindingView::from).toList(),
                    report.notice()
            );
        }
    }

    public record ReviewFindingView(
            String findingId,
            String ruleId,
            String severity,
            String status,
            String ruleStatus,
            String evidenceLevel,
            String owner,
            String verifiedBy,
            LocalDate lastReviewedAt,
            String title,
            String reason,
            List<String> matchedEvidence,
            List<String> recommendations
    ) {
        static ReviewFindingView from(com.commerce.context_engine.core.model.ReviewFinding finding) {
            return new ReviewFindingView(
                    finding.findingId(), finding.ruleId(), value(finding.severity()), value(finding.status()),
                    value(finding.ruleStatus()), value(finding.evidenceLevel()), finding.owner(),
                    finding.verifiedBy(), finding.lastReviewedAt(), finding.title(), finding.reason(),
                    finding.matchedEvidence(), finding.recommendations()
            );
        }
    }
}
