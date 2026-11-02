package baseup.service;

import baseup.model.Question;
import java.util.ArrayList;
import java.util.List;

/** 새 상황 사이에 이전 판단을 다시 적용하는 문제를 끼워 넣는다. */
public final class StepSelector implements QuestionSelector {
    private final int step;
    public StepSelector(int step) { this.step = step; }

    @Override public List<Question> select(LearningService service) {
        if (!service.stepUnlocked(step)) throw new IllegalStateException("앞 단계를 먼저 마쳐 주세요.");
        List<Question> core = service.step(step).questionIds().stream()
                .map(id -> service.catalog().question(id).orElseThrow()).toList();
        List<Question> reviews = service.adaptiveQuestions(core, 2);
        List<Question> result = new ArrayList<>();
        result.add(core.get(0));
        result.addAll(reviews);
        result.addAll(core.subList(1, core.size()));
        return List.copyOf(result);
    }
}
