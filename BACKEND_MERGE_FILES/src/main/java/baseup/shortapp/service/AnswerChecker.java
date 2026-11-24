package baseup.shortapp.service;

import baseup.shortapp.model.QuestionType;
import baseup.shortapp.model.QuizQuestion;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;

/** 혼합 퀴즈의 정답 비교만 담당한다. 정답 문자열/정답 번호는 문제 객체에서 얻는다. */
public final class AnswerChecker {
    private AnswerChecker() { }

    public static boolean check(QuizQuestion question, String typed) {
        return checkShortAnswer(question, typed);
    }
    public static boolean checkShortAnswer(QuizQuestion question, String typed) {
        if (question == null || question.getQuestionType() != QuestionType.SHORT_ANSWER || typed == null) return false;
        String input = normalize(typed);
        if (input.isEmpty()) return false;
        for (String candidate : question.getAcceptedAnswers()) {
            String expected = normalize(candidate);
            if (input.equals(expected)) return true;
            if (input.matches("[0-9]+(?:\\.[0-9]+)?") && expected.matches("[0-9]+(?:\\.[0-9]+)?")) {
                try { if (new BigDecimal(input).compareTo(new BigDecimal(expected)) == 0) return true; }
                catch (NumberFormatException ignored) { }
            }
        }
        return false;
    }
    public static boolean checkChoice(QuizQuestion question, int selectedNumber) {
        if (question == null || question.getQuestionType() != QuestionType.MULTIPLE_CHOICE) return false;
        if (selectedNumber < 1 || selectedNumber > question.getOptions().size())
            throw new IllegalArgumentException("보기 번호는 1~4 사이여야 합니다");
        return selectedNumber == question.getCorrectOptionNumber();
    }
    public static String normalize(String s) {
        if (s == null) return "";
        String value = Normalizer.normalize(s, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).trim();
        value = value.replaceAll("[\\s·,→/\\-:;!?()\\[\\]{}\\\"\'~]+", "");
        if (value.matches("약[0-9]+(?:\\.[0-9]+)?")) value = value.substring(1);
        return value;
    }
}
