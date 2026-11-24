package baseup.repository;

import baseup.model.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import static baseup.repository.Tsv.line;

/** 사용자 기록 저장 계약. 파일 이름과 TSV 형식은 이곳에서만 정의한다. */
public record UserRepositories(Repository<UserProfile> profiles,
                               Repository<Attempt> attempts,
                               Repository<Bookmark> bookmarks,
                               Repository<StudyNote> notes,
                               Repository<StepCompletion> completions,
                               Repository<NoteFavorite> noteFavorites) {
    public UserRepositories {
        Objects.requireNonNull(profiles);
        Objects.requireNonNull(attempts);
        Objects.requireNonNull(bookmarks);
        Objects.requireNonNull(notes);
        Objects.requireNonNull(completions);
        Objects.requireNonNull(noteFavorites);
    }

    public static UserRepositories files(Path userDir) {
        Repository<UserProfile> profileRepository = new FileRepository<>(userDir.resolve("profiles.tsv"), "id\tnickname",
                f -> new UserProfile(f[0], f[1]), x -> line(x.id(), x.nickname()));
        Repository<Attempt> attemptRepository = new FileRepository<>(userDir.resolve("attempts.tsv"),
                "id\tuserId\tquestionId\tchoice\tclue\tanswerCorrect\tclueCorrect\thintUsed\tseconds\tat",
                f -> new Attempt(f[0], f[1], f[2], Integer.parseInt(f[3]), f[4], bool(f[5]), bool(f[6]),
                        bool(f[7]), nonnegative(f[8]), Instant.parse(f[9])),
                x -> line(x.id(), x.userId(), x.questionId(), "" + x.choice(), x.clue(), "" + x.answerCorrect(),
                        "" + x.clueCorrect(), "" + x.hintUsed(), "" + x.seconds(), x.at().toString()));
        Repository<Bookmark> bookmarkRepository = new FileRepository<>(userDir.resolve("bookmarks.tsv"), "id\tuserId\truleId",
                f -> new Bookmark(f[0], f[1], f[2]), x -> line(x.id(), x.userId(), x.ruleId()));
        Repository<StudyNote> noteRepository = new FileRepository<>(userDir.resolve("notes.tsv"), "id\tuserId\truleId\ttext",
                f -> new StudyNote(f[0], f[1], f[2], f[3]), x -> line(x.id(), x.userId(), x.ruleId(), x.text()));
        Repository<StepCompletion> completionRepository = new FileRepository<>(userDir.resolve("steps.tsv"), "id\tuserId\tstep\tat",
                f -> new StepCompletion(f[0], f[1], Integer.parseInt(f[2]), Instant.parse(f[3])),
                x -> line(x.id(), x.userId(), "" + x.step(), x.at().toString()));
        // 메모 즐겨찾기(마이페이지). 이전 버전에는 파일이 없으므로 빈 목록으로 시작한다.
        Repository<NoteFavorite> noteFavoriteRepository = new FileRepository<>(userDir.resolve("note_favorites.tsv"),
                "id\tuserId\tnoteId", f -> new NoteFavorite(f[0], f[1], f[2]), x -> line(x.id(), x.userId(), x.noteId()));

        return new UserRepositories(profileRepository, attemptRepository, bookmarkRepository,
                noteRepository, completionRepository, noteFavoriteRepository);
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
}
