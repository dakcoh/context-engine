package com.commerce.context_engine.application.checklist;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ChecklistEvaluationTest {

    @Autowired
    ChecklistService checklistService;

    @Test
    void scenarioChecklists_includeExpectedRuleAndStayBounded() throws Exception {
        String fixture = new ClassPathResource("checklist-evaluation.tsv")
                .getContentAsString(StandardCharsets.UTF_8);
        var failures = new ArrayList<String>();
        int count = 0;

        for (String line : fixture.lines().filter(value -> !value.isBlank() && !value.startsWith("#")).toList()) {
            count++;
            String[] columns = line.split("\\t", 3);
            assertThat(columns).as("checklist fixture row: %s", line).hasSize(3);
            var rules = checklistService.find(columns[0], columns[1]);
            var actualIds = rules.stream().map(rule -> rule.id()).toList();
            if (!actualIds.contains(columns[2])) {
                failures.add("scenario '%s' expected %s, actual=%s".formatted(
                        columns[1], columns[2], actualIds));
            }
            assertThat(rules).hasSizeLessThanOrEqualTo(10);
            assertThat(rules).allSatisfy(rule -> assertThat(rule.evidenceLevel()).isNotNull());
        }

        assertThat(count).isEqualTo(10);
        assertThat(failures).as("checklist evaluation failures").isEmpty();
    }
}
