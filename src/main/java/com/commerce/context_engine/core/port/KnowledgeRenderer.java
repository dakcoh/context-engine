package com.commerce.context_engine.core.port;

import com.commerce.context_engine.core.model.KnowledgeRule;
import com.commerce.context_engine.core.model.ResponseDetail;
import com.commerce.context_engine.core.model.SearchResult;

import java.util.List;

public interface KnowledgeRenderer {
    String renderRules(List<KnowledgeRule> rules, ResponseDetail detail);

    String renderSearchResults(List<SearchResult> results, ResponseDetail detail);

    String renderChecklist(List<KnowledgeRule> rules);
}
