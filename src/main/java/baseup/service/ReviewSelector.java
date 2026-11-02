package baseup.service;

import baseup.model.Question;
import java.util.List;

public final class ReviewSelector implements QuestionSelector {
    @Override public List<Question> select(LearningService service) {
        return service.reviewQuestions().stream().limit(4).toList();
    }
}
