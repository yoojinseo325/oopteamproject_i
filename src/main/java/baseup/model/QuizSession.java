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
    private String typed = "";
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
    /**
     * 단답형: 입력한 글자와, 채점 결과를 기존 정답 번호 체계로 옮긴 선택지 번호를 함께 기록한다.
     * 기록·통계는 선택지 번호로 처리하고, 해설 화면은 입력한 글자를 보여 준다.
     */
    public void answerText(String text, int mappedChoice) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("답을 입력해 주세요.");
        choose(mappedChoice);
        typed = text.strip();
    }
    /** 단답형으로 입력한 답. 객관식이면 빈 문자열. */
    public String typedAnswer() { return typed; }
    /** 입력을 모두 지웠을 때: 아직 답하지 않은 상태로 되돌린다. */
    public void clearAnswer() { require(Stage.JUDGE); choice = -1; typed = ""; }
    public void beginClue() {
        require(Stage.JUDGE);
        if (choice < 0) throw new IllegalStateException("먼저 판정을 선택해 주세요.");
        if (!current().requiresClue()) throw new IllegalStateException("이 문제는 선택한 답을 바로 제출합니다.");
        stage = Stage.CLUE;
    }
    public void chooseClue(String value) { require(Stage.CLUE); clue = value; }
    public void useHint() {
        if (stage == Stage.JUDGE || stage == Stage.CLUE) hint = true;
    }
    public void recordSaved(Attempt attempt) {
        require(current().requiresClue() ? Stage.CLUE : Stage.JUDGE);
        results.add(attempt);
        stage = Stage.FEEDBACK;
    }
    public void next() {
        require(Stage.FEEDBACK);
        if (index + 1 == size()) { stage = Stage.COMPLETE; return; }
        index++; choice = -1; clue = ""; typed = ""; hint = false;
        started = clock.instant(); stage = Stage.JUDGE;
    }
    private void require(Stage expected) {
        if (stage != expected) throw new IllegalStateException("현재 단계에서는 이 동작을 할 수 없습니다.");
    }
}
