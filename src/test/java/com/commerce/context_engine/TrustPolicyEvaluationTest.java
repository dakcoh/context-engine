package com.commerce.context_engine;

import com.commerce.context_engine.core.port.KnowledgeCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TrustPolicyEvaluationTest {

    @Autowired
    KnowledgeCatalog catalog;

    @Test
    void representativeRules_exposeHonestTrustMetadata() throws Exception {
        String fixture = new ClassPathResource("trust-evaluation.tsv")
                .getContentAsString(StandardCharsets.UTF_8);
        int count = 0;

        for (String line : fixture.lines().filter(value -> !value.isBlank() && !value.startsWith("#")).toList()) {
            count++;
            String[] columns = line.split("\\t", 4);
            assertThat(columns).as("trust fixture row: %s", line).hasSize(4);
            var rule = catalog.findById(columns[0]).orElseThrow();

            assertThat(rule.governance().owner()).isEqualTo("commerce-context-maintainers");
            assertThat(rule.governance().verifiedBy()).isBlank();
            assertThat(rule.status().name().toLowerCase().replace('_', '-')).isEqualTo(columns[1]);
            assertThat(rule.evidenceLevel().name().toLowerCase().replace('_', '-')).isEqualTo(columns[2]);
            if (columns[3].equals("NONE")) {
                assertThat(rule.lastReviewedAt()).isNull();
            }
            else {
                assertThat(rule.lastReviewedAt()).hasToString(columns[3]);
            }
        }

        assertThat(count).isEqualTo(15);
    }
}
