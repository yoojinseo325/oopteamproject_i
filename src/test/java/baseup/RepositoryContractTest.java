package baseup;

import baseup.model.*;
import baseup.repository.*;
import baseup.service.*;
import java.io.IOException;
import java.nio.file.Path;
import java.time.*;
import java.util.List;

/** 파일 대신 주입한 저장소에서도 저장 실패/재시도와 사용자 격리가 유지되는지 검사한다. */
public final class RepositoryContractTest {
    private static int checks;

    private static final class MemoryRepository<T> implements Repository<T> {
        private List<T> items = List.of();
        private boolean fail;
        @Override public List<T> load() { return List.copyOf(items); }
        @Override public void save(List<T> values) throws IOException {
            if (fail) throw new IOException("test save failure");
            items = List.copyOf(values);
        }
    }

    @FunctionalInterface private interface Operation { void run() throws Exception; }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks++;
    }

    private static void fails(Operation operation) throws Exception {
        try {
            operation.run();
            throw new AssertionError("Save failure must reach caller");
        } catch (IOException expected) { checks++; }
    }

    private static void chooseCorrect(QuizSession session) {
        Question question = session.current();
        session.choose(question.answer());
        if (question.requiresClue()) {
            session.beginClue();
            session.chooseClue(question.clueTarget());
        }
    }

    public static void main(String[] args) throws Exception {
        MemoryRepository<UserProfile> profiles = new MemoryRepository<>();
        MemoryRepository<Attempt> attempts = new MemoryRepository<>();
        MemoryRepository<Bookmark> bookmarks = new MemoryRepository<>();
        MemoryRepository<StudyNote> notes = new MemoryRepository<>();
        MemoryRepository<StepCompletion> completions = new MemoryRepository<>();
        UserRepositories stores = new UserRepositories(profiles, attempts, bookmarks, notes, completions);
        Catalog catalog = new Catalog(Path.of("data"));
        Clock clock = Clock.fixed(Instant.parse("2026-11-15T00:00:00Z"), ZoneOffset.UTC);
        LearningService service = new LearningService(catalog, stores, clock);
        String firstUser = service.user().id();
        check(profiles.load().size() == 1, "Default profile uses injected storage");

        QuizSession session = service.startNext();
        chooseCorrect(session);
        QuizSession.Stage inputStage = session.stage();
        attempts.fail = true;
        fails(() -> service.submit(session));
        check(session.stage() == inputStage && session.results().isEmpty(), "Failed submission keeps editable session");
        check(service.attempts().isEmpty() && service.xp() == 0, "Failed submission does not change memory or score");
        attempts.fail = false;
        service.submit(session);
        check(attempts.load().size() == 1 && session.results().size() == 1, "Retry saves exactly once");

        while (session.index() + 1 < session.size()) {
            service.advance(session);
            chooseCorrect(session);
            service.submit(session);
        }
        LearningProgress beforeCompletion = service.progress();
        LearningStatistics firstStatistics = service.statistics();
        completions.fail = true;
        fails(() -> service.advance(session));
        check(session.stage() == QuizSession.Stage.FEEDBACK, "Failed completion keeps feedback screen");
        check(!service.stepCompleted(1) && !service.stepUnlocked(2), "Failed completion does not unlock next step");
        completions.fail = false;
        service.advance(session);
        check(session.stage() == QuizSession.Stage.COMPLETE && service.stepUnlocked(2), "Completion retry unlocks next step");
        check(completions.load().size() == 1, "Completion is stored exactly once");
        check(!beforeCompletion.stepCompleted(1), "Earlier progress snapshot stays unchanged");

        profiles.fail = true;
        fails(() -> service.addProfile("저장 실패"));
        check(service.user().id().equals(firstUser) && service.profiles().size() == 1, "Profile save failure keeps current user");
        profiles.fail = false;
        service.addProfile("두 번째 사용자");
        check(service.xp() == 0 && service.completedSteps() == 0, "Another user has independent score and progress");
        check(service.adaptiveQuestions(List.of(), 4).isEmpty(), "Another user inherits no review history");
        check(firstStatistics.xp() == session.size() * 30, "Earlier statistics snapshot remains attached to original user");
        service.selectUser(firstUser);
        check(service.xp() == firstStatistics.xp() && service.completedSteps() == 1, "Switching back restores original achievements");

        String ruleId = catalog.questions().get(0).ruleId();
        bookmarks.fail = true;
        fails(() -> service.toggleBookmark(ruleId));
        check(!service.bookmarked(ruleId), "Failed bookmark does not appear in memory");
        bookmarks.fail = false;
        service.toggleBookmark(ruleId);
        notes.fail = true;
        fails(() -> service.addNote(ruleId, "내 메모"));
        check(service.notes("").isEmpty(), "Failed note does not appear in memory");
        notes.fail = false;
        service.addNote(ruleId, "내 메모");

        LearningService restored = new LearningService(catalog, stores, clock);
        restored.selectUser(firstUser);
        check(restored.xp() == firstStatistics.xp() && restored.stepCompleted(1), "Reload through repository contract restores progress");
        check(restored.bookmarked(ruleId) && restored.notes("").size() == 1, "Reload restores bookmark and note");
        System.out.println("PASS: " + checks + " injected repository/failure/isolation checks");
    }
}
