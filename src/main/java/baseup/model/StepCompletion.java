package baseup.model;

import java.time.Instant;

public record StepCompletion(String id, String userId, int step, Instant at) implements Identifiable {
    public StepCompletion {
        if (step < 1 || !id.equals(userId + ":" + step))
            throw new IllegalArgumentException("단계 완료 기록 오류");
    }
}
