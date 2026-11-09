package baseup.model;

import java.util.Arrays;
import java.util.Objects;

/** 한 개의 학습 카드 겸 4지선다 퀴즈 문제를 나타내는 객체. */
public final class QuizQuestion {
    private final String id;
    private final String category;
    private final String imageFile;
    private final String imageGuide;
    private final String learningTerm;
    private final String learningDescription;
    private final String situationText;
    private final String question;
    private final String[] options;
    private final int answerNumber; // 1부터 4 (배열 인덱스와 다름)
    private final String explanation;

    public QuizQuestion(String id, String category, String imageFile, String imageGuide,
                        String learningTerm, String learningDescription, String situationText,
                        String question, String[] options, int answerNumber, String explanation) {
        this.id = required(id, "문제 ID").toUpperCase();
        this.category = required(category, "유형").toUpperCase();
        this.imageFile = required(imageFile, "이미지 파일명");
        this.imageGuide = clean(imageGuide);
        this.learningTerm = required(learningTerm, "학습 용어");
        this.learningDescription = required(learningDescription, "학습 설명");
        this.situationText = clean(situationText);
        this.question = required(question, "질문");
        this.options = Objects.requireNonNull(options, "보기 4개가 필요합니다.").clone();
        if (this.options.length != 4) throw new IllegalArgumentException("보기는 반드시 4개여야 합니다.");
        for (int i = 0; i < 4; i++) this.options[i] = required(this.options[i], (i + 1) + "번 보기");
        if (answerNumber < 1 || answerNumber > 4) throw new IllegalArgumentException("정답은 1~4여야 합니다.");
        if (!this.id.matches("[EMHU][0-9]{2,3}")) throw new IllegalArgumentException("문제 ID는 E01, U11 형식입니다.");
        if (!"EMHU".contains(this.category) || this.category.length() != 1 || !this.id.startsWith(this.category))
            throw new IllegalArgumentException("ID 첫 글자와 유형(E/M/H/U)이 일치해야 합니다.");
        if (!this.imageFile.matches("[a-zA-Z0-9_-]+\\.(png|jpg|jpeg|gif)"))
            throw new IllegalArgumentException("이미지는 파일명만 입력해 주세요 (예: e-1.png).");
        this.answerNumber = answerNumber;
        this.explanation = required(explanation, "정답 해설");
    }

    private static String clean(String text) {
        return text == null ? "" : text.trim();
    }
    private static String required(String text, String name) {
        String cleaned = clean(text);
        if (cleaned.isEmpty()) throw new IllegalArgumentException(name + " 항목은 비워둘 수 없습니다.");
        if (cleaned.contains("\t") || cleaned.contains("\n") || cleaned.contains("\r"))
            throw new IllegalArgumentException(name + " 항목에 탭이나 줄바꿈을 사용할 수 없습니다.");
        return cleaned;
    }

    public static QuizQuestion fromLine(String line) {
        String[] p = line.split("\t", -1);
        if (p.length != 14) throw new IllegalArgumentException("필드가 " + p.length + "개입니다. 14개여야 합니다.");
        return new QuizQuestion(p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7],
                Arrays.copyOfRange(p, 8, 12), Integer.parseInt(p[12]), p[13]);
    }

    public String toLine() {
        String[] fields = {id, category, imageFile, imageGuide, learningTerm, learningDescription,
                situationText, question, options[0], options[1], options[2], options[3],
                Integer.toString(answerNumber), explanation};
        return String.join("\t", fields);
    }
    public String getId() { return id; }
    public String getCategory() { return category; }
    public String getImageFile() { return imageFile; }
    public String getImageGuide() { return imageGuide; }
    public String getLearningTerm() { return learningTerm; }
    public String getLearningDescription() { return learningDescription; }
    public String getSituationText() { return situationText; }
    public String getQuestion() { return question; }
    public String[] getOptions() { return options.clone(); }
    public int getAnswerNumber() { return answerNumber; }
    public String getExplanation() { return explanation; }
    public String getAnswerText() { return options[answerNumber - 1]; }

    public static String categoryName(String code) {
        switch (code) {
            case "E": return "쉬움";
            case "M": return "중간";
            case "H": return "어려움";
            case "U": return "심판 판정";
            default: return "기타";
        }
    }
}
