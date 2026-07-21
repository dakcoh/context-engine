package com.commerce.context_engine.adapter.in.mcp;

import com.commerce.context_engine.core.model.ResponseDetail;
import com.commerce.context_engine.core.port.KnowledgeCatalog;
import com.commerce.context_engine.core.port.KnowledgeRenderer;
import com.commerce.context_engine.core.port.KnowledgeSearch;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KnowledgeResourceProvider {

    private final KnowledgeCatalog catalog;
    private final KnowledgeSearch search;
    private final KnowledgeRenderer renderer;

    public KnowledgeResourceProvider(KnowledgeCatalog catalog, KnowledgeSearch search, KnowledgeRenderer renderer) {
        this.catalog = catalog;
        this.search = search;
        this.renderer = renderer;
    }

    @McpResource(
            name = "commerce-rule",
            uri = "commerce://rules/{ruleId}",
            title = "Commerce knowledge rule",
            description = "ID로 단일 커머스 규칙과 근거를 읽습니다.",
            mimeType = "text/markdown"
    )
    public String rule(@McpArg(name = "ruleId", description = "knowledge rule ID", required = true) String ruleId) {
        var rule = search.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown knowledge rule: " + ruleId));
        return renderer.renderRules(List.of(rule), ResponseDetail.FULL);
    }

    @McpResource(
            name = "commerce-domain",
            uri = "commerce://domains/{domain}",
            title = "Commerce domain knowledge",
            description = "domain의 커머스 규칙 요약을 읽습니다.",
            mimeType = "text/markdown"
    )
    public String domain(@McpArg(name = "domain", description = "knowledge domain", required = true) String domain) {
        return renderer.renderRules(search.findByDomain(domain), ResponseDetail.SUMMARY);
    }

    @McpResource(
            name = "commerce-catalog",
            uri = "commerce://catalog",
            title = "Commerce knowledge catalog",
            description = "사용 가능한 domain과 rule 수를 보여줍니다.",
            mimeType = "text/markdown"
    )
    public String catalog() {
        long needsReview = catalog.findAll().stream()
                .filter(rule -> rule.status() == com.commerce.context_engine.core.model.RuleStatus.NEEDS_REVIEW)
                .count();
        long projectGuidance = catalog.findAll().stream()
                .filter(rule -> rule.evidenceLevel() == com.commerce.context_engine.core.model.EvidenceLevel.PROJECT_GUIDANCE)
                .count();
        long verified = catalog.findAll().stream()
                .filter(rule -> !rule.governance().verifiedBy().isBlank())
                .count();
        String owners = catalog.findAll().stream()
                .map(rule -> rule.governance().owner())
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.joining(", "));
        return "# Commerce Knowledge Catalog\n\n"
                + "- rules: " + catalog.findAll().size() + "\n"
                + "- domains: " + String.join(", ", catalog.domains()) + "\n"
                + "- owners: " + owners + "\n"
                + "- verified: " + verified + "\n"
                + "- project-guidance: " + projectGuidance + "\n"
                + "- needs-review: " + needsReview + "\n\n"
                + "> 프로젝트 관리 지침입니다. 고위험 결정은 최신 공식 자료로 확인하세요.";
    }
}
