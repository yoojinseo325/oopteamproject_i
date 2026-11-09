package baseup;

import baseup.data.QuestionRepository;
import baseup.data.ResultRepository;
import baseup.model.QuizQuestion;
import baseup.model.QuizResult;
import baseup.service.QuizSession;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;

/** 실행 전 데이터를 검사하고, 채점과 파일 입출력을 자동으로 검증하는 테스트. */
public final class SmokeTest {
    public static void main(String[] args) throws Exception {
        QuestionRepository repo = new QuestionRepository(Path.of("data", "questions.txt"));
        List<QuizQuestion> all = repo.getAll();
        check(all.size()==40, "원본 문항 수가 40이 아닙니다.");
        HashSet<String> names = new HashSet<>();
        for (QuizQuestion q : all) {
            check(names.add(q.getImageFile()), "이미지 파일명이 중복됩니다: " + q.getImageFile());
            check(Files.isRegularFile(Path.of("images", q.getImageFile())), "이미지 누락: " + q.getImageFile());
            check(q.getAnswerText().equals(q.getOptions()[q.getAnswerNumber()-1]), "정답 연결 오류");
        }
        for (String code : new String[]{"E", "M", "H", "U"}) {
            List<QuizQuestion> selected = repo.getByCategory(code);
            check(selected.size()==10, code+" 유형은 10문제가 있어야 합니다.");
            QuizSession session = new QuizSession(selected);
            for (QuizQuestion q : selected) {
                check(session.getCurrentQuestion().getId().equals(q.getId()), "문제 순서 오류");
                check(session.submit(q.getAnswerNumber()), "정답 채점 오류");
                session.next();
            }
            check(session.isFinished(), "퀴즈 완료 상태 오류");
            QuizResult result = session.toResult();
            check(result.getScore()==100 && result.getWrongIds().isEmpty(), "점수 계산 오류");
        }
        Path temp = Files.createTempDirectory("baseup-test-");
        try {
            ResultRepository r = new ResultRepository(temp.resolve("results.txt"));
            QuizResult sample = QuizResult.now("E", 8, 10, List.of("E02", "E08"));
            r.append(sample);
            check(r.load().size()==1 && r.load().get(0).getWrongIds().size()==2, "기록 저장/읽기 오류");
            Path copy = temp.resolve("questions.txt");
            Files.copy(Path.of("data", "questions.txt"), copy, StandardCopyOption.REPLACE_EXISTING);
            QuestionRepository r2 = new QuestionRepository(copy);
            QuizQuestion first = r2.findById("E01");
            r2.deleteAndSave("E01");
            check(new QuestionRepository(copy).getAll().size()==39, "문제 삭제/저장 오류");
            r2.addAndSave(first);
            check(new QuestionRepository(copy).getAll().size()==40, "문제 추가/저장 오류");
        } finally {
            Files.walk(temp).sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (Exception ignored) {}
            });
        }
        System.out.println("PASS: 원본 40문항, 유형별 10문항, 정답 판정, 결과 저장, 문제 추가/삭제 검증 완료");
    }
    private static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
    }
}
