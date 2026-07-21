package com.commerce.context_engine.application.checklist;

import com.commerce.context_engine.core.model.KnowledgeRule;
import com.commerce.context_engine.core.port.KnowledgeSearch;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class ChecklistService {

    private final KnowledgeSearch search;

    public ChecklistService(KnowledgeSearch search) {
        this.search = search;
    }

    public List<KnowledgeRule> find(String domain, String scenario) {
        List<KnowledgeRule> domainRules = search.findByDomain(domain);
        if (scenario == null || scenario.isBlank()) {
            return domainRules.stream()
                    .filter(rule -> !rule.checklist().isEmpty())
                    .sorted(Comparator.comparing(KnowledgeRule::severity)
                            .thenComparing(KnowledgeRule::category)
                            .thenComparing(KnowledgeRule::id))
                    .limit(com.commerce.context_engine.core.model.SearchQuery.MAX_TOP_K)
                    .toList();
        }
        return search.search(com.commerce.context_engine.core.model.SearchQuery.of(
                        scenario, domain, com.commerce.context_engine.core.model.SearchQuery.MAX_TOP_K,
                        com.commerce.context_engine.core.model.ResponseDetail.SUMMARY))
                .stream().map(com.commerce.context_engine.core.model.SearchResult::rule)
                .filter(rule -> !rule.checklist().isEmpty())
                .toList();
    }
}
