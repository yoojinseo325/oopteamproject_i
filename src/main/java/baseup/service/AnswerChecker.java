package baseup.service;

import baseup.common.TextNormalizer;
import baseup.model.Question;
import baseup.model.QuizSession;
import baseup.repository.Catalog;
import java.util.List;

/**
 * 단답형 채점(교수님 피드백 8, 기능 7). 공백·대소문자·문장부호를 무시하고 동의어까지 허용한다.
 * 예) "중견수", "센터", "center fielder", "CF"는 모두 정답.
 *
 * 채점 결과는 기존 객관식 정답 번호로 옮겨 기록하므로, 기록·복습·통계 코드를 바꾸지 않아도 된다.
 *
 * <pre>
 * AnswerChecker checker = new AnswerChecker(service.catalog());
 * if (checker.isShortAnswer(q)) checker.apply(session, typedText); // 이후 service.submit(session)
 * </pre>
 */
public final class AnswerChecker {
    private final Catalog catalog;

    public AnswerChecker(Catalog catalog) { this.catalog = catalog; }

    public boolean isShortAnswer(Question q) { return catalog.isShortAnswer(q); }

    /** 허용 답안(첫 번째가 대표 정답). 단답형이 아니면 빈 목록. */
    public List<String> acceptedAnswers(Question q) { return catalog.shortAnswers(q.id()); }

    /** 입력이 허용 답안 중 하나와 같으면 true. 빈 입력은 항상 false. */
    public boolean isCorrect(Question q, String input) {
        return TextNormalizer.matchesAny(input, acceptedAnswers(q));
    }

    /** 채점 결과를 선택지 번호로 바꾼다. 정답이면 정답 번호, 오답이면 정답이 아닌 첫 번호. */
    public int choiceFor(Question q, String input) {
        if (isCorrect(q, input)) return q.answer();
        return q.answer() == 0 ? 1 : 0;
    }

    /** 현재 문제에 입력한 답을 기록한다. 단답형이 아니거나 입력이 비어 있으면 예외. */
    public void apply(QuizSession session, String input) {
        Question q = session.current();
        if (!isShortAnswer(q)) throw new IllegalStateException("단답형 문제가 아닙니다.");
        session.answerText(input, choiceFor(q, input));
    }
}
