package com.commerce.context_engine.application.review;

import com.commerce.context_engine.core.model.FindingStatus;
import com.commerce.context_engine.core.model.ResponseDetail;
import com.commerce.context_engine.core.model.ReviewFinding;
import com.commerce.context_engine.core.model.ReviewReport;
import com.commerce.context_engine.core.model.ReviewRequest;
import com.commerce.context_engine.core.model.SearchQuery;
import com.commerce.context_engine.core.model.SearchResult;
import com.commerce.context_engine.core.port.KnowledgeReviewer;
import com.commerce.context_engine.core.port.KnowledgeSearch;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;

@Service
public class DefaultKnowledgeReviewer implements KnowledgeReviewer {

    private final KnowledgeSearch search;

    public DefaultKnowledgeReviewer(KnowledgeSearch search) {
        this.search = search;
    }

    @Override
    public ReviewReport review(ReviewRequest request) {
        String queryText = request.scenario().isBlank()
                ? request.artifact()
                : request.scenario() + " " + request.artifact();
        List<SearchResult> results = search.search(new SearchQuery(
                queryText,
                request.domains(),
                java.util.Set.of(),
                java.util.Set.of(),
                request.maxFindings(),
                ResponseDetail.SUMMARY,
                false
        ));

        LinkedHashSet<String> domains = new LinkedHashSet<>();
        List<ReviewFinding> findings = java.util.stream.IntStream.range(0, results.size())
                .mapToObj(index -> finding(index, results.get(index), domains))
                .toList();
        return new ReviewReport(
                request.artifactType(),
                domains,
                findings,
                "프로젝트 규칙 기반 후보입니다. finding의 ruleStatus, evidenceLevel, owner, verifiedBy와 lastReviewedAt을 확인하고 실제 결함 확정 전에 코드 흐름, 공식 문서와 외부 사업자 계약을 확인하세요."
        );
    }

    private static ReviewFinding finding(int index, SearchResult result, LinkedHashSet<String> domains) {
        var rule = result.rule();
        domains.add(rule.domain());
        List<String> recommendations = rule.sections().entrySet().stream()
                .filter(entry -> !entry.getKey().equals("avoid-patterns")
                        && !entry.getKey().equals("failure-scenarios"))
                .flatMap(entry -> entry.getValue().stream())
                .limit(5)
                .toList();
        return new ReviewFinding(
                "review-" + (index + 1) + "-" + rule.id(),
                rule.id(),
                rule.severity(),
                FindingStatus.REVIEW_REQUIRED,
                rule.status(),
                rule.evidenceLevel(),
                rule.governance().owner(),
                rule.governance().verifiedBy().isBlank() ? null : rule.governance().verifiedBy(),
                rule.lastReviewedAt(),
                rule.title(),
                rule.summary(),
                result.matchedTerms(),
                recommendations.isEmpty() ? rule.checklist().stream().limit(5).toList() : recommendations
        );
    }
}
