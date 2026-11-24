package baseup.service;

import baseup.model.LearningStep;
import baseup.model.Question;
import baseup.repository.Catalog;
import java.util.List;

/** 출제 전략에 필요한 조회만 제공한다. 제출/프로필/저장 변경은 노출하지 않는다. */
public interface QuestionSource {
    Catalog catalog();
    LearningStep step(int number);
    boolean stepUnlocked(int number);
    List<Question> adaptiveQuestions(List<Question> core, int limit);
    List<Question> reviewQuestions();
}
