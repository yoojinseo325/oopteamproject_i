package baseup.service;

import baseup.model.*;
import baseup.repository.*;
import java.io.IOException;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.function.Predicate;
import static baseup.repository.Tsv.line;

/** UI가 사용하는 진입점. 저장 성공 후에만 메모리와 퀴즈 단계를 변경한다. */
public final class LearningService {
    private final Catalog catalog;
    private final Clock clock;
    private final Repository<UserProfile> profileRepository;
    private final Repository<Attempt> attemptRepository;
    private final Repository<Bookmark> bookmarkRepository;
    private final Repository<StudyNote> noteRepository;
    private final Manager<UserProfile> profiles;
    private final Manager<Attempt> attempts;
    private final Manager<Bookmark> bookmarks;
    private final Manager<StudyNote> notes;
    private final Repository<StepCompletion> completionRepository;
    private final Manager<StepCompletion> completions;
    private static final List<LearningStep> STEPS = List.of(
            new LearningStep(1, "장면에서 결정적인 단서 찾기", List.of("Q01", "Q03")),
            new LearningStep(2, "조건이 달라져도 스스로 판단하기", List.of("Q02", "Q04")),
            new LearningStep(3, "새로운 상황에서 기준 세우기", List.of("Q05", "Q07")),
            new LearningStep(4, "비슷해 보이는 상황 구별하기", List.of("Q06", "Q08")),
            new LearningStep(5, "여러 단서를 함께 살펴보기", List.of("Q09", "Q11")),
            new LearningStep(6, "조건을 종합해 판정 완성하기", List.of("Q10", "Q12")));
    private UserProfile currentUser;

    public LearningService(Catalog catalog, Path userDir) throws IOException {
        this(catalog, userDir, Clock.systemUTC());
    }
    public LearningService(Catalog catalog, Path userDir, Clock clock) throws IOException {
        this.catalog = catalog; this.clock = clock;
        profileRepository = new FileRepository<>(userDir.resolve("profiles.tsv"), "id\tnickname",
                f -> new UserProfile(f[0], f[1]), x -> line(x.id(), x.nickname()));
        attemptRepository = new FileRepository<>(userDir.resolve("attempts.tsv"),
                "id\tuserId\tquestionId\tchoice\tclue\tanswerCorrect\tclueCorrect\thintUsed\tseconds\tat",
                f -> new Attempt(f[0], f[1], f[2], Integer.parseInt(f[3]), f[4], bool(f[5]), bool(f[6]),
                        bool(f[7]), nonnegative(f[8]), Instant.parse(f[9])),
                x -> line(x.id(), x.userId(), x.questionId(), "" + x.choice(), x.clue(), "" + x.answerCorrect(),
                        "" + x.clueCorrect(), "" + x.hintUsed(), "" + x.seconds(), x.at().toString()));
        bookmarkRepository = new FileRepository<>(userDir.resolve("bookmarks.tsv"), "id\tuserId\truleId",
                f -> new Bookmark(f[0], f[1], f[2]), x -> line(x.id(), x.userId(), x.ruleId()));
        noteRepository = new FileRepository<>(userDir.resolve("notes.tsv"), "id\tuserId\truleId\ttext",
                f -> new StudyNote(f[0], f[1], f[2], f[3]), x -> line(x.id(), x.userId(), x.ruleId(), x.text()));
        profiles = new Manager<>(profileRepository.load());
        attempts = new Manager<>(attemptRepository.load());
        bookmarks = new Manager<>(bookmarkRepository.load());
        notes = new Manager<>(noteRepository.load());
        completionRepository = new FileRepository<>(userDir.resolve("steps.tsv"), "id\tuserId\tstep\tat",
                f -> new StepCompletion(f[0], f[1], Integer.parseInt(f[2]), Instant.parse(f[3])),
                x -> line(x.id(), x.userId(), "" + x.step(), x.at().toString()));
        completions = new Manager<>(completionRepository.load());
        if (profiles.all().isEmpty()) addProfile("신입 심판");
        currentUser = profiles.all().get(0);
    }
    private static boolean bool(String text) {
        if (!text.equals("true") && !text.equals("false")) throw new IllegalArgumentException("boolean 값 오류");
        return Boolean.parseBoolean(text);
    }
    private static long nonnegative(String text) {
        long n = Long.parseLong(text);
        if (n < 0) throw new IllegalArgumentException("음수 풀이 시간");
        return n;
    }
    public Catalog catalog() { return catalog; }
    public UserProfile user() { return currentUser; }
    public List<UserProfile> profiles() { return profiles.all(); }
    public void selectUser(String id) { currentUser = profiles.find(id).orElseThrow(); }
    public UserProfile addProfile(String nickname) throws IOException {
        String name = nickname.strip();
        if (name.isEmpty() || name.length() > 16 || name.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("닉네임은 줄바꿈 없이 1~16자로 입력해 주세요.");
        if (profiles.all().stream().anyMatch(p -> p.nickname().equalsIgnoreCase(name)))
            throw new IllegalArgumentException("이미 있는 닉네임입니다.");
        UserProfile created = new UserProfile(UUID.randomUUID().toString(), name);
        saveAdded(profileRepository, profiles, created);
        currentUser = created;
        return created;
    }
    public QuizSession start(QuestionSelector selector) { return new QuizSession(selector.select(this), currentUser.id(), clock); }
    public List<LearningStep> steps() { return STEPS; }
    public LearningStep step(int number) {
        if (number < 1 || number > STEPS.size()) throw new IllegalArgumentException("없는 단계입니다.");
        return STEPS.get(number - 1);
    }
    public int completedSteps() {
        int count = 0;
        for (LearningStep step : STEPS) {
            if (completions.find(currentUser.id() + ":" + step.number()).isEmpty()) break;
            count++;
        }
        return count;
    }
    public boolean stepUnlocked(int number) { step(number); return number <= completedSteps() + 1; }
    public boolean courseComplete() { return completedSteps() == STEPS.size(); }
    public boolean discovered(String ruleId) {
        return attempts().stream().anyMatch(a -> catalog.question(a.questionId()).map(q -> q.ruleId().equals(ruleId)).orElse(false));
    }
    public QuizSession startStep(int number) {
        return new QuizSession(new StepSelector(number).select(this), currentUser.id(), clock, number);
    }
    public QuizSession startNext() {
        if (!courseComplete()) return startStep(completedSteps() + 1);
        List<Question> selected = adaptiveQuestions(List.of(), 4);
        if (selected.isEmpty()) {
            selected = catalog.questions().stream()
                    .sorted(Comparator.comparing(q -> latest(q.id()).map(Attempt::at).orElse(Instant.MIN)))
                    .limit(4).toList();
        }
        return new QuizSession(selected, currentUser.id(), clock);
    }
    /** 완료 저장 실패 시 해설 화면에 머물러 재시도할 수 있다. */
    public void advance(QuizSession session) throws IOException {
        if (!session.userId().equals(currentUser.id())) throw new IllegalStateException("미션을 시작한 사용자로 돌아가 주세요.");
        if (session.stage() != QuizSession.Stage.FEEDBACK) throw new IllegalStateException("해설을 먼저 확인해 주세요.");
        if (session.index() + 1 == session.size() && session.stepNumber() > 0) {
            String id = currentUser.id() + ":" + session.stepNumber();
            if (completions.find(id).isEmpty()) saveAdded(completionRepository, completions,
                    new StepCompletion(id, currentUser.id(), session.stepNumber(), clock.instant()));
        }
        session.next();
    }
    private List<Attempt> ruleAttempts(String ruleId) {
        return attempts().stream().filter(a -> catalog.question(a.questionId()).map(q -> q.ruleId().equals(ruleId)).orElse(false)).toList();
    }
    private boolean ruleDue(String ruleId) {
        List<Attempt> history = ruleAttempts(ruleId);
        if (history.isEmpty()) return false;
        Attempt last = history.get(history.size() - 1);
        if (!last.independent()) return true;
        int streak = 0;
        for (int i = history.size() - 1; i >= 0 && history.get(i).independent(); i--) streak++;
        long days = streak >= 3 ? 7 : streak == 2 ? 3 : 1;
        return !clock.instant().isBefore(last.at().plus(Duration.ofDays(days)));
    }
    public List<Question> adaptiveQuestions(List<Question> core, int limit) {
        Set<String> coreRules = new HashSet<>();
        core.forEach(q -> coreRules.add(q.ruleId()));
        List<String> rules = catalog.rules().stream().map(RuleConcept::id)
                .filter(id -> !coreRules.contains(id) && ruleDue(id))
                .sorted(Comparator.<String, Boolean>comparing(id -> {
                    List<Attempt> h = ruleAttempts(id); return h.get(h.size() - 1).independent();
                }).thenComparing(id -> {
                    List<Attempt> h = ruleAttempts(id); return h.get(h.size() - 1).at();
                })).toList();
        List<Question> selected = new ArrayList<>();
        for (String rule : rules) {
            if (selected.size() >= limit) break;
            List<Attempt> history = ruleAttempts(rule);
            Attempt last = history.get(history.size() - 1);
            boolean struggling = history.size() >= 2 && !last.independent() && !history.get(history.size() - 2).independent();
            // 반복해서 막히면 기본 상황으로 돌아가고, 그 외에는 직전과 다른 조건을 우선한다.
            catalog.questions().stream().filter(q -> q.ruleId().equals(rule))
                    .sorted(Comparator.comparingInt((Question q) -> struggling && !q.level().equals("기본") ? 1 : 0)
                            .thenComparing(q -> q.id().equals(last.questionId())))
                    .findFirst().ifPresent(selected::add);
        }
        return List.copyOf(selected);
    }
    public Attempt submit(QuizSession session) throws IOException {
        if (!session.userId().equals(currentUser.id())) throw new IllegalStateException("미션을 시작한 사용자로 돌아가 주세요.");
        if (session.stage() != QuizSession.Stage.CLUE || session.clue().isBlank())
            throw new IllegalStateException("장면 또는 아래 버튼에서 단서를 선택해 주세요.");
        Question q = session.current();
        if (catalog.scene(q.beforeScene()).tokens().stream().noneMatch(t -> t.id().equals(session.clue())))
            throw new IllegalArgumentException("유효한 단서가 아닙니다.");
        Attempt attempt = new Attempt(UUID.randomUUID().toString(), currentUser.id(), q.id(), session.choice(),
                session.clue(), q.isAnswerCorrect(session.choice()), q.isClueCorrect(session.clue()), session.hintUsed(),
                Math.max(0, Duration.between(session.started(), clock.instant()).getSeconds()), clock.instant());
        saveAdded(attemptRepository, attempts, attempt);
        session.recordSaved(attempt);
        return attempt;
    }
    public List<Attempt> attempts() { return attempts.search(a -> a.userId().equals(currentUser.id())); }
    public Optional<Attempt> latest(String questionId) {
        // 파일 저장 순서를 이용하므로 시스템 시계가 바뀌어도 최신 제출을 찾을 수 있다.
        List<Attempt> list = attempts();
        for (int i = list.size() - 1; i >= 0; i--) if (list.get(i).questionId().equals(questionId)) return Optional.of(list.get(i));
        return Optional.empty();
    }
    public boolean needsReview(Question q) {
        return latest(q.id()).map(a -> !a.independent() || !clock.instant().isBefore(a.at().plus(Duration.ofDays(1)))).orElse(false);
    }
    public List<Question> reviewQuestions() {
        return catalog.questions().stream().filter(this::needsReview)
                .sorted(Comparator.comparing((Question q) -> latest(q.id()).orElseThrow().independent())).toList();
    }
    public int solvedCount() { return (int) catalog.questions().stream().filter(q -> latest(q.id()).map(Attempt::independent).orElse(false)).count(); }
    public int understoodCount() { return (int) attempts().stream().filter(Attempt::understood).count(); }
    public int mastery(String ruleId) {
        return (int) catalog.questions().stream().filter(q -> q.ruleId().equals(ruleId))
                .filter(q -> latest(q.id()).map(Attempt::independent).orElse(false)).count();
    }
    public long questionCount(String ruleId) { return catalog.questions().stream().filter(q -> q.ruleId().equals(ruleId)).count(); }
    public int xp() {
        // 같은 문제를 반복 클릭해도 점수를 무한히 얻지 않도록 문항별 최고 성취만 계산한다.
        int total = 0;
        for (Question q : catalog.questions()) {
            int best = attempts().stream().filter(a -> a.questionId().equals(q.id()))
                    .mapToInt(a -> a.independent() ? 30 : a.understood() ? 20 : a.answerCorrect() ? 10 : 0).max().orElse(0);
            total += best;
        }
        return total;
    }
    public String rank() { return xp() >= 300 ? "규칙 탐험가" : xp() >= 120 ? "루키 심판" : "신입 심판"; }
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
    public void addNote(String ruleId, String text) throws IOException {
        catalog.rule(ruleId);
        if (text.isBlank() || text.length() > 500) throw new IllegalArgumentException("메모는 1~500자로 입력해 주세요.");
        saveAdded(noteRepository, notes, new StudyNote(UUID.randomUUID().toString(), currentUser.id(), ruleId, text.strip()));
    }
    public void deleteNote(String id) throws IOException {
        saveRemoved(noteRepository, notes, n -> n.userId().equals(currentUser.id()) && n.id().equals(id));
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
