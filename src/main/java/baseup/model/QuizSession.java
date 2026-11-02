package baseup.model;

import java.time.Instant;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/** 화면 재생성과 무관하게 미션 상태 및 중복 제출을 관리한다. */
public final class QuizSession {
    public enum Stage { JUDGE, CLUE, FEEDBACK, COMPLETE }
    private final List<Question> questions;
    private final String userId;
    private final Clock clock;
    private final int stepNumber;
    private final List<Attempt> results = new ArrayList<>();
    private int index;
    private int choice = -1;
    private String clue = "";
    private boolean hint;
    private Instant started;
    private Stage stage = Stage.JUDGE;

    public QuizSession(List<Question> questions) {
        this(questions, "", Clock.systemUTC());
    }
    public QuizSession(List<Question> questions, String userId, Clock clock) {
        this(questions, userId, clock, 0);
    }
    public QuizSession(List<Question> questions, String userId, Clock clock, int stepNumber) {
        if (questions.isEmpty()) throw new IllegalArgumentException("풀 수 있는 문제가 없습니다.");
        this.questions = List.copyOf(questions);
        this.userId = userId; this.clock = clock; this.started = clock.instant(); this.stepNumber = stepNumber;
    }
    public int stepNumber() { return stepNumber; }
    public String userId() { return userId; }
    public Question current() { return questions.get(index); }
    public int index() { return index; }
    public int size() { return questions.size(); }
    public int choice() { return choice; }
    public String clue() { return clue; }
    public boolean hintUsed() { return hint; }
    public Stage stage() { return stage; }
    public Instant started() { return started; }
    public List<Attempt> results() { return List.copyOf(results); }
    public void choose(int value) {
        require(Stage.JUDGE);
        if (value < 0 || value >= current().choices().size()) throw new IllegalArgumentException("판정을 선택해 주세요.");
        choice = value;
    }
    public void beginClue() {
        require(Stage.JUDGE);
        if (choice < 0) throw new IllegalStateException("먼저 판정을 선택해 주세요.");
        stage = Stage.CLUE;
    }
    public void chooseClue(String value) { require(Stage.CLUE); clue = value; }
    public void useHint() {
        if (stage == Stage.JUDGE || stage == Stage.CLUE) hint = true;
    }
    public void recordSaved(Attempt attempt) {
        require(Stage.CLUE);
        results.add(attempt);
        stage = Stage.FEEDBACK;
    }
    public void next() {
        require(Stage.FEEDBACK);
        if (index + 1 == size()) { stage = Stage.COMPLETE; return; }
        index++; choice = -1; clue = ""; hint = false;
        started = clock.instant(); stage = Stage.JUDGE;
    }
    private void require(Stage expected) {
        if (stage != expected) throw new IllegalStateException("현재 단계에서는 이 동작을 할 수 없습니다.");
    }
}
