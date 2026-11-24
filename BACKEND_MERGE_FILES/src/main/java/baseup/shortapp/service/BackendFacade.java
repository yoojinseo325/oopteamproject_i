package baseup.shortapp.service;

import baseup.shortapp.data.QuestionRepository;
import baseup.shortapp.data.ResultRepository;
import baseup.shortapp.data.StudyRepository;
import baseup.shortapp.model.QuizQuestion;
import baseup.shortapp.model.QuizResult;
import baseup.shortapp.model.UserStatistics;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** 프론트 팀이 호출할 공개 진입점: 데이터 파일/채점/대시보드/시나리오. */
public final class BackendFacade {
    private final QuestionRepository questions;
    private final ResultRepository results;
    private final StudyRepository studies;
    private final ScenarioManager scenarios;

    /** userDataDirectory는 현재 로그인한 사용자별 폴더로 전달해야 한다. */
    public BackendFacade(QuestionRepository questions, Path userDataDirectory, Path assetsDirectory)
            throws IOException {
        this.questions = questions;
        this.results = new ResultRepository(userDataDirectory.resolve("results.tsv"));
        this.studies = new StudyRepository(userDataDirectory.resolve("study.tsv"));
        this.scenarios = new ScenarioManager(assetsDirectory);
    }
    public List<QuizQuestion> getQuestionsByCategory(String category) {
        return questions.getByCategory(category);
    }
    public QuizSession startQuiz(String category) {
        return new QuizSession(questions.getByCategory(category));
    }
    /** 결과 저장은 해당 세션의 모든 문항이 종료된 뒤 한 번만 호출하도록 한다. */
    public QuizResult saveFinishedQuiz(QuizSession finishedSession) throws IOException {
        QuizResult result = finishedSession.toResult();
        results.append(result);
        return result;
    }
    public UserStatistics getStatistics() throws IOException {
        return new StatisticsManager(results, studies).calculate();
    }
    public ScenarioManager.ScenarioMedia getScenario(QuizQuestion question) {
        return scenarios.getMedia(question);
    }
}
