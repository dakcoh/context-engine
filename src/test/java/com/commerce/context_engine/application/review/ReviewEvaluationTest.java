package com.commerce.context_engine.application.review;

import com.commerce.context_engine.core.model.ReviewRequest;
import com.commerce.context_engine.core.port.KnowledgeReviewer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ReviewEvaluationTest {

    @Autowired
    KnowledgeReviewer reviewer;

    @Test
    void representativeArtifacts_includeExpectedReviewRule() throws Exception {
        String fixture = new ClassPathResource("review-evaluation.tsv")
                .getContentAsString(StandardCharsets.UTF_8);
        var failures = new ArrayList<String>();
        int count = 0;

        for (String line : fixture.lines().filter(value -> !value.isBlank() && !value.startsWith("#")).toList()) {
            count++;
            String[] columns = line.split("\\t", 5);
            assertThat(columns).as("review fixture row: %s", line).hasSize(5);
            var report = reviewer.review(new ReviewRequest(
                    columns[3], columns[0], Set.of(columns[1]), columns[2], 5));
            var actualIds = report.findings().stream().map(finding -> finding.ruleId()).toList();
            if (!actualIds.contains(columns[4])) {
                failures.add("scenario '%s' expected %s, actual=%s".formatted(
                        columns[2], columns[4], actualIds));
            }
            assertThat(report.findings()).allSatisfy(finding -> {
                assertThat(finding.ruleStatus()).isNotNull();
                assertThat(finding.evidenceLevel()).isNotNull();
            });
        }

        assertThat(count).isEqualTo(20);
        assertThat(failures).as("review evaluation failures").isEmpty();
    }
}
