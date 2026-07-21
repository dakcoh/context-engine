package com.commerce.context_engine.application.search;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class QueryNormalizer {

    private static final int MAX_TERMS = 32;
    private static final Set<String> STOP_WORDS = Set.of(
            "그리고", "또는", "에서", "으로", "하는", "있는", "대한", "위한",
            "방법", "전체", "확인", "싶어", "궁금해", "해줘",
            "public", "private", "class", "void", "string", "return", "this", "new"
    );
    private static final List<String> KOREAN_PARTICLES = List.of(
            "으로부터", "에서", "에게", "처럼", "보다", "으로", "하고", "하며",
            "라고", "이라", "은", "는", "이", "가", "을", "를", "의", "에", "로", "도", "만"
    );

    public NormalizedQuery normalize(String query) {
        String normalized = normalizeText(query);
        LinkedHashSet<String> terms = Arrays.stream(normalized.split("[^\\p{L}\\p{N}@_-]+"))
                .map(String::trim)
                .map(QueryNormalizer::stripKoreanParticle)
                .filter(term -> term.length() >= 2)
                .filter(term -> !STOP_WORDS.contains(term))
                .limit(MAX_TERMS)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return new NormalizedQuery(normalized, stripSpaces(normalized), List.copyOf(terms));
    }

    static String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    static String stripSpaces(String value) {
        return value.replaceAll("\\s+", "");
    }

    private static String stripKoreanParticle(String value) {
        for (String particle : KOREAN_PARTICLES) {
            if (value.endsWith(particle) && value.length() - particle.length() >= 2) {
                return value.substring(0, value.length() - particle.length());
            }
        }
        return value;
    }

    public record NormalizedQuery(String phrase, String compactPhrase, List<String> terms) {
        public boolean isBlank() {
            return phrase.isBlank();
        }
    }
}
