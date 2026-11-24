package baseup;

import baseup.model.*;
import baseup.repository.Catalog;
import baseup.service.*;
import baseup.service.StepProgressCalculator.Gauge;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** 기능 1(진행 게이지), 2(오늘의 규칙), 3(로그인 세션) 검사. JUnit 없이 실행한다. */
public final class FeatureTeamServicesTest {
    private static int checks;
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); checks++; }
    @FunctionalInterface interface Work { void run() throws Exception; }
    private static void rejects(Work work, String message) throws Exception {
        try { work.run(); } catch (IllegalArgumentException | IllegalStateException e) { checks++; return; }
        throw new AssertionError(message);
    }
    private static void answer(LearningService s, Question q) throws Exception {
        QuizSession session = s.start(x -> List.of(q));
        session.choose(q.answer());
        if (q.requiresClue()) { session.beginClue(); session.chooseClue(q.clueTarget()); }
        s.submit(session);
    }

    public static void main(String[] args) throws Exception {
        Catalog catalog = new Catalog(Path.of("data"));
        gauge();
        progress(catalog);
        dailyRule(catalog);
        authSession(catalog);
        System.out.println("PASS: " + checks + " progress/daily-rule/auth-session checks");
    }

    private static void gauge() throws Exception {
        check(new Gauge(1, 3).percent() == 33 && new Gauge(1, 3).label().equals("1 / 3"), "percent rounds down");
        check(new Gauge(5, 3).done() == 3 && new Gauge(-1, 3).done() == 0, "done clamped");
        check(new Gauge(0, 0).percent() == 0 && new Gauge(0, 0).ratio() == 0 && !new Gauge(0, 0).complete(), "empty gauge");
        check(new Gauge(4, 4).complete() && new Gauge(4, 4).percent() == 100, "full gauge");
        rejects(() -> new Gauge(0, -1), "negative total");
    }

    private static void progress(Catalog catalog) throws Exception {
        LearningService s = new LearningService(catalog, Files.createTempDirectory(Path.of("build"), "progress-test-"));
        StepProgressCalculator calc = new StepProgressCalculator(s);
        int steps = catalog.steps().size();
        check(calc.course().equals(new Gauge(0, steps)), "fresh course 0 / " + steps);
        check(calc.currentStepNumber() == 1 && calc.currentStep().done() == 0, "fresh current step");
        check(calc.rules().equals(new Gauge(0, 40)), "no rule discovered yet");

        LearningStep first = s.step(1);
        Question q = catalog.question(first.questionIds().get(0)).orElseThrow();
        QuizSession session = s.start(x -> List.of(q));
        check(StepProgressCalculator.session(session).equals(new Gauge(0, 1)), "session before submit");
        session.choose(q.answer());
        if (q.requiresClue()) { session.beginClue(); session.chooseClue(q.clueTarget()); }
        s.submit(session);
        check(StepProgressCalculator.session(session).equals(new Gauge(1, 1)), "session after submit");
        check(calc.step(1).equals(new Gauge(1, first.questionIds().size())), "step counts answered question");
        check(calc.rules().done() == 1, "rule discovered by answering");
        answer(s, q);
        check(calc.step(1).done() == 1, "same question counted once");

        QuizSession whole = s.startStep(1);
        while (whole.stage() != QuizSession.Stage.COMPLETE) {
            Question current = whole.current();
            whole.choose(current.answer());
            if (current.requiresClue()) { whole.beginClue(); whole.chooseClue(current.clueTarget()); }
            s.submit(whole);
            s.advance(whole);
        }
        check(calc.course().equals(new Gauge(1, steps)), "completed step counted");
        check(calc.step(1).complete() && calc.currentStepNumber() == 2, "next step becomes current");
    }

    private static void dailyRule(Catalog catalog) {
        Clock day = Clock.fixed(Instant.parse("2026-01-01T03:00:00Z"), ZoneOffset.UTC);
        DailyRuleService daily = new DailyRuleService(catalog, day);
        RuleConcept today = daily.today();
        check(today.equals(daily.forDate(LocalDate.of(2026, 1, 1))), "today uses clock date");
        check(today.equals(new DailyRuleService(catalog, day).today()), "same day, same rule after restart");
        List<RuleConcept> next40 = daily.upcoming(40);
        check(next40.size() == 40 && new HashSet<>(next40).size() == 40, "40 days without repeats");
        check(new HashSet<>(daily.upcoming(40)).containsAll(catalog.rules()), "every rule appears (including undiscovered)");
        Random random = new Random(7);
        for (int i = 0; i < 50; i++) check(!daily.another(today, random).equals(today), "another excludes current");
        RuleConcept only = catalog.rules().get(0);
        check(new DailyRuleService(List.of(only), day).another(only, random).equals(only), "single rule fallback");
        try { new DailyRuleService(List.of(), day); throw new AssertionError("empty rules"); }
        catch (IllegalArgumentException expected) { checks++; }
    }

    private static void authSession(Catalog catalog) throws Exception {
        Path dir = Files.createTempDirectory(Path.of("build"), "auth-test-");
        LearningService learning = new LearningService(catalog, dir);
        AuthSessionService session = new AuthSessionService(new AuthService(learning, dir), learning);
        int[] events = {0};
        session.addListener(() -> events[0]++);
        check(!session.isLoggedIn() && session.currentUser().isEmpty(), "starts logged out");
        rejects(session::requireUser, "require login");

        char[] password = "password123".toCharArray();
        UserProfile first = session.signUp("Tester01", password, "테스터");
        check(session.isLoggedIn() && session.loginId().orElse("").equals("tester01"), "sign up logs in");
        check(first.nickname().equals("테스터") && session.requireUser().equals(first), "profile linked");
        check(new String(password).chars().allMatch(c -> c == 0), "password array cleared");
        answer(learning, catalog.questions().get(0));
        check(session.myAttempts().size() == 1, "attempt saved for logged-in user");

        session.logout();
        check(!session.isLoggedIn() && events[0] == 2, "logout notifies");
        rejects(session::myAttempts, "records need login");
        rejects(() -> session.signUp("tester01", "password123".toCharArray(), "중복"), "duplicate id");

        session.signUp("tester02", "secret-pass".toCharArray(), "두번째");
        check(session.myAttempts().isEmpty(), "new user has separate records");
        rejects(() -> session.login("tester01", "wrong-pass".toCharArray()), "wrong password");
        check(session.loginId().orElse("").equals("tester02"), "failed login keeps current session");

        UserProfile again = session.login("tester01", "password123".toCharArray());
        check(again.id().equals(first.id()) && session.myAttempts().size() == 1, "login restores own records");

        LearningService reopened = new LearningService(catalog, dir);
        AuthSessionService restarted = new AuthSessionService(new AuthService(reopened, dir), reopened);
        check(!restarted.isLoggedIn(), "restart starts logged out");
        restarted.login("TESTER01", "password123".toCharArray());
        check(restarted.myAttempts().size() == 1, "accounts and records persist in files");
    }
}
