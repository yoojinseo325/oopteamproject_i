package baseup.service;

import baseup.model.Attempt;
import baseup.model.Question;
import java.util.*;

/** 제출 기록에서 독립 해결과 최고 성취 XP를 계산하는 불변 조회 객체. */
public final class LearningStatistics {
    private final List<Question> questions;
    private final LearningHistory history;

    public LearningStatistics(List<Question> questions, List<Attempt> attempts) {
        this.questions = List.copyOf(questions);
        this.history = new LearningHistory(questions, attempts);
    }

    public Optional<Attempt> latest(String questionId) { return history.latest(questionId); }
    public int solvedCount() { return (int) questions.stream().filter(q -> latest(q.id()).map(Attempt::independent).orElse(false)).count(); }

    public int understoodCount() { return (int) history.attempts().stream().filter(Attempt::understood).count(); }

    public int mastery(String ruleId) {
        return (int) questions.stream().filter(q -> q.ruleId().equals(ruleId))
                .filter(q -> latest(q.id()).map(Attempt::independent).orElse(false)).count();
    }

    public long questionCount(String ruleId) { return questions.stream().filter(q -> q.ruleId().equals(ruleId)).count(); }

    public int xp() {
        Map<String, Integer> best = new HashMap<>();
        for (Attempt attempt : history.attempts()) {
            int score = attempt.independent() ? 30 : attempt.understood() ? 20 : attempt.answerCorrect() ? 10 : 0;
            best.merge(attempt.questionId(), score, Math::max);
        }
        return questions.stream().mapToInt(q -> best.getOrDefault(q.id(), 0)).sum();
    }
    public String rank() {
        int score = xp();
        return score >= 300 ? "규칙 탐험가" : score >= 120 ? "루키 심판" : "신입 심판";
    }
}
