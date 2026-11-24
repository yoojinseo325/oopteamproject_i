package baseup.shortapp.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 40문항 공통 모델: 객관식/단답형 모두 동일 객체로 취급한다. */
public final class QuizQuestion {
    private final String id, category, imageFile, imageGuide, learningTerm, learningDescription;
    private final String situationText, question, correctAnswer, explanation, scenarioGif;
    private final List<String> acceptedAnswers;
    private final QuestionType questionType;
    private final List<String> options;
    private final int correctOptionNumber; // 단답형 0, 객관식 1~4

    /** 구버전 12컬럼 생성자도 유지해 기존 프론트 코드가 컴파일되도록 한다. */
    public QuizQuestion(String id, String category, String imageFile, String imageGuide,
                        String learningTerm, String learningDescription, String situationText,
                        String question, String correctAnswer, String aliases,
                        String explanation, String scenarioGif) {
        this(id, category, imageFile, imageGuide, learningTerm, learningDescription,
                situationText, question, correctAnswer, aliases, explanation, scenarioGif,
                QuestionType.SHORT_ANSWER, List.of());
    }

    public QuizQuestion(String id, String category, String imageFile, String imageGuide,
                        String learningTerm, String learningDescription, String situationText,
                        String question, String correctAnswer, String aliases,
                        String explanation, String scenarioGif, QuestionType questionType,
                        List<String> options) {
        this.id = required(id, "문제 ID").toUpperCase(Locale.ROOT);
        this.category = required(category, "난이도").toUpperCase(Locale.ROOT);
        if (!this.id.matches("[EMHU][0-9]{2,3}") || !this.id.startsWith(this.category))
            throw new IllegalArgumentException("유형과 문제 ID가 일치해야 합니다: " + id);
        this.imageFile = required(imageFile, "이미지");
        if (!this.imageFile.matches("[a-zA-Z0-9_-]+\\.(png|jpg|jpeg)"))
            throw new IllegalArgumentException("이미지 파일명이 올바르지 않습니다");
        this.imageGuide = clean(imageGuide);
        this.learningTerm = required(learningTerm, "학습 용어");
        this.learningDescription = required(learningDescription, "학습 설명");
        this.situationText = clean(situationText);
        this.question = required(question, "질문");
        this.correctAnswer = required(correctAnswer, "정답");
        this.explanation = required(explanation, "해설");
        this.scenarioGif = clean(scenarioGif);
        if (!this.scenarioGif.isEmpty() && !this.scenarioGif.matches("[a-zA-Z0-9_-]+\\.gif"))
            throw new IllegalArgumentException("시나리오 GIF 파일명 오류");
        this.questionType = java.util.Objects.requireNonNull(questionType, "문제 유형 없음");
        ArrayList<String> accepted = new ArrayList<>();
        accepted.add(this.correctAnswer);
        for (String alias : clean(aliases).split("\\|", -1)) {
            if (!alias.isBlank()) accepted.add(required(alias, "허용 정답"));
        }
        this.acceptedAnswers = List.copyOf(accepted);
        if (questionType == QuestionType.MULTIPLE_CHOICE) {
            if (options == null || options.size() != 4)
                throw new IllegalArgumentException("객관식에는 정확히 4개 보기가 필요합니다");
            ArrayList<String> validated = new ArrayList<>();
            Set<String> unique = new HashSet<>();
            int correctIndex = 0;
            for (int i = 0; i < options.size(); i++) {
                String option = required(options.get(i), "객관식 보기");
                if (!unique.add(option)) throw new IllegalArgumentException("중복 보기: " + option);
                validated.add(option);
                if (option.equals(correctAnswer)) correctIndex = i + 1;
            }
            if (correctIndex == 0) throw new IllegalArgumentException("정답이 4개 보기 중에 없습니다: " + id);
            this.options = List.copyOf(validated);
            this.correctOptionNumber = correctIndex;
        } else {
            if (options != null && !options.isEmpty() && options.stream().anyMatch(o -> !o.isBlank()))
                throw new IllegalArgumentException("단답형 문제에는 객관식 보기가 없어야 합니다");
            this.options = List.of();
            this.correctOptionNumber = 0;
        }
    }

    private static String clean(String s) { return s == null ? "" : s.trim(); }
    private static String required(String s, String name) {
        String v = clean(s);
        if (v.isBlank() || v.indexOf('\t') >= 0 || v.indexOf('\n') >= 0 || v.indexOf('\r') >= 0)
            throw new IllegalArgumentException(name + "에 빈 값/탭/줄바꿈을 사용할 수 없습니다");
        return v;
    }
    public static QuizQuestion fromLine(String line) {
        String[] p = line.split("\t", -1);
        if (p.length != 12 && p.length != 17)
            throw new IllegalArgumentException("문제 필드는 12개(구버전) 또는 17개(혼합형)여야 합니다: " + p.length);
        if (p.length == 12) return new QuizQuestion(p[0],p[1],p[2],p[3],p[4],p[5],p[6],p[7],p[8],p[9],p[10],p[11]);
        QuestionType type = QuestionType.parse(p[12]);
        return new QuizQuestion(p[0],p[1],p[2],p[3],p[4],p[5],p[6],p[7],p[8],p[9],p[10],p[11],
                type, type == QuestionType.MULTIPLE_CHOICE ? List.of(p[13],p[14],p[15],p[16]) : List.of());
    }
    public String toLine() {
        ArrayList<String> parts = new ArrayList<>(List.of(id,category,imageFile,imageGuide,learningTerm,
                learningDescription,situationText,question,correctAnswer,
                String.join("|",acceptedAnswers.subList(1,acceptedAnswers.size())),explanation,scenarioGif,
                questionType.name()));
        if (questionType == QuestionType.MULTIPLE_CHOICE) parts.addAll(options);
        else parts.addAll(List.of("","","",""));
        return String.join("\t", parts);
    }
    public String getId(){return id;}
    public String getCategory(){return category;}
    public String getImageFile(){return imageFile;}
    public String getImageGuide(){return imageGuide;}
    public String getLearningTerm(){return learningTerm;}
    public String getLearningDescription(){return learningDescription;}
    public String getSituationText(){return situationText;}
    public String getQuestion(){return question;}
    public String getCorrectAnswer(){return correctAnswer;}
    public String getAnswerText(){return correctAnswer;}
    public List<String> getAcceptedAnswers(){return acceptedAnswers;}
    public String getExplanation(){return explanation;}
    public String getScenarioGif(){return scenarioGif;}
    public boolean hasScenario(){return !scenarioGif.isEmpty();}
    public QuestionType getQuestionType(){return questionType;}
    public boolean isMultipleChoice(){return questionType == QuestionType.MULTIPLE_CHOICE;}
    public List<String> getOptions(){return options;}
    public int getCorrectOptionNumber(){return correctOptionNumber;}
    public static String categoryName(String code) {
        return switch(code){case "E"->"쉬움";case "M"->"중간";case "H"->"어려움";case "U"->"심판 판정";default->"기타";};
    }
}
