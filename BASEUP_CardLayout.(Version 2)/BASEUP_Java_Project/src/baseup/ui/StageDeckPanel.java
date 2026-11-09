package baseup.ui;

import baseup.data.QuestionRepository;
import baseup.model.QuizQuestion;
import baseup.service.QuizSession;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * 교수님 단계별 예시를 응용한 화면.
 * 상단 단계 선택 JButton 4개 + 내부 CardLayout 4개를 갖는다.
 * 학습/퀴즈 화면 각각 이 클래스를 하나씩 사용하고, 단계별로 StudyPanel 또는 QuizPanel을 생성한다.
 * 탭과 달리 단계 선택 버튼 디자인을 자유롭게 바꿀 수 있다.
 */
public final class StageDeckPanel extends JPanel {
    private static final String[] CODES = {"E", "M", "H", "U"};
    private static final String[] CAPTIONS = {"1단계 · 쉬움", "2단계 · 중간", "3단계 · 어려움", "심판 판정"};
    private final MainFrame frame;
    private final QuestionRepository repository;
    private final boolean study;
    private final CardLayout stageLayout = new CardLayout();
    private final JPanel stageCards = new JPanel(stageLayout);
    private final Map<String, JPanel> slots = new LinkedHashMap<>();
    private final Map<String, JButton> buttons = new LinkedHashMap<>();
    private String active = "E";
    private long dataRevision;

    public StageDeckPanel(MainFrame frame, QuestionRepository repository, boolean study) {
        this.frame = frame;
        this.repository = repository;
        this.study = study;
        this.dataRevision = repository.getRevision();
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        JPanel header = Theme.panel();
        header.setBorder(new EmptyBorder(20, 45, 13, 45));
        header.setLayout(new BorderLayout(0, 14));
        header.add(Theme.label(study ? "학습 모드 · 단계 선택" : "퀴즈 모드 · 단계 선택", 25,
                Theme.WHITE, true), BorderLayout.NORTH);
        JPanel nav = Theme.panel();
        nav.setLayout(new GridLayout(1, 4, 10, 0));
        for (int i = 0; i < CODES.length; i++) {
            final String code = CODES[i];
            JButton stageButton = Theme.button(CAPTIONS[i], false);
            stageButton.addActionListener(e -> selectStage(code));
            buttons.put(code, stageButton);
            nav.add(stageButton);
        }
        header.add(nav, BorderLayout.CENTER);
        header.add(Theme.label("단계 버튼을 누르면 해당 화면으로 전환됩니다. 다른 단계에 갔다 돌아와도 진행 상태가 유지됩니다.",
                13, Theme.MUTED, false), BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        stageCards.setBackground(Theme.BG);
        for (String code : CODES) {
            JPanel slot = Theme.panel();
            slot.setLayout(new BorderLayout());
            slots.put(code, slot);
            stageCards.add(slot, code);
            resetStage(code);
        }
        add(stageCards, BorderLayout.CENTER);
        selectStage("E");
    }

    /** 문제 관리에서 추가/삭제되면 새 데이터를 읽어 단계 화면을 갱신한다. */
    public void refreshIfDataChanged() {
        if (dataRevision == repository.getRevision()) return;
        dataRevision = repository.getRevision();
        for (String code : CODES) resetStage(code);
        selectStage(active);
    }

    /** 학습/퀴즈 각 단계에 같은 종류의 화면 클래스를 다른 데이터로 구성한다. */
    public void resetStage(String code) {
        JPanel slot = slots.get(code);
        if (slot == null) throw new IllegalArgumentException("알 수 없는 단계: " + code);
        List<QuizQuestion> items = repository.getByCategory(code);
        slot.removeAll();
        if (items.isEmpty()) {
            JPanel empty = Theme.card();
            empty.setLayout(new BorderLayout());
            empty.add(Theme.label("이 단계에는 문제가 없습니다. 문제 관리에서 추가하세요.",
                    18, Theme.MUTED, false), BorderLayout.CENTER);
            slot.add(Theme.padded(empty, 35, 45, 35, 45), BorderLayout.CENTER);
        } else if (study) {
            slot.add(new StudyPanel(frame, items), BorderLayout.CENTER);
        } else {
            slot.add(new QuizPanel(frame, new QuizSession(items)), BorderLayout.CENTER);
        }
        slot.revalidate();
        slot.repaint();
    }

    public void selectStage(String code) {
        if (!slots.containsKey(code)) throw new IllegalArgumentException("알 수 없는 단계: " + code);
        active = code;
        stageLayout.show(stageCards, code);
        for (Map.Entry<String, JButton> entry : buttons.entrySet()) {
            boolean chosen = entry.getKey().equals(code);
            JButton button = entry.getValue();
            button.setBackground(chosen ? Theme.ORANGE : Theme.SURFACE_2);
            button.setForeground(chosen ? Theme.BG : Theme.WHITE);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(chosen ? Theme.ORANGE : Theme.BORDER, 1),
                    new EmptyBorder(12, 12, 12, 12)));
        }
    }

    public String getActiveStage() { return active; }
    public boolean isStudyMode() { return study; }
}
