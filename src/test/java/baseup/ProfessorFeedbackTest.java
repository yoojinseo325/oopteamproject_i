package baseup;

import baseup.model.*;
import baseup.repository.Catalog;
import baseup.service.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** 교수님 피드백 8(단답형)과 9(시나리오 재생)의 로직 검사. JUnit 없이 실행한다. */
public final class ProfessorFeedbackTest {
    private static int checks;
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); checks++; }

    public static void main(String[] args) throws Exception {
        Catalog catalog = new Catalog(Path.of("data"));
        shortAnswer(catalog);
        rejectsAmbiguousAnswers();
        scenario(catalog);
        System.out.println("PASS: " + checks + " short-answer/scenario checks");
    }

    private static void shortAnswer(Catalog catalog) throws Exception {
        AnswerChecker checker = new AnswerChecker(catalog);
        List<String> ids = catalog.questions().stream().filter(checker::isShortAnswer).map(Question::id).toList();
        check(ids.equals(List.of("E02", "E04", "E05", "E08", "E09", "M03", "M04", "M05", "H03", "H05", "H09", "H10")),
                "twelve short-answer questions: " + ids);
        // 숫자 답은 값으로 비교한다(팀원 백엔드에서 가져온 규칙).
        Question average = catalog.question("M03").orElseThrow(), ops = catalog.question("M04").orElseThrow();
        for (String ok : List.of("0.300", "0.3", ".3", "0.30", " 0.300 ", "약 0.3", "3할", "０.３"))
            check(checker.isCorrect(average, ok), "numeric accepted: " + ok);
        for (String wrong : List.of("0.250", "03", "0 3", "3", "0.3할", "300"))
            check(!checker.isCorrect(average, wrong), "numeric rejected: " + wrong);
        check(checker.isCorrect(ops, "0.84") && !checker.isCorrect(ops, "0.48"), "OPS value");
        check(checker.isCorrect(catalog.question("H03").orElseThrow(), "5와 3분의 2이닝")
                && !checker.isCorrect(catalog.question("H03").orElseThrow(), "5.2"), "innings notation");
        check(checker.isCorrect(catalog.question("H09").orElseThrow(), "불인정"), "short phrase");
        Question center = catalog.question("E09").orElseThrow();
        for (String ok : List.of("중견수", " 중 견 수 ", "센터", "Center Fielder", "cf", "센터필더"))
            check(checker.isCorrect(center, ok), "accepted: " + ok);
        for (String wrong : List.of("유격수", "", "   ", "중견", "중견수다"))
            check(!checker.isCorrect(center, wrong), "rejected: " + wrong);
        check(checker.choiceFor(center, "센터") == center.answer(), "correct maps to answer index");
        check(checker.choiceFor(center, "유격수") != center.answer(), "wrong maps to other index");
        check(!checker.isShortAnswer(catalog.question("E01").orElseThrow()), "multiple choice stays");
        check(checker.isCorrect(catalog.question("M05").orElseThrow(), "dh"), "DH lower case");
        check(checker.isCorrect(catalog.question("H10").orElseThrow(), "사이클링히트"), "spacing ignored");

        LearningService service = new LearningService(catalog, Files.createTempDirectory(Path.of("build"), "short-answer-test-"));
        QuizSession session = service.start(x -> List.of(center));
        try { checker.apply(session, "  "); throw new AssertionError("blank answer"); }
        catch (IllegalArgumentException expected) { checks++; }
        checker.apply(session, "유격수");
        check(session.typedAnswer().equals("유격수"), "typed text kept");
        session.clearAnswer();
        check(session.choice() < 0 && session.typedAnswer().isEmpty(), "clearing input resets answer");
        checker.apply(session, " Center Fielder ");
        Attempt right = service.submit(session);
        check(right.answerCorrect() && right.understood(), "typed synonym is recorded correct");
        check(session.typedAnswer().equals("Center Fielder"), "typed text trimmed");

        QuizSession wrong = service.start(x -> List.of(center));
        checker.apply(wrong, "유격수");
        check(!service.submit(wrong).answerCorrect(), "wrong typed answer is recorded wrong");
        try { checker.apply(service.start(x -> List.of(catalog.question("E01").orElseThrow())), "9명"); throw new AssertionError("not short answer"); }
        catch (IllegalStateException expected) { checks++; }
    }

    private static void rejectsAmbiguousAnswers() throws Exception {
        Path temp = Files.createTempDirectory(Path.of("build"), "short-answer-data-");
        try (var paths = Files.walk(Path.of("data"))) {
            for (Path path : paths.toList()) {
                Path target = temp.resolve(Path.of("data").relativize(path).toString());
                if (Files.isDirectory(path)) Files.createDirectories(target); else Files.copy(path, target);
            }
        }
        Path file = temp.resolve("short_answers.tsv");
        // E02 오답 선택지인 "세트"를 허용하면 채점이 모호해지므로 시작할 때 막아야 한다.
        Files.writeString(file, "id\tanswers\nE02\t이닝|세트\n", StandardCharsets.UTF_8);
        try { new Catalog(temp); throw new AssertionError("wrong choice accepted"); }
        catch (IOException expected) { check(expected.getMessage().contains("E02"), "names the question"); }
        Files.writeString(file, "id\tanswers\nE02\tinning\n", StandardCharsets.UTF_8);
        try { new Catalog(temp); throw new AssertionError("missing correct choice"); }
        catch (IOException expected) { checks++; }
        // 오답 보기 0.250과 값이 같은 답(0.25)도 모호하므로 막는다.
        Files.writeString(file, "id\tanswers\nM03\t0.300|0.25\n", StandardCharsets.UTF_8);
        try { new Catalog(temp); throw new AssertionError("numeric wrong choice accepted"); }
        catch (IOException expected) { check(expected.getMessage().contains("M03"), "numeric ambiguity names the question"); }
        Files.delete(file);
        Catalog withoutFile = new Catalog(temp);
        check(withoutFile.questions().stream().noneMatch(withoutFile::isShortAnswer), "file is optional");
    }

    private static void scenario(Catalog catalog) {
        Question q = catalog.question("Q01").orElseThrow();
        Scene a = catalog.scene(q.beforeScene()), b = catalog.scene(q.afterScene());
        check(Scene.between(a, b, 0).tokens().equals(a.tokens()), "start equals scene 1");
        check(Scene.between(a, b, 1).tokens().stream().map(Scene.Token::id).toList().equals(
                mergedIds(a, b)) && positionsMatch(Scene.between(a, b, 1), b), "end equals scene 2 positions");
        check(Scene.between(a, b, -3).tokens().equals(a.tokens()) && positionsMatch(Scene.between(a, b, 9), b), "progress clamped");
        check(Scene.between(a, b, .2).caption().equals(a.caption()) && Scene.between(a, b, .8).caption().equals(b.caption()), "caption switches midway");
        Scene.Token from = new Scene.Token("r", "주자", "runner", 100, 100), to = new Scene.Token("r", "주자", "runner", 300, 200);
        Scene.Token only1 = new Scene.Token("x", "공", "ball", 50, 50), only2 = new Scene.Token("y", "!", "fact", 60, 60);
        Scene s1 = new Scene("s1", "c1", List.of(from, only1)), s2 = new Scene("s2", "c2", List.of(to, only2));
        Scene.Token mid = Scene.between(s1, s2, .5).tokens().get(0);
        check(mid.x() == 200 && mid.y() == 150, "halfway position");
        Scene.Token early = Scene.between(s1, s2, .25).tokens().get(0);
        check(early.x() > 100 && early.x() < 150, "eases in slowly");
        check(ids(Scene.between(s1, s2, .4)).equals(List.of("r", "x")) && ids(Scene.between(s1, s2, .6)).equals(List.of("r", "y")), "one-sided tokens swap midway");
        int animated = 0;
        for (Question each : catalog.questions())
            if (!each.hasImage() && !catalog.scene(each.beforeScene()).tokens().equals(catalog.scene(each.afterScene()).tokens())) animated++;
        check(animated >= 4, "at least 4 scenario questions move: " + animated);
    }

    private static List<String> ids(Scene s) { return s.tokens().stream().map(Scene.Token::id).toList(); }
    private static List<String> mergedIds(Scene a, Scene b) {
        List<String> result = new ArrayList<>();
        for (Scene.Token t : a.tokens()) if (b.tokens().stream().anyMatch(x -> x.id().equals(t.id()))) result.add(t.id());
        for (Scene.Token t : b.tokens()) if (a.tokens().stream().noneMatch(x -> x.id().equals(t.id()))) result.add(t.id());
        return result;
    }
    private static boolean positionsMatch(Scene actual, Scene expected) {
        for (Scene.Token t : actual.tokens()) {
            Scene.Token e = expected.tokens().stream().filter(x -> x.id().equals(t.id())).findFirst().orElse(null);
            if (e == null || e.x() != t.x() || e.y() != t.y()) return false;
        }
        return actual.tokens().size() == expected.tokens().size();
    }
}
