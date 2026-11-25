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

    /** 제출 수와 그중 답이 맞은 수. 제출이 없으면 percent()는 0이다. */
    public record Accuracy(int correct, int total) {
        public int wrong() { return total - correct; }
        public int percent() { return total == 0 ? 0 : (int) Math.round(100.0 * correct / total); }
        public String label() { return total == 0 ? "기록 없음" : percent() + "%  (" + correct + "/" + total + ")"; }
    }

    /** 난이도 표시 순서. 목록에 없는 난이도는 뒤에 붙는다. */
    private static final List<String> LEVEL_ORDER = List.of("입문", "기본", "심화", "응용");

    /** 모든 제출의 정답·오답 누적(다시 푼 문제도 한 번씩 센다). */
    public Accuracy accuracy() {
        List<Attempt> all = history.attempts();
        return new Accuracy((int) all.stream().filter(Attempt::answerCorrect).count(), all.size());
    }

    /** 난이도별 정답률. 문제가 있는 난이도는 제출이 없어도 "기록 없음"으로 들어간다. */
    public Map<String, Accuracy> accuracyByLevel() {
        Map<String, String> levelOf = new HashMap<>();
        questions.forEach(q -> levelOf.put(q.id(), q.level()));
        List<String> levels = new ArrayList<>(LEVEL_ORDER);
        questions.stream().map(Question::level).distinct().filter(l -> !levels.contains(l)).forEach(levels::add);
        Map<String, int[]> counts = new LinkedHashMap<>();
        for (String level : levels) if (levelOf.containsValue(level)) counts.put(level, new int[2]);
        for (Attempt a : history.attempts()) {
            int[] slot = counts.get(levelOf.get(a.questionId()));
            if (slot == null) continue;
            slot[1]++;
            if (a.answerCorrect()) slot[0]++;
        }
        Map<String, Accuracy> result = new LinkedHashMap<>();
        counts.forEach((level, c) -> result.put(level, new Accuracy(c[0], c[1])));
        return Collections.unmodifiableMap(result);
    }

    /** 최근 제출 n개의 정답률(최근 실력 흐름). */
    public Accuracy recentAccuracy(int n) {
        List<Attempt> recent = history.attempts().stream()
                .sorted(Comparator.comparing(Attempt::at).reversed()).limit(Math.max(0, n)).toList();
        return new Accuracy((int) recent.stream().filter(Attempt::answerCorrect).count(), recent.size());
    }

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
