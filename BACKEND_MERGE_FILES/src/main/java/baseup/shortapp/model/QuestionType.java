package baseup.shortapp.model;

/** The grading method for a question. */
public enum QuestionType {
    MULTIPLE_CHOICE, SHORT_ANSWER;

    public static QuestionType parse(String value) {
        if (value == null) throw new IllegalArgumentException("문제 유형이 없습니다");
        try { return QuestionType.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("알 수 없는 문제 유형: " + value, e); }
    }
}
