package baseup.service;

import baseup.model.Attempt;
import baseup.model.Question;
import java.time.Clock;
import java.time.Duration;
import java.util.*;

/** 오답 우선순위, 간격 반복, 대체 문항 선택을 담당한다. 저장/화면에는 의존하지 않는다. */
public final class ReviewPolicy {
    private final List<Question> questions;
    private final LearningHistory history;
    private final Clock clock;

    public ReviewPolicy(List<Question> questions, List<Attempt> attempts, Clock clock) {
        this.questions = List.copyOf(questions);
        this.history = new LearningHistory(questions, attempts);
        this.clock = clock;
    }

    private List<Attempt> ruleAttempts(String key) {
        return history.forRule(key);
    }

    private boolean ruleDue(String ruleId) {
        List<Attempt> history = ruleAttempts(ruleId);
        if (history.isEmpty()) return false;
        Attempt last = history.get(history.size() - 1);
        if (!last.independent()) return true;
        int streak = 0;
        for (int i = history.size() - 1; i >= 0 && history.get(i).independent(); i--) streak++;
        long days = streak >= 3 ? 7 : streak == 2 ? 3 : 1;
        return !clock.instant().isBefore(last.at().plus(Duration.ofDays(days)));
    }

    public List<Question> adaptiveQuestions(List<Question> core, int limit, Set<String> eligible) {
        Set<String> coreRules = new HashSet<>();
        core.forEach(q -> coreRules.add(q.reviewKey()));
        List<String> rules = questions.stream().map(Question::reviewKey).distinct()
                .filter(id -> !coreRules.contains(id) && ruleDue(id))
                .sorted(Comparator.<String, Boolean>comparing(id -> {
                    List<Attempt> h = ruleAttempts(id); return h.get(h.size() - 1).independent();
                }).thenComparing(id -> {
                    List<Attempt> h = ruleAttempts(id); return h.get(h.size() - 1).at();
                })).toList();
        List<Question> selected = new ArrayList<>();
        for (String rule : rules) {
            if (selected.size() >= limit) break;
            List<Attempt> history = ruleAttempts(rule);
            Attempt last = history.get(history.size() - 1);
            boolean struggling = history.size() >= 2 && !last.independent() && !history.get(history.size() - 2).independent();
            // 반복해서 막히면 기본 상황으로 돌아가고, 그 외에는 직전과 다른 조건을 우선한다.
            questions.stream().filter(q -> q.reviewKey().equals(rule) && eligible.contains(q.id()))
                    .sorted(Comparator.comparingInt((Question q) -> struggling && !q.level().equals("기본") ? 1 : 0)
                            .thenComparing(q -> q.id().equals(last.questionId())))
                    .findFirst().ifPresent(selected::add);
        }
        return List.copyOf(selected);
    }

    public boolean needsReview(Question q) {
        return history.latest(q.id()).map(a -> !a.independent() || !clock.instant().isBefore(a.at().plus(Duration.ofDays(1)))).orElse(false);
    }

    public List<Question> reviewQuestions() {
        return questions.stream().filter(this::needsReview)
                .sorted(Comparator.comparing((Question q) -> history.latest(q.id()).orElseThrow().independent())).toList();
    }
}
