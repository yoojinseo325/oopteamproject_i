package baseup.data;

import baseup.model.QuizResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/** 퀴즈 기록을 results.txt에 영구 저장한다. */
public final class ResultRepository {
    private final Path path;
    public ResultRepository(Path path) { this.path = path; }
    public synchronized void append(QuizResult r) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, r.toLine() + System.lineSeparator(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
    public synchronized List<QuizResult> load() throws IOException {
        List<QuizResult> result = new ArrayList<>();
        if (!Files.exists(path)) return result;
        int n = 0;
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            n++;
            if (line.isBlank() || line.startsWith("#")) continue;
            try { result.add(QuizResult.fromLine(line)); }
            catch (RuntimeException ex) { throw new IOException("기록 파일 " + n + "행 오류: " + ex.getMessage(), ex); }
        }
        return result;
    }
}
