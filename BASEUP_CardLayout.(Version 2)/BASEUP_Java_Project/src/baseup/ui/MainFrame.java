package baseup.ui;

import baseup.data.QuestionRepository;
import baseup.data.ResultRepository;
import baseup.model.QuizQuestion;
import baseup.model.QuizResult;
import baseup.service.QuizSession;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * BASEUP의 메인 창. 교수님 예시처럼 CardLayout을 사용해
 * 홈/학습/퀴즈/결과/기록/문제관리 화면을 겹쳐 놓고 전환한다.
 * 각 화면의 상태는 화면을 바꾸더라도 유지된다.
 */
public final class MainFrame extends JFrame {
    public static final String HOME = "HOME";
    public static final String STUDY = "STUDY";
    public static final String QUIZ = "QUIZ";
    public static final String RESULT = "RESULT";
    public static final String HISTORY = "HISTORY";
    public static final String MANAGE = "MANAGE";

    private final QuestionRepository questions;
    private final ResultRepository results;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainCards = new JPanel(cardLayout);
    private final JPanel homeSlot = new JPanel(new BorderLayout());
    private final JPanel resultSlot = new JPanel(new BorderLayout());
    private final JPanel historySlot = new JPanel(new BorderLayout());
    private final StageDeckPanel studyStages;
    private final StageDeckPanel quizStages;

    public MainFrame(QuestionRepository questions, ResultRepository results) {
        super("BASEUP | 야구 학습 & 퀴즈");
        this.questions = questions;
        this.results = results;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(930, 670));
        setSize(1180, 860);
        setLocationRelativeTo(null);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.BG);
        getContentPane().add(makeNav(), BorderLayout.NORTH);

        // 메인 CardLayout에 모든 종류의 화면을 등록한다.
        mainCards.setBackground(Theme.BG);
        studyStages = new StageDeckPanel(this, questions, true);
        quizStages = new StageDeckPanel(this, questions, false);
        mainCards.add(homeSlot, HOME);
        mainCards.add(studyStages, STUDY);
        mainCards.add(quizStages, QUIZ);
        mainCards.add(resultSlot, RESULT);
        mainCards.add(historySlot, HISTORY);
        mainCards.add(new QuestionManagePanel(this, questions), MANAGE);
        add(mainCards, BorderLayout.CENTER);
        showHome();
    }

    private JPanel makeNav() {
        JPanel nav = Theme.panel();
        nav.setLayout(new BorderLayout());
        nav.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                new EmptyBorder(12, 25, 12, 25)));
        JLabel brand = Theme.label("⚾  BASEUP", 23, Theme.ORANGE, true);
        nav.add(brand, BorderLayout.WEST);
        JPanel actions = Theme.panel();
        actions.setLayout(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        addNavButton(actions, "홈", this::showHome);
        addNavButton(actions, "학습", this::showStudyStages);
        addNavButton(actions, "퀴즈", this::showQuizStages);
        addNavButton(actions, "기록", this::showHistory);
        addNavButton(actions, "문제 관리", this::showManagement);
        nav.add(actions, BorderLayout.EAST);
        return nav;
    }

    private void addNavButton(JPanel holder, String title, Runnable action) {
        JButton button = Theme.button(title, false);
        button.addActionListener(e -> action.run());
        holder.add(button);
    }

    private void showCard(String key) {
        cardLayout.show(mainCards, key);
    }

    // 홈과 결과·기록 화면은 그 안의 내용만 갈아끼운다.
    // 메인 CardLayout에 들어간 카드 자체는 제거하지 않는다.
    private static void replaceSlot(JPanel slot, JPanel screen) {
        slot.removeAll();
        slot.add(screen, BorderLayout.CENTER);
        slot.revalidate();
        slot.repaint();
    }

    public void showHome() {
        replaceSlot(homeSlot, new HomePanel(this, questions.getAll().size()));
        showCard(HOME);
    }

    /** 기존 HomePanel 및 ResultPanel과의 호환을 위한 메서드. */
    public void showCategorySelection(boolean study) {
        if (study) showStudyStages();
        else showQuizStages();
    }

    public void showStudyStages() {
        studyStages.refreshIfDataChanged();
        showCard(STUDY);
    }

    public void showQuizStages() {
        quizStages.refreshIfDataChanged();
        showCard(QUIZ);
    }

    public void startStudy(String category) {
        showStudyStages();
        studyStages.selectStage(category);
    }

    public void startQuiz(String category) {
        showQuizStages();
        quizStages.resetStage(category); // 명시적인 재도전은 해당 단계만 처음부터 시작
        quizStages.selectStage(category);
    }

    public void completeQuiz(QuizSession session) {
        QuizResult result = session.toResult();
        try {
            results.append(result);
        } catch (IOException ex) {
            warning("결과는 표시되지만 저장에 실패했습니다:\n" + ex.getMessage());
        }
        // 완료한 단계만 새 QuizSession으로 교체한다. 다른 단계의 진행 상태는 유지된다.
        quizStages.resetStage(session.getCategory());
        replaceSlot(resultSlot, new ResultPanel(this, session, result));
        showCard(RESULT);
    }

    public void showHistory() {
        try {
            replaceSlot(historySlot, new HistoryPanel(this, results.load()));
            showCard(HISTORY);
        } catch (IOException ex) {
            warning("기록을 읽지 못했습니다:\n" + ex.getMessage());
        }
    }

    public void showManagement() { showCard(MANAGE); }

    public void warning(String message) {
        JOptionPane.showMessageDialog(this, message, "BASEUP", JOptionPane.WARNING_MESSAGE);
    }
}
