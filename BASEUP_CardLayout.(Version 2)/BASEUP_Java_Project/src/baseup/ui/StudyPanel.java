package baseup.ui;

import baseup.model.QuizQuestion;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/** 학습 카드 앞면은 이미지, 클릭 후 용어와 설명을 보여 준다. */
public final class StudyPanel extends JPanel {
    private final MainFrame frame;
    private final List<QuizQuestion> cards;
    private final JScrollPane scroll;
    private int index = 0;
    private boolean revealed = false;

    public StudyPanel(MainFrame frame, List<QuizQuestion> cards) {
        this.frame = frame;
        this.cards = cards;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        JPanel holder = Theme.panel();
        holder.setLayout(new BorderLayout());
        scroll = Theme.scroll(holder);
        add(scroll, BorderLayout.CENTER);
        showCard();
    }
    private void showCard() {
        QuizQuestion q = cards.get(index);
        JPanel page = Theme.panel();
        page.setLayout(new BorderLayout(0, 20));
        page.setBorder(new EmptyBorder(26, 55, 28, 55));
        JPanel top = Theme.panel();
        top.setLayout(new BorderLayout());
        top.add(Theme.label("학습 모드  /  " + QuizQuestion.categoryName(q.getCategory()), 24, Theme.WHITE, true), BorderLayout.WEST);
        top.add(Theme.label("카드 " + (index + 1) + " / " + cards.size(), 15, Theme.ORANGE, true), BorderLayout.EAST);
        page.add(top, BorderLayout.NORTH);
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 14));
        ImageView view = new ImageView();
        view.setImageFile(q.getImageFile());
        view.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.add(view, BorderLayout.NORTH);
        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new javax.swing.BoxLayout(details, javax.swing.BoxLayout.Y_AXIS));
        if (!q.getSituationText().isBlank()) {
            details.add(Theme.label("상황을 읽고 판정해 보세요", 15, Theme.ORANGE, true));
            details.add(Theme.spacer(8));
            details.add(Theme.wrap(q.getSituationText(), 16, Theme.WHITE));
            details.add(Theme.spacer(18));
        }
        JPanel revealedContent = new JPanel();
        revealedContent.setOpaque(false);
        revealedContent.setLayout(new javax.swing.BoxLayout(revealedContent, javax.swing.BoxLayout.Y_AXIS));
        revealedContent.add(Theme.label(q.getLearningTerm(), 22, Theme.GREEN, true));
        revealedContent.add(Theme.spacer(12));
        revealedContent.add(Theme.wrap(q.getLearningDescription(), 17, Theme.WHITE));
        revealedContent.setVisible(revealed);
        details.add(revealedContent);
        JButton reveal = Theme.button(revealed ? "설명 숨기기" : "이미지 클릭 / 정답 보기", true);
        Runnable toggle = () -> {
            revealed = !revealed;
            revealedContent.setVisible(revealed);
            reveal.setText(revealed ? "설명 숨기기" : "이미지 클릭 / 정답 보기");
            revalidate();
            repaint();
        };
        reveal.addActionListener(e -> toggle.run());
        view.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { toggle.run(); } });
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        controls.setOpaque(false);
        controls.add(reveal);
        details.add(Theme.spacer(14));
        details.add(controls);
        card.add(details, BorderLayout.CENTER);
        page.add(card, BorderLayout.CENTER);
        JPanel bottom = Theme.panel();
        bottom.setLayout(new BorderLayout());
        JButton previous = Theme.button("← 이전 카드", false);
        previous.setEnabled(index > 0);
        previous.addActionListener(e -> move(-1));
        JButton next = Theme.button("다음 카드 →", true);
        next.setEnabled(index + 1 < cards.size());
        next.addActionListener(e -> move(1));
        bottom.add(previous, BorderLayout.WEST);
        bottom.add(next, BorderLayout.EAST);
        page.add(bottom, BorderLayout.SOUTH);
        scroll.setViewportView(page);
        SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
    }
    private void move(int delta) {
        index += delta;
        revealed = false; // 문제 전환 시 학습 설명을 다시 숨긴다.
        showCard();
    }
}
