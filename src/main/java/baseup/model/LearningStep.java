package baseup.model;

import java.util.List;

/** 문제 내용은 단계를 시작한 뒤에 공개한다. */
public record LearningStep(int number, String goal, List<String> questionIds) {
    public LearningStep { questionIds = List.copyOf(questionIds); }
}
