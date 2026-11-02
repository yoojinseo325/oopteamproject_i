package baseup.service;

import baseup.model.Question;
import java.util.List;

public final class TopicSelector implements QuestionSelector {
    private final String topic;
    public TopicSelector(String topic) { this.topic = topic; }
    @Override public List<Question> select(LearningService service) {
        return service.catalog().questions().stream()
                .filter(q -> topic.equals(service.catalog().rule(q.ruleId()).topic())).toList();
    }
}
