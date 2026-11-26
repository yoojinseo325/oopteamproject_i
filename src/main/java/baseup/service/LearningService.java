package baseup.service;

import baseup.model.*;
import baseup.common.Manager;
import baseup.repository.*;
import java.io.IOException;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.function.Predicate;

/** UI가 사용하는 진입점. 저장 성공 후에만 메모리와 퀴즈 단계를 변경한다. */
public final class LearningService implements QuestionSource {
    private final Catalog catalog;
    private final Clock clock;
    private final Repository<UserProfile> profileRepository;
    private final Repository<Attempt> attemptRepository;
    private final Repository<Bookmark> bookmarkRepository;
    private final Repository<StudyNote> noteRepository;
    private final Repository<NoteFavorite> noteFavoriteRepository;
    private final Manager<UserProfile> profiles;
    private final Manager<Attempt> attempts;
    private final Manager<Bookmark> bookmarks;
    private final Manager<StudyNote> notes;
    private final Manager<NoteFavorite> noteFavorites;
    private final Repository<StepCompletion> completionRepository;
    private final Manager<StepCompletion> completions;
    private UserProfile currentUser;
    /** 관리자 계정으로 로그인한 동안만 true. 사용자가 바뀌면 다시 false가 된다. */
    private boolean allStepsUnlocked;

    public LearningService(Catalog catalog, Path userDir) throws IOException {
        this(catalog, userDir, Clock.systemUTC());
    }
    public LearningService(Catalog catalog, Path userDir, Clock clock) throws IOException {
        this(catalog, UserRepositories.files(userDir), clock);
    }

    /** 저장 계약을 주입하므로 화면과 학습 규칙을 바꾸지 않고 저장 방식을 교체할 수 있다. */
    public LearningService(Catalog catalog, UserRepositories repositories, Clock clock) throws IOException {
        this.catalog = catalog;
        this.clock = clock;
        profileRepository = repositories.profiles();
        attemptRepository = repositories.attempts();
        bookmarkRepository = repositories.bookmarks();
        noteRepository = repositories.notes();
        completionRepository = repositories.completions();
        profiles = new Manager<>(profileRepository.load());
        attempts = new Manager<>(attemptRepository.load());
        bookmarks = new Manager<>(bookmarkRepository.load());
        notes = new Manager<>(noteRepository.load());
        noteFavoriteRepository = repositories.noteFavorites();
        noteFavorites = new Manager<>(noteFavoriteRepository.load());
        completions = new Manager<>(completionRepository.load());
        if (profiles.all().isEmpty()) addProfile("신입 심판");
        currentUser = profiles.all().get(0);
    }

    public LearningProgress progress() {
        return new LearningProgress(catalog.steps(), completions.all(), currentUser.id(), allStepsUnlocked);
    }

    /** 현재 사용자에게 모든 STEP을 열어 둘지 정한다. 관리자 로그인(AuthService)에서만 켠다. */
    public void setAllStepsUnlocked(boolean unlocked) { allStepsUnlocked = unlocked; }
    public boolean allStepsUnlocked() { return allStepsUnlocked; }

    public LearningStatistics statistics() {
        return new LearningStatistics(catalog.questions(), attempts());
    }

    private ReviewPolicy reviewPolicy() {
        return new ReviewPolicy(catalog.questions(), attempts(), clock);
    }

    public Catalog catalog() { return catalog; }
    public UserProfile user() { return currentUser; }
    public List<UserProfile> profiles() { return profiles.all(); }
    public void selectUser(String id) { currentUser = profiles.find(id).orElseThrow(); allStepsUnlocked = false; }
    public UserProfile addProfile(String nickname) throws IOException {
        String name = nickname.strip();
        if (name.isEmpty() || name.length() > 16 || name.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("닉네임은 줄바꿈 없이 1~16자로 입력해 주세요.");
        if (profiles.all().stream().anyMatch(p -> p.nickname().equalsIgnoreCase(name)))
            throw new IllegalArgumentException("이미 있는 닉네임입니다.");
        UserProfile created = new UserProfile(UUID.randomUUID().toString(), name);
        saveAdded(profileRepository, profiles, created);
        currentUser = created;
        allStepsUnlocked = false;
        return created;
    }
    public QuizSession start(QuestionSelector selector) { return new QuizSession(selector.select(this), currentUser.id(), clock); }
    public List<LearningStep> steps() { return catalog.steps(); }
    public LearningStep step(int number) { return progress().step(number); }
    public int completedSteps() { return progress().completedSteps(); }
    public boolean stepCompleted(int number) { return progress().stepCompleted(number); }
    public int nextStepNumber() { return progress().nextStepNumber(); }
    public boolean stepUnlocked(int number) { return progress().stepUnlocked(number); }
    public boolean courseComplete() { return progress().courseComplete(); }
    public boolean discovered(String ruleId) {
        return attempts().stream().anyMatch(a -> catalog.question(a.questionId()).map(q -> q.ruleId().equals(ruleId)).orElse(false));
    }
    public QuizSession startStep(int number) {
        return new QuizSession(new StepSelector(number).select(this), currentUser.id(), clock, number);
    }
    public QuizSession startNext() {
        if (!courseComplete()) return startStep(nextStepNumber());
        List<Question> selected = adaptiveQuestions(List.of(), 4);
        if (selected.isEmpty()) {
            LearningStatistics statistics = statistics();
            selected = catalog.questions().stream()
                    .sorted(Comparator.comparing(q -> statistics.latest(q.id()).map(Attempt::at).orElse(Instant.MIN)))
                    .limit(4).toList();
        }
        return new QuizSession(selected, currentUser.id(), clock);
    }
    /** 완료 저장 실패 시 해설 화면에 머물러 재시도할 수 있다. */
    public void advance(QuizSession session) throws IOException {
        if (!session.userId().equals(currentUser.id())) throw new IllegalStateException("미션을 시작한 사용자로 돌아가 주세요.");
        if (session.stage() != QuizSession.Stage.FEEDBACK) throw new IllegalStateException("해설을 먼저 확인해 주세요.");
        if (session.index() + 1 == session.size() && session.stepNumber() > 0) {
            int key = step(session.stepNumber()).key();
            String id = currentUser.id() + ":" + key;
            if (completions.find(id).isEmpty()) saveAdded(completionRepository, completions,
                    new StepCompletion(id, currentUser.id(), key, clock.instant()));
        }
        session.next();
    }
    public List<Question> adaptiveQuestions(List<Question> core, int limit) {
        LearningProgress progress = progress();
        Set<String> eligible = new HashSet<>();
        steps().stream().filter(s -> progress.stepUnlocked(s.number()))
                .forEach(s -> eligible.addAll(s.questionIds()));
        List<Attempt> history = attempts();
        history.forEach(a -> eligible.add(a.questionId()));
        return new ReviewPolicy(catalog.questions(), history, clock).adaptiveQuestions(core, limit, eligible);
    }
    public Attempt submit(QuizSession session) throws IOException {
        if (!session.userId().equals(currentUser.id())) throw new IllegalStateException("미션을 시작한 사용자로 돌아가 주세요.");
        Question q = session.current();
        QuizSession.Stage expected = q.requiresClue() ? QuizSession.Stage.CLUE : QuizSession.Stage.JUDGE;
        if (session.stage() != expected || session.choice() < 0 || (q.requiresClue() && session.clue().isBlank()))
            throw new IllegalStateException(q.requiresClue() ? "답과 단서를 선택해 주세요." : "먼저 답을 선택해 주세요.");
        if (q.requiresClue() && catalog.clues(q).stream().noneMatch(t -> t.id().equals(session.clue())))
            throw new IllegalArgumentException("유효한 단서가 아닙니다.");
        Attempt attempt = new Attempt(UUID.randomUUID().toString(), currentUser.id(), q.id(), session.choice(),
                q.requiresClue() ? session.clue() : Attempt.NO_CLUE, q.isAnswerCorrect(session.choice()),
                q.requiresClue() && q.isClueCorrect(session.clue()), session.hintUsed(),
                Math.max(0, Duration.between(session.started(), clock.instant()).getSeconds()), clock.instant());
        saveAdded(attemptRepository, attempts, attempt);
        session.recordSaved(attempt);
        return attempt;
    }
    public List<Attempt> attempts() { return attempts.search(a -> a.userId().equals(currentUser.id())); }
    public Optional<Attempt> latest(String questionId) { return statistics().latest(questionId); }
    public boolean needsReview(Question q) { return reviewPolicy().needsReview(q); }
    public List<Question> reviewQuestions() { return reviewPolicy().reviewQuestions(); }
    public int solvedCount() { return statistics().solvedCount(); }
    public int understoodCount() { return statistics().understoodCount(); }
    public int mastery(String ruleId) { return statistics().mastery(ruleId); }
    public long questionCount(String ruleId) { return statistics().questionCount(ruleId); }
    public int xp() { return statistics().xp(); }
    public String rank() { return statistics().rank(); }
    public boolean bookmarked(String ruleId) { return bookmarks.find(currentUser.id() + ":" + ruleId).isPresent(); }
    public void toggleBookmark(String ruleId) throws IOException {
        catalog.rule(ruleId);
        String id = currentUser.id() + ":" + ruleId;
        if (bookmarked(ruleId)) saveRemoved(bookmarkRepository, bookmarks, b -> b.id().equals(id));
        else saveAdded(bookmarkRepository, bookmarks, new Bookmark(id, currentUser.id(), ruleId));
    }
    public List<StudyNote> notes(String query) {
        String term = query.toLowerCase(Locale.ROOT).strip();
        return notes.search(n -> n.userId().equals(currentUser.id())
                && (n.text() + catalog.rule(n.ruleId()).title()).toLowerCase(Locale.ROOT).contains(term));
    }
    /** 마지막으로 문제를 풀었던 학습 단계. 제출 기록이 없으면 최근 완료 단계로 대체한다. */
    public Optional<LearningStep> lastStudiedStep() {
        List<Attempt> history = attempts();
        for (int i = history.size() - 1; i >= 0; i--) {
            String questionId = history.get(i).questionId();
            Optional<LearningStep> step = steps().stream()
                    .filter(s -> s.questionIds().contains(questionId)).findFirst();
            if (step.isPresent()) return step;
        }
        return completions.all().stream().filter(c -> c.userId().equals(currentUser.id()))
                .max(Comparator.comparing(StepCompletion::at))
                .flatMap(c -> steps().stream().filter(s -> s.key() == c.step()).findFirst());
    }

    /** 저장한 순서에서 가장 최근에 제출한 문제를 반환한다. */
    public Optional<Attempt> lastAttempt() {
        List<Attempt> history = attempts();
        return history.isEmpty() ? Optional.empty() : Optional.of(history.get(history.size() - 1));
    }

    public boolean noteFavorited(String noteId) {
        return noteFavorites.find(currentUser.id() + ":" + noteId).isPresent();
    }

    public List<StudyNote> favoriteNotes() {
        return notes("").stream().filter(n -> noteFavorited(n.id())).toList();
    }

    private StudyNote ownedNote(String noteId) {
        return notes.find(noteId).filter(n -> n.userId().equals(currentUser.id()))
                .orElseThrow(() -> new IllegalArgumentException("현재 사용자의 메모가 아닙니다."));
    }

    public void toggleNoteFavorite(String noteId) throws IOException {
        ownedNote(noteId);
        String id = currentUser.id() + ":" + noteId;
        if (noteFavorited(noteId))
            saveRemoved(noteFavoriteRepository, noteFavorites, f -> f.id().equals(id));
        else
            saveAdded(noteFavoriteRepository, noteFavorites,
                    new NoteFavorite(id, currentUser.id(), noteId));
    }

    /** 기존 ID/규칙/소유자를 유지하고 본문만 변경한다. 저장 실패 시 메모리 상태는 유지된다. */
    public void updateNote(String noteId, String text) throws IOException {
        StudyNote previous = ownedNote(noteId);
        String clean = validatedNoteText(text);
        StudyNote updated = new StudyNote(previous.id(), previous.userId(), previous.ruleId(), clean);
        List<StudyNote> copy = new ArrayList<>(notes.all());
        for (int i = 0; i < copy.size(); i++)
            if (copy.get(i).id().equals(noteId)) { copy.set(i, updated); break; }
        noteRepository.save(copy);
        notes.replace(updated);
    }

    private static String validatedNoteText(String text) {
        if (text == null || text.isBlank() || text.length() > 500)
            throw new IllegalArgumentException("메모는 1~500자로 입력해 주세요.");
        return text.strip();
    }

    public void addNote(String ruleId, String text) throws IOException {
        catalog.rule(ruleId);
        saveAdded(noteRepository, notes,
                new StudyNote(UUID.randomUUID().toString(), currentUser.id(), ruleId, validatedNoteText(text)));
    }
    public void deleteNote(String id) throws IOException {
        ownedNote(id);
        saveRemoved(noteRepository, notes, n -> n.userId().equals(currentUser.id()) && n.id().equals(id));
        // 삭제 후 남을 수 있는 즐겨찾기 표식도 정리한다.
        saveRemoved(noteFavoriteRepository, noteFavorites,
                f -> f.userId().equals(currentUser.id()) && f.noteId().equals(id));
    }
    private static <T extends Identifiable> void saveAdded(Repository<T> repo, Manager<T> manager, T item) throws IOException {
        List<T> copy = new ArrayList<>(manager.all()); copy.add(item);
        repo.save(copy); manager.add(item);
    }
    private static <T extends Identifiable> void saveRemoved(Repository<T> repo, Manager<T> manager, Predicate<T> predicate) throws IOException {
        List<T> copy = manager.all().stream().filter(predicate.negate()).toList();
        repo.save(copy);
        manager.all().stream().filter(predicate).forEach(x -> manager.remove(x.id()));
    }
}
