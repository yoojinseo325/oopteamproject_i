package baseup.model;

import java.util.List;

/**
 * 규칙·개념 카드 한 장.
 * topic은 퀴즈 주제(TopicSelector), category는 백과사전 분류, keywords는 검색용 동의어다.
 */
public record RuleConcept(String id, String topic, String title, String summary,
                          String detail, String source, RuleCategory category,
                          List<String> keywords) implements Identifiable {
    public RuleConcept {
        keywords = List.copyOf(keywords);
    }
    @Override public String toString() { return title; }
}
