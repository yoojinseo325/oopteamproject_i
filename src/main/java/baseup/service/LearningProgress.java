package baseup.service;

import baseup.model.LearningStep;
import baseup.model.StepCompletion;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 화면 순번과 영구 ID를 구분하는 사용자별 단계 진도 스냅샷. */
public final class LearningProgress {
    private final List<LearningStep> steps;
    private final Set<Integer> completedKeys;
    private final boolean allUnlocked;

    public LearningProgress(List<LearningStep> steps, List<StepCompletion> completions, String userId) {
        this(steps, completions, userId, false);
    }

    /** allUnlocked가 true면 완료 여부와 관계없이 모든 단계를 열어 둔다(관리자 계정). 완료 기록은 그대로 센다. */
    public LearningProgress(List<LearningStep> steps, List<StepCompletion> completions, String userId, boolean allUnlocked) {
        this.allUnlocked = allUnlocked;
        this.steps = List.copyOf(steps);
        completedKeys = completions.stream().filter(c -> c.userId().equals(userId))
                .map(StepCompletion::step).collect(Collectors.toUnmodifiableSet());
    }

    public LearningStep step(int number) {
        if (number < 1 || number > steps.size()) throw new IllegalArgumentException("없는 단계입니다.");
        return steps.get(number - 1);
    }

    public int completedSteps() {
        return (int) steps.stream().filter(s -> stepCompleted(s.number())).count();
    }

    public boolean stepCompleted(int number) { return completedKeys.contains(step(number).key()); }

    public int nextStepNumber() {
        return steps.stream().filter(s -> !stepCompleted(s.number())).mapToInt(LearningStep::number).findFirst().orElse(steps.size());
    }

    public boolean stepUnlocked(int number) {
        step(number); // 없는 단계는 관리자라도 예외
        return allUnlocked || stepCompleted(number) || number == nextStepNumber();
    }

    public boolean courseComplete() { return completedSteps() == steps.size(); }

}
