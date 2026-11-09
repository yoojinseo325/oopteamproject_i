package baseup.service;

import baseup.model.QuizQuestion;
import baseup.model.QuizResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 문제 진행, 중복 제출 방지, 정답 판정, 점수 계산. GUI와 독립적인 로직. */
public final class QuizSession {
    private final List<QuizQuestion> questions;
    private final String category;
    private final Map<String, Integer> selections = new LinkedHashMap<>();
    private int index = 0;
    private int correct = 0;

    public QuizSession(List<QuizQuestion> questions) {
        if (questions.isEmpty()) throw new IllegalArgumentException("해당 유형의 문제가 없습니다.");
        this.questions = new ArrayList<>(questions);
        this.category = questions.get(0).getCategory();
    }
    public QuizQuestion getCurrentQuestion() {
        if (isFinished()) throw new IllegalStateException("이미 모든 문제를 풀었습니다.");
        return questions.get(index);
    }
    public boolean submit(int answerNumber) {
        if (answerNumber < 1 || answerNumber > 4) throw new IllegalArgumentException("답은 1~4 중에서 골라야 합니다.");
        QuizQuestion current = getCurrentQuestion();
        if (selections.containsKey(current.getId())) throw new IllegalStateException("이미 제출한 문제입니다.");
        selections.put(current.getId(), answerNumber);
        boolean right = current.getAnswerNumber() == answerNumber;
        if (right) correct++;
        return right;
    }
    public void next() {
        if (isFinished()) throw new IllegalStateException("이미 모든 문제를 풀었습니다.");
        if (!selections.containsKey(getCurrentQuestion().getId()))
            throw new IllegalStateException("정답을 제출한 뒤 다음으로 이동할 수 있습니다.");
        index++;
    }
    public boolean isCurrentAnswered() {
        return !isFinished() && selections.containsKey(getCurrentQuestion().getId());
    }
    public boolean isFinished() { return index >= questions.size(); }
    public int getIndex() { return index; }
    public int getTotal() { return questions.size(); }
    public int getCorrect() { return correct; }
    public String getCategory() { return category; }
    public List<QuizQuestion> getQuestions() { return Collections.unmodifiableList(questions); }
    public Map<String, Integer> getSelections() { return Collections.unmodifiableMap(selections); }
    public QuizResult toResult() {
        if (!isFinished()) throw new IllegalStateException("퀴즈를 모두 끝낸 뒤 결과를 만들 수 있습니다.");
        List<String> wrong = new ArrayList<>();
        for (QuizQuestion q : questions) if (selections.get(q.getId()) != q.getAnswerNumber()) wrong.add(q.getId());
        return QuizResult.now(category, correct, questions.size(), wrong);
    }
}
