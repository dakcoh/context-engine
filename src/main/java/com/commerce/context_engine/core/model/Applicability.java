package com.commerce.context_engine.core.model;

import java.util.List;

public record Applicability(
        List<String> appliesWhen,
        List<String> doesNotApplyWhen,
        List<String> assumptions,
        String jurisdiction
) {
    public Applicability {
        appliesWhen = immutable(appliesWhen);
        doesNotApplyWhen = immutable(doesNotApplyWhen);
        assumptions = immutable(assumptions);
        jurisdiction = jurisdiction == null || jurisdiction.isBlank() ? "KR" : jurisdiction.trim();
    }

    public static Applicability defaultFor(String domain, String category) {
        return new Applicability(
                List.of(domain + " 도메인의 " + category + " 기능을 설계하거나 구현한다."),
                List.of(),
                List.of("실제 사업 정책과 외부 사업자 계약을 함께 확인한다."),
                "KR"
        );
    }

    private static List<String> immutable(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }
}
