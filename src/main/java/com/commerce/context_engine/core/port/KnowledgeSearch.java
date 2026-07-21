package com.commerce.context_engine.core.port;

import com.commerce.context_engine.core.model.KnowledgeRule;
import com.commerce.context_engine.core.model.SearchQuery;
import com.commerce.context_engine.core.model.SearchResult;

import java.util.List;
import java.util.Optional;

public interface KnowledgeSearch {
    List<SearchResult> search(SearchQuery query);

    Optional<KnowledgeRule> findById(String id);

    List<KnowledgeRule> findByDomain(String domain);

    List<KnowledgeRule> findByCategory(String domain, String category);
}
