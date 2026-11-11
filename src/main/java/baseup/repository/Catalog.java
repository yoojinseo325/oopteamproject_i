package baseup.repository;

import baseup.model.*;
import baseup.service.Manager;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** 시작 시 콘텐츠 전체와 연결 ID를 검사한다. 잘못된 콘텐츠로 채점하지 않는다. */
public final class Catalog {
    private final Manager<RuleConcept> rules;
    private final Manager<Question> questions;
    private final Manager<Scene> scenes;
    private final Manager<Team> teams;
    private final Manager<Player> players;
    private final Manager<Matchup> matchups;
    private final Path data;
    private final List<LearningStep> steps;
    public Catalog(Path data) throws IOException {
        this.data = data.toAbsolutePath().normalize();
        for (String name : List.of("rules.tsv", "questions.tsv", "scenes.tsv", "teams.tsv", "players.tsv", "matchups.tsv", "image_questions.tsv", "steps.tsv"))
            if (!Files.isRegularFile(data.resolve(name))) throw new IOException("필수 파일이 없습니다: " + data.resolve(name));
        teams = new Manager<>(new FileRepository<Team>(data.resolve("teams.tsv"), "id\tname\tshortName\tcolor\taccent",
                f -> new Team(f[0], f[1], f[2], rgb(f[3]), rgb(f[4])), x -> "").load());
        players = new Manager<>(new FileRepository<Player>(data.resolve("players.tsv"), "id\tname\tteamId",
                f -> new Player(f[0], f[1], teams.find(f[2]).orElseThrow(() -> new IllegalArgumentException("선수의 구단 ID 오류"))), x -> "").load());
        matchups = new Manager<>(new FileRepository<Matchup>(data.resolve("matchups.tsv"), "id\toffense\tdefense",
                f -> new Matchup(f[0], teams.find(f[1]).orElseThrow(), teams.find(f[2]).orElseThrow()), x -> "").load());
        rules = new Manager<>(new FileRepository<RuleConcept>(data.resolve("rules.tsv"),
                "id\ttopic\ttitle\tsummary\tdetail\tsource",
                f -> new RuleConcept(f[0], f[1], f[2], f[3], f[4], f[5]), x -> "").load());
        questions = new Manager<>(new FileRepository<Question>(data.resolve("questions.tsv"),
                "id\truleId\ttitle\tlevel\tsituation\tprompt\tchoices\tanswer\tcluePrompt\tclueTarget\thint\texplanation\tmisconception\tbeforeScene\tafterScene",
                f -> new Question(f[0], f[1], f[2], f[3], f[4], f[5], List.of(f[6].split("\\|", -1)),
                        Integer.parseInt(f[7]), f[8], f[9], f[10], f[11], f[12], f[13], f[14]), x -> "").load());
        scenes = new Manager<>(new FileRepository<Scene>(data.resolve("scenes.tsv"), "id\tcaption\ttokens", f -> {
            List<Scene.Token> tokens = new ArrayList<>();
            for (String token : f[2].split("\\|")) {
                String[] v = token.split(",", -1);
                if (v.length != 5 && v.length != 6) throw new IllegalArgumentException("장면 토큰은 5개 또는 6개 항목이 필요합니다.");
                Player player = v.length == 6 && !v[5].isBlank() ? players.find(v[5])
                        .orElseThrow(() -> new IllegalArgumentException("장면의 선수 ID 오류")) : null;
                tokens.add(new Scene.Token(v[0], v[1], v[2], Integer.parseInt(v[3]), Integer.parseInt(v[4]), player));
            }
            return new Scene(f[0], f[1], tokens);
        }, x -> "").load());
        for (Question q : new FileRepository<Question>(data.resolve("image_questions.tsv"),
                "id\truleId\ttitle\tlevel\tsituation\tprompt\tchoices\tanswer\tcluePrompt\tclueTarget\thint\texplanation\tmisconception\timageFile\timageGuide\tclueChoices",
                f -> new Question(f[0], f[1], f[2], f[3], f[4], f[5], List.of(f[6].split("\\|", -1)),
                        Integer.parseInt(f[7]), f[8], f[9], f[10], f[11], f[12], "", "", f[13], f[14],
                        f[15].isBlank() ? List.of() : Arrays.stream(f[15].split("\\|", -1)).map(value -> {
                            String[] pair = value.split(":", 2);
                            return new Question.Clue(pair[0], pair[1]);
                        }).toList()), x -> "").load()) {
            if (questions.find(q.id()).isPresent()) throw new IOException("중복 문제 ID: " + q.id());
            questions.add(q);
        }
        List<LearningStep> loaded = new ArrayList<>();
        // 파일 행 순서가 화면의 STEP 번호, key는 기존 학습 기록을 연결하는 영구 ID다.
        for (StepRow row : new FileRepository<StepRow>(data.resolve("steps.tsv"), "id\tgoal\tquestionIds",
                f -> new StepRow(f[0], f[1], List.of(f[2].split("\\|", -1))), x -> "").load())
            loaded.add(new LearningStep(loaded.size() + 1, Integer.parseInt(row.id()), row.goal(), row.questions()));
        steps = List.copyOf(loaded);
        validate();
    }
    private record StepRow(String id, String goal, List<String> questions) implements Identifiable { }
    private static int rgb(String text) {
        if (!text.matches("#[0-9a-fA-F]{6}")) throw new IllegalArgumentException("색상은 #RRGGBB 형식이어야 합니다.");
        return Integer.parseInt(text.substring(1),16);
    }
    private void validate() throws IOException {
        if (questions.all().isEmpty() || rules.all().isEmpty()) throw new IOException("규칙과 문제를 하나 이상 등록해 주세요.");
        for (Scene scene : scenes.all()) {
            Set<String> ids = new HashSet<>();
            for (Scene.Token t : scene.tokens())
                if (!ids.add(t.id()) || t.x() < 30 || t.x() > 770 || t.y() < 35 || t.y() > 470
                        || !List.of("runner", "fielder", "ball", "fact").contains(t.kind()))
                    throw new IOException("장면 토큰 오류: " + scene.id() + " / " + t.id());
        }
        for (Question q : questions.all()) {
            if (rules.find(q.ruleId()).isEmpty()) throw new IOException("문제의 규칙 연결 오류: " + q.id());
            if (q.choices().size() < 2 || q.answer() < 0 || q.answer() >= q.choices().size())
                throw new IOException("정답 번호 오류: " + q.id());
            if (q.choices().stream().anyMatch(String::isBlank) || q.title().isBlank() || q.prompt().isBlank())
                throw new IOException("문제 내용 누락: " + q.id());
            if (q.hasImage()) {
                Path path = imagePath(q);
                if (!Files.isRegularFile(path) || javax.imageio.ImageIO.read(path.toFile()) == null)
                    throw new IOException("그림 파일을 읽을 수 없습니다: " + q.id() + " / " + path);
                Set<String> ids = new HashSet<>();
                for (Question.Clue clue : q.clueOptions())
                    if (clue.id().isBlank() || clue.text().isBlank() || !ids.add(clue.id()) || Attempt.NO_CLUE.equals(clue.id()))
                        throw new IOException("그림 문제 단서 오류: " + q.id());
                if (q.requiresClue() && (q.clueOptions().size() < 2 || !ids.contains(q.clueTarget()) || q.cluePrompt().isBlank()))
                    throw new IOException("그림 문제의 정답 단서 누락: " + q.id());
                if (!q.requiresClue() && !q.clueOptions().isEmpty()) throw new IOException("기초 문제에 불필요한 단서가 있습니다: " + q.id());
                continue;
            }
            if (scenes.find(q.beforeScene()).isEmpty() || scenes.find(q.afterScene()).isEmpty() || !q.requiresClue())
                throw new IOException("문제의 장면/단서 연결 오류: " + q.id());
            if (scene(q.beforeScene()).tokens().stream().noneMatch(t -> t.id().equals(q.clueTarget())))
                throw new IOException("타격 전 장면에 정답 단서가 없습니다: " + q.id());
            Matchup matchup = matchups.find(q.id()).orElseThrow(() -> new IOException("문제의 구단 배정이 없습니다: " + q.id()));
            if (matchup.offense().id().equals(matchup.defense().id())) throw new IOException("공격/수비 구단이 같습니다: " + q.id());
            for (Scene scene : List.of(scene(q.beforeScene()),scene(q.afterScene()))) {
                for (Scene.Token token : scene.tokens()) {
                    boolean runner = token.kind().equals("runner"), fielder = token.kind().equals("fielder");
                    if ((runner || fielder) && token.player() == null) throw new IOException("선수 배정 누락: " + scene.id()+" / "+token.id());
                    if (token.player() != null && (!(runner || fielder) || !token.player().team().id()
                            .equals((runner ? matchup.offense() : matchup.defense()).id())))
                        throw new IOException("공격/수비 선수 소속 오류: " + scene.id()+" / "+token.id());
                }
            }
        }
        for (Matchup matchup : matchups.all()) if (questions.find(matchup.id()).isEmpty()) throw new IOException("없는 문제의 구단 배정: " + matchup.id());
        Set<Integer> keys = new HashSet<>(); Set<String> assigned = new HashSet<>();
        for (LearningStep step : steps) {
            if (step.key() < 1 || !keys.add(step.key()) || step.questionIds().isEmpty() || step.questionIds().size() > 4 || step.goal().isBlank())
                throw new IOException("STEP 구성 오류: " + step.number());
            for (String id : step.questionIds())
                if (questions.find(id).isEmpty() || !assigned.add(id)) throw new IOException("STEP 문제 누락/중복: " + id);
        }
        if (assigned.size() != questions.all().size()) throw new IOException("모든 문제를 STEP에 한 번씩 배정해 주세요.");
    }
    public List<LearningStep> steps() { return steps; }
    public Path imagePath(Question q) throws IOException {
        if (!q.hasImage() || !q.imageFile().matches("[A-Za-z0-9_-]+\\.png")) throw new IOException("허용되지 않는 그림 파일명: " + q.id());
        Path root = data.resolve("images").toRealPath();
        Path path = root.resolve(q.imageFile()).toRealPath();
        if (!path.startsWith(root)) throw new IOException("그림 경로가 images 폴더 밖입니다: " + q.id());
        return path;
    }
    public List<Question.Clue> clues(Question q) {
        return q.hasImage() ? q.clueOptions() : scene(q.beforeScene()).tokens().stream()
                .map(t -> new Question.Clue(t.id(), t.displayLabel())).toList();
    }
    public List<Team> teams() { return teams.all(); }
    public List<Player> players() { return players.all(); }
    public Matchup matchup(String questionId) { return matchups.find(questionId).orElseThrow(); }
    public List<RuleConcept> rules() { return rules.all(); }
    public List<Question> questions() { return questions.all(); }
    public RuleConcept rule(String id) { return rules.find(id).orElseThrow(); }
    public Scene scene(String id) { return scenes.find(id).orElseThrow(); }
    public Optional<Question> question(String id) { return questions.find(id); }
    public List<RuleConcept> searchRules(String query) {
        String term = query.strip().toLowerCase(Locale.ROOT);
        return rules.search(r -> (r.title() + r.topic() + r.summary() + r.detail()).toLowerCase(Locale.ROOT).contains(term));
    }
}
