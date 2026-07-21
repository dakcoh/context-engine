package com.commerce.context_engine.core.port;

import com.commerce.context_engine.core.model.KnowledgeRule;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface KnowledgeCatalog {
    List<KnowledgeRule> findAll();

    Optional<KnowledgeRule> findById(String id);

    List<KnowledgeRule> findByDomains(Set<String> domains);

    Set<String> domains();
}
