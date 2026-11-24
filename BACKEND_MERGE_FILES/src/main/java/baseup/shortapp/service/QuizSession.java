package baseup.shortapp.service;

import baseup.shortapp.model.QuestionType;
import baseup.shortapp.model.QuizQuestion;
import baseup.shortapp.model.QuizResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 4지선다 + 단답형을 하나의 진행 상태에서 채점. 영상형도 동일 방식으로 채점. */
public final class QuizSession {
    private final List<QuizQuestion> questions;
    private final String category;
    private final Map<String, Boolean> marked = new LinkedHashMap<>();
    private final Map<String, String> submittedAnswers = new LinkedHashMap<>();
    private int index = 0;
    private int correct = 0;

    public QuizSession(List<QuizQuestion> questions) {
        if (questions == null || questions.isEmpty()) throw new IllegalArgumentException("해당 유형의 문제가 없습니다");
        this.questions = List.copyOf(questions);
        this.category = questions.get(0).getCategory();
        for (QuizQuestion q : questions) if (!category.equals(q.getCategory()))
            throw new IllegalArgumentException("하나의 세션에는 같은 난이도만 사용할 수 있습니다");
    }
    public QuizQuestion getCurrentQuestion() {
        if (isFinished()) throw new IllegalStateException("퀴즈가 종료되었습니다");
        return questions.get(index);
    }
    /** 기존 단답형 UI 연동과 호환되는 메서드. */
    public boolean submit(String answer) {
        QuizQuestion q = getCurrentQuestion();
        if (q.getQuestionType() != QuestionType.SHORT_ANSWER)
            throw new IllegalStateException("객관식 문제에는 submitChoice(1~4)를 호출하세요");
        if (answer == null || answer.trim().isEmpty()) throw new IllegalArgumentException("정답을 입력해 주세요");
        ensureNotAnswered(q);
        boolean good = AnswerChecker.checkShortAnswer(q, answer);
        record(q, answer.strip(), good);
        return good;
    }
    public boolean submitChoice(int choiceNumber) {
        QuizQuestion q = getCurrentQuestion();
        if (q.getQuestionType() != QuestionType.MULTIPLE_CHOICE)
            throw new IllegalStateException("단답형 문제에는 submit(문자열)을 호출하세요");
        ensureNotAnswered(q);
        boolean good = AnswerChecker.checkChoice(q, choiceNumber);
        // 화면 복습에서 실제 고른 보기 내용과 번호를 보여줄 수 있게 보존
        record(q, choiceNumber + ". " + q.getOptions().get(choiceNumber - 1), good);
        return good;
    }
    private void ensureNotAnswered(QuizQuestion question) {
        if (marked.containsKey(question.getId())) throw new IllegalStateException("이미 제출한 문제입니다");
    }
    private void record(QuizQuestion question, String answer, boolean good) {
        marked.put(question.getId(), good);
        submittedAnswers.put(question.getId(), answer);
        if (good) correct++;
    }
    public void next() {
        if (isFinished() || !isCurrentAnswered()) throw new IllegalStateException("정답 제출 후 이동하세요");
        index++;
    }
    public boolean isCurrentAnswered(){return !isFinished() && marked.containsKey(getCurrentQuestion().getId());}
    public boolean isFinished(){return index >= questions.size();}
    public int getIndex(){return index;}
    public int getTotal(){return questions.size();}
    public int getCorrect(){return correct;}
    public int getProgressPercent(){return 100 * marked.size() / questions.size();}
    public String getCategory(){return category;}
    public List<QuizQuestion> getQuestions(){return questions;}
    public Map<String,String> getTypedAnswers(){return Collections.unmodifiableMap(submittedAnswers);}
    public QuizResult toResult() {
        if (!isFinished()) throw new IllegalStateException("모든 문제를 완료해야 합니다");
        List<String> wrong = new ArrayList<>();
        for (QuizQuestion q : questions) if (!marked.getOrDefault(q.getId(), false)) wrong.add(q.getId());
        return QuizResult.now(category, correct, questions.size(), wrong, submittedAnswers);
    }
}
