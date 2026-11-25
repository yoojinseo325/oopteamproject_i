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
    /**
     * 요약에 없는 설명만 돌려준다. 설명이 요약과 같으면 빈 문자열, 요약으로 시작하면 나머지 부분.
     * 백과사전에서 같은 문장을 두 번 보여 주지 않기 위해 쓴다(띄어쓰기 차이는 무시).
     */
    public String extraDetail() {
        String d = detail.strip(), s = summary.strip();
        if (s.isEmpty() || compact(d).equals(compact(s))) return s.isEmpty() ? d : "";
        return d.startsWith(s) ? d.substring(s.length()).strip() : d;
    }
    private static String compact(String text) { return text.replaceAll("\\s+", ""); }

    @Override public String toString() { return title; }
}
