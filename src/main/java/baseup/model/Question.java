package baseup.model;

import java.util.List;

/** 화면과 무관한 문제 데이터. 선택지는 파일 순서대로 보관한다. */
public record Question(String id, String ruleId, String title, String level,
                       String situation, String prompt, List<String> choices, int answer,
                       String cluePrompt, String clueTarget, String hint, String explanation,
                       String misconception, String beforeScene, String afterScene)
        implements Identifiable {
    public Question { choices = List.copyOf(choices); }
    public boolean isAnswerCorrect(int selected) { return answer == selected; }
    public boolean isClueCorrect(String selected) { return clueTarget.equals(selected); }
}
