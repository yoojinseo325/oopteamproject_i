package baseup.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** 한 번의 퀴즈 진행 결과. */
public final class QuizResult {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final String timestamp;
    private final String category;
    private final int correct;
    private final int total;
    private final List<String> wrongIds;

    public QuizResult(String timestamp, String category, int correct, int total, List<String> wrongIds) {
        this.timestamp = timestamp;
        this.category = category;
        this.correct = correct;
        this.total = total;
        this.wrongIds = new ArrayList<>(wrongIds);
    }

    public static QuizResult now(String category, int correct, int total, List<String> wrongIds) {
        return new QuizResult(LocalDateTime.now().format(FORMAT), category, correct, total, wrongIds);
    }
    public String toLine() {
        return String.join("\t", timestamp, category, Integer.toString(correct),
                Integer.toString(total), String.join(",", wrongIds));
    }
    public static QuizResult fromLine(String line) {
        String[] p = line.split("\t", -1);
        if (p.length != 5) throw new IllegalArgumentException("기록 파일 형식 오류");
        List<String> wrong = p[4].isEmpty() ? Collections.emptyList() : Arrays.asList(p[4].split(","));
        return new QuizResult(p[0], p[1], Integer.parseInt(p[2]), Integer.parseInt(p[3]), wrong);
    }
    public String getTimestamp() { return timestamp; }
    public String getCategory() { return category; }
    public int getCorrect() { return correct; }
    public int getTotal() { return total; }
    public int getScore() { return total == 0 ? 0 : (int)Math.round(100.0 * correct / total); }
    public List<String> getWrongIds() { return Collections.unmodifiableList(wrongIds); }
}
