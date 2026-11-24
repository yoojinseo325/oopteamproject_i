package baseup.service;

import baseup.model.Attempt;
import baseup.model.Question;
import java.util.*;

/** 한 사용자의 현재 제출 순서와 문항/복습 단위별 이력 조회. */
final class LearningHistory {
    private final List<Attempt> attempts;
    private final Map<String, Attempt> latest = new HashMap<>();
    private final Map<String, List<Attempt>> byRule = new HashMap<>();

    LearningHistory(List<Question> questions, List<Attempt> attempts) {
        this.attempts = List.copyOf(attempts);
        Map<String, Question> byId = new HashMap<>();
        questions.forEach(q -> byId.put(q.id(), q));
        for (Attempt attempt : attempts) {
            // 파일 순서를 유지한다. 시스템 시계 변경이 최신 제출을 바꾸지 않는다.
            latest.put(attempt.questionId(), attempt);
            Question question = byId.get(attempt.questionId());
            if (question != null) {
                byRule.computeIfAbsent(question.reviewKey(), key -> new ArrayList<>()).add(attempt);
            }
        }
    }

    List<Attempt> attempts() { return attempts; }
    Optional<Attempt> latest(String questionId) { return Optional.ofNullable(latest.get(questionId)); }
    List<Attempt> forRule(String key) { return List.copyOf(byRule.getOrDefault(key, List.of())); }
}
