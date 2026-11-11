package baseup.model;

import java.util.List;

/** 화면과 무관한 문제 데이터. 선택지는 파일 순서대로 보관한다. */
public record Question(String id, String ruleId, String title, String level,
                       String situation, String prompt, List<String> choices, int answer,
                       String cluePrompt, String clueTarget, String hint, String explanation,
                       String misconception, String beforeScene, String afterScene,
                       String imageFile, String imageGuide, List<Clue> clueOptions)
        implements Identifiable {
    public record Clue(String id, String text) { }
    public Question {
        choices = List.copyOf(choices);
        clueOptions = List.copyOf(clueOptions);
    }
    public Question(String id, String ruleId, String title, String level, String situation,
                    String prompt, List<String> choices, int answer, String cluePrompt,
                    String clueTarget, String hint, String explanation, String misconception,
                    String beforeScene, String afterScene) {
        this(id, ruleId, title, level, situation, prompt, choices, answer, cluePrompt, clueTarget,
                hint, explanation, misconception, beforeScene, afterScene, "", "", List.of());
    }
    public boolean hasImage() { return !imageFile.isBlank(); }
    public boolean requiresClue() { return !clueTarget.isBlank(); }
    // 용어를 맞힌 기록이 실제 판정에서 틀린 근거를 덮어쓰지 않도록 분리한다.
    public String reviewKey() { return ruleId + (requiresClue() ? ":judgment" : ":concept"); }
    public boolean isAnswerCorrect(int selected) { return answer == selected; }
    public boolean isClueCorrect(String selected) { return clueTarget.equals(selected); }
}
