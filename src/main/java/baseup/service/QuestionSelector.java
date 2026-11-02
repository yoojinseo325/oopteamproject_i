package baseup.service;

import baseup.model.Question;
import java.util.List;

/** 일반 학습과 복습의 출제 방식을 교체하는 전략 인터페이스. */
public interface QuestionSelector {
    List<Question> select(LearningService service);
}
