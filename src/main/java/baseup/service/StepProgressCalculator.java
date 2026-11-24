package baseup.service;

import baseup.model.Attempt;
import baseup.model.LearningStep;
import baseup.model.QuizSession;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 진행 상태 게이지바(기능 1)에 쓸 값을 계산한다. 화면은 Gauge의 percent()와 label()만 그리면 된다.
 *
 * <pre>
 * StepProgressCalculator calc = new StepProgressCalculator(service);
 * calc.course();          // 전체 코스: 완료한 STEP / 전체 STEP
 * calc.currentStep();     // 지금 도전 중인 STEP 안에서 푼 문제 / 그 STEP의 문제 수
 * calc.rules();           // 문제로 만나 본 규칙 / 전체 규칙
 * StepProgressCalculator.session(quizSession); // 진행 중인 퀴즈: 제출한 문제 / 문제 수
 * </pre>
 *
 * 계산할 때마다 현재 사용자의 기록을 새로 읽으므로, 로그인 사용자가 바뀌어도 그대로 쓸 수 있다.
 */
public final class StepProgressCalculator {
    /** 게이지 한 개의 값. done은 0 이상 total 이하로 보정된다. */
    public record Gauge(int done, int total) {
        public Gauge {
            if (total < 0) throw new IllegalArgumentException("전체 수는 0 이상이어야 합니다.");
            done = Math.max(0, Math.min(done, total));
        }
        /** 0~100 정수. 전체가 0이면 0. JProgressBar.setValue()에 바로 넣을 수 있다. */
        public int percent() { return total == 0 ? 0 : (int) Math.floor(done * 100.0 / total); }
        /** 0.0~1.0. 직접 막대를 그릴 때 너비 계산용. */
        public double ratio() { return total == 0 ? 0 : (double) done / total; }
        public boolean complete() { return total > 0 && done == total; }
        /** "3 / 26" 형태. */
        public String label() { return done + " / " + total; }
    }

    private final LearningService service;

    public StepProgressCalculator(LearningService service) { this.service = service; }

    /** 전체 학습 코스 진행률: 완료한 STEP 수 / 전체 STEP 수. */
    public Gauge course() {
        return new Gauge(service.completedSteps(), service.steps().size());
    }

    /** 한 STEP 안의 진행률: 현재 사용자가 한 번이라도 제출한 문제 수 / 그 STEP의 문제 수. 완료한 STEP은 가득 찬다. */
    public Gauge step(int number) {
        LearningStep step = service.step(number);
        if (service.stepCompleted(number)) return new Gauge(step.questionIds().size(), step.questionIds().size());
        Set<String> answered = answeredQuestionIds();
        int done = (int) step.questionIds().stream().filter(answered::contains).count();
        return new Gauge(done, step.questionIds().size());
    }

    /** 다음에 도전할 STEP의 진행률. 코스를 모두 마쳤으면 마지막 STEP(가득 참)을 돌려준다. */
    public Gauge currentStep() { return step(currentStepNumber()); }

    /** 다음에 도전할 STEP 번호(화면 순번). */
    public int currentStepNumber() { return service.nextStepNumber(); }

    /** 규칙 발견 진행률: 문제로 한 번이라도 만난 규칙 수 / 전체 규칙 수. 백과사전이나 마이페이지용. */
    public Gauge rules() {
        int total = service.catalog().rules().size();
        int done = (int) service.catalog().rules().stream().filter(r -> service.discovered(r.id())).count();
        return new Gauge(done, total);
    }

    /** 진행 중인 퀴즈 세션의 진행률: 제출을 마친 문제 수 / 세션의 문제 수. */
    public static Gauge session(QuizSession session) {
        return new Gauge(session.results().size(), session.size());
    }

    private Set<String> answeredQuestionIds() {
        return service.attempts().stream().map(Attempt::questionId).collect(Collectors.toSet());
    }
}
