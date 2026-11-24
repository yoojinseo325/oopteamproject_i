package baseup.shortapp.service;

import baseup.shortapp.model.QuizQuestion;
import java.nio.file.Path;
import java.util.List;

/** 시나리오 메타데이터 제공. 애니메이션/MP4 재생은 UI에서 담당한다. */
public final class ScenarioManager {
    public record ScenarioMedia(String questionId, String situation, Path gifPath, Path mp4Path, Path stillPath) { }

    private final Path root;
    public ScenarioManager(Path projectRoot) { this.root = projectRoot.toAbsolutePath().normalize(); }

    public ScenarioMedia getMedia(QuizQuestion question) {
        if (question == null || !question.hasScenario())
            throw new IllegalArgumentException("영상형 문제로 지정되지 않았습니다");
        String name = question.getScenarioGif();
        if (!name.matches("[a-zA-Z0-9_-]+\\.gif")) throw new IllegalArgumentException("잘못된 영상 경로");
        String stem = name.substring(0,name.length()-4);
        Path mediaRoot = root.resolve("scenarios");
        return new ScenarioMedia(question.getId(), question.getSituationText(),
                mediaRoot.resolve(name), mediaRoot.resolve(stem + ".mp4"), mediaRoot.resolve(stem + ".png"));
    }
    public List<String> getScenarioIds(List<QuizQuestion> all) {
        return all.stream().filter(QuizQuestion::hasScenario).map(QuizQuestion::getId).toList();
    }
}
