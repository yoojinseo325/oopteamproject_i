package baseup.model;

import java.time.Instant;

/** 최초 판정과 단서를 모두 저장한다. 힌트를 사용하면 독립 해결로 세지 않는다. */
public record Attempt(String id, String userId, String questionId, int choice, String clue,
                      boolean answerCorrect, boolean clueCorrect, boolean hintUsed,
                      long seconds, Instant at) implements Identifiable {
    public static final String NO_CLUE = "@not-required";
    public boolean clueRequired() { return !NO_CLUE.equals(clue); }
    public boolean understood() { return answerCorrect && (!clueRequired() || clueCorrect); }
    public boolean independent() { return understood() && !hintUsed; }
}
