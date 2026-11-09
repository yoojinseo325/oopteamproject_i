package baseup.ui;

import baseup.data.QuestionRepository;
import baseup.data.ResultRepository;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** 디스플레이 환경에서 CardLayout 화면 및 단계 전환을 검증. GUI 창이 잠깐 열립니다. */
public final class GuiIntegrationTest {
    public static void main(String[] args) throws Exception {
        AtomicReference<Throwable> error = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            MainFrame frame = null;
            try {
                frame = new MainFrame(new QuestionRepository(Path.of("data/questions.txt")),
                        new ResultRepository(Path.of("data/results.txt")));
                frame.setVisible(true);
                frame.showStudyStages();
                StageDeckPanel study = findStagePanel(frame, true);
                check(study != null && study.getActiveStage().equals("E"), "학습 기본 단계");
                study.selectStage("M");
                check(study.getActiveStage().equals("M"), "학습 중간 단계 전환");
                study.selectStage("U");
                check(study.getActiveStage().equals("U"), "학습 심판 단계 전환");
                frame.showQuizStages();
                StageDeckPanel quiz = findStagePanel(frame, false);
                check(quiz != null && quiz.getActiveStage().equals("E"), "퀴즈 기본 단계");
                quiz.selectStage("H");
                check(quiz.getActiveStage().equals("H"), "퀴즈 어려움 단계 전환");
                frame.showHome();
                frame.startQuiz("U");
                check(quiz.getActiveStage().equals("U"), "심판 퀴즈 시작");
                frame.showManagement();
                frame.showStudyStages();
                check(study.getActiveStage().equals("U"), "학습 단계 유지");
                System.out.println("PASS: 메인 CardLayout + 단계별 CardLayout 화면 이동 및 단계 유지");
            } catch (Throwable ex) {
                error.set(ex);
            } finally {
                if (frame != null) frame.dispose();
            }
        });
        if (error.get() != null) throw new RuntimeException("GUI 테스트 실패", error.get());
    }
    private static StageDeckPanel findStagePanel(Container root, boolean study) {
        for (Component c : root.getComponents()) {
            if (c instanceof StageDeckPanel) {
                StageDeckPanel deck = (StageDeckPanel) c;
                if (study == deck.isStudyMode()) return deck;
            }
            if (c instanceof Container) {
                StageDeckPanel found = findStagePanel((Container)c, study);
                if (found != null) return found;
            }
        }
        return null;
    }
    private static void check(boolean b, String message) {
        if (!b) throw new AssertionError(message);
    }
}
