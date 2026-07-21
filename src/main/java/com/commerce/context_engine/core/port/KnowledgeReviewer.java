package com.commerce.context_engine.core.port;

import com.commerce.context_engine.core.model.ReviewReport;
import com.commerce.context_engine.core.model.ReviewRequest;

public interface KnowledgeReviewer {
    ReviewReport review(ReviewRequest request);
}
