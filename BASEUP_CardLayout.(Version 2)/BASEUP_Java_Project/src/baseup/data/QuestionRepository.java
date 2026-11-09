package baseup.data;

import baseup.model.QuizQuestion;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** questions.txt 파일을 읽어 문제 객체를 관리한다. */
public final class QuestionRepository {
    private final Path path;
    private final Map<String, QuizQuestion> questions = new LinkedHashMap<>();
    private long revision = 0;

    public QuestionRepository(Path path) throws IOException {
        this.path = path;
        load();
    }
    private synchronized void load() throws IOException {
        if (!Files.exists(path)) throw new IOException("문제 파일이 없습니다: " + path.toAbsolutePath());
        int n = 0;
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            n++;
            if (line.trim().isEmpty() || line.startsWith("#")) continue;
            try {
                QuizQuestion q = QuizQuestion.fromLine(line);
                if (questions.putIfAbsent(q.getId(), q) != null)
                    throw new IllegalArgumentException("중복 문제 ID: " + q.getId());
            } catch (RuntimeException e) {
                throw new IOException("문제 파일 " + n + "행 오류: " + e.getMessage(), e);
            }
        }
    }
    public synchronized long getRevision() { return revision; }
    public synchronized List<QuizQuestion> getAll() { return new ArrayList<>(questions.values()); }
    public synchronized List<QuizQuestion> getByCategory(String category) {
        List<QuizQuestion> result = new ArrayList<>();
        for (QuizQuestion q : questions.values()) if (q.getCategory().equals(category)) result.add(q);
        return result;
    }
    public synchronized QuizQuestion findById(String id) {
        return questions.get(id.trim().toUpperCase());
    }
    public synchronized void addAndSave(QuizQuestion question) throws IOException {
        if (questions.containsKey(question.getId()))
            throw new IllegalArgumentException("이미 존재하는 문제 ID입니다: " + question.getId());
        questions.put(question.getId(), question);
        try { save(); revision++; }
        catch (IOException e) { questions.remove(question.getId()); throw e; }
    }
    public synchronized void deleteAndSave(String id) throws IOException {
        if (!questions.containsKey(id)) throw new IllegalArgumentException("문제가 존재하지 않습니다: " + id);
        Map<String, QuizQuestion> before = new LinkedHashMap<>(questions);
        questions.remove(id);
        try { save(); revision++; }
        catch (IOException e) {
            questions.clear();
            questions.putAll(before);
            throw e;
        }
    }
    private synchronized void save() throws IOException {
        Files.createDirectories(path.getParent());
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        List<String> lines = new ArrayList<>();
        lines.add("# BASEUP 문제 목록 / UTF-8 / 탭으로 분리한 14개 필드");
        lines.add("# id, category, imageFile, imageGuide, learningTerm, learningDescription, situationText, question, option1, option2, option3, option4, answerNumber, explanation");
        for (QuizQuestion q : questions.values()) lines.add(q.toLine());
        Files.write(temp, lines, StandardCharsets.UTF_8);
        try {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
