package baseup.ui;

import baseup.model.QuizQuestion;
import baseup.service.QuizSession;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Enumeration;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/** 4지선다 제출, 정오답/해설 공개, 다음 문제 이동 화면. */
public final class QuizPanel extends JPanel {
    private final MainFrame frame;
    private final QuizSession session;
    private final JScrollPane scroll;

    public QuizPanel(MainFrame frame, QuizSession session) {
        this.frame = frame;
        this.session = session;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        scroll = Theme.scroll(Theme.panel());
        add(scroll, BorderLayout.CENTER);
        showQuestion();
    }
    private void showQuestion() {
        QuizQuestion q = session.getCurrentQuestion();
        JPanel page = Theme.panel();
        page.setLayout(new BorderLayout(0, 15));
        page.setBorder(new EmptyBorder(25, 55, 28, 55));
        JPanel top = Theme.panel();
        top.setLayout(new BorderLayout());
        top.add(Theme.label("퀴즈 모드  /  " + QuizQuestion.categoryName(q.getCategory()), 24, Theme.WHITE, true), BorderLayout.WEST);
        top.add(Theme.label("문제 " + (session.getIndex()+1) + " / " + session.getTotal(), 15, Theme.ORANGE, true), BorderLayout.EAST);
        page.add(top, BorderLayout.NORTH);
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 16));
        ImageView image = new ImageView();
        image.setImageFile(q.getImageFile());
        image.setPreferredSize(new java.awt.Dimension(700, 300));
        card.add(image, BorderLayout.NORTH);
        JPanel main = new JPanel();
        main.setOpaque(false);
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        if (!q.getSituationText().isBlank()) {
            main.add(Theme.wrap("상황: " + q.getSituationText(), 15, Theme.MUTED));
            main.add(Theme.spacer(12));
        }
        main.add(Theme.wrap("Q. " + q.getQuestion(), 18, Theme.WHITE));
        main.add(Theme.spacer(16));
        ButtonGroup group = new ButtonGroup();
        String[] choices = q.getOptions();
        JRadioButton[] buttons = new JRadioButton[4];
        for (int i = 0; i < 4; i++) {
            int answerNumber = i+1;
            JRadioButton b = new JRadioButton(answerNumber + ".  " + choices[i]);
            b.setActionCommand(Integer.toString(answerNumber));
            b.setFont(Theme.font(Font.PLAIN, 15));
            b.setForeground(Theme.WHITE);
            b.setBackground(Theme.SURFACE_2);
            b.setOpaque(true);
            b.setFocusPainted(false);
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.BORDER), new EmptyBorder(9, 12, 9, 12)));
            b.setBorderPainted(true);
            group.add(b);
            buttons[i] = b;
            JPanel item = new JPanel(new BorderLayout());
            item.setOpaque(false);
            item.add(b);
            main.add(item);
            main.add(Theme.spacer(7));
        }
        JPanel feedback = new JPanel();
        feedback.setOpaque(false);
        feedback.setLayout(new BoxLayout(feedback, BoxLayout.Y_AXIS));
        feedback.setVisible(false);
        main.add(feedback);
        main.add(Theme.spacer(10));
        JButton submit = Theme.button("정답 제출", true);
        JButton next = Theme.button(session.getIndex()+1 == session.getTotal() ? "결과 확인 →" : "다음 문제 →", false);
        next.setEnabled(false);
        submit.addActionListener(e -> {
            if (group.getSelection() == null) {
                frame.warning("정답을 하나 선택해 주세요.");
                return;
            }
            int pick = Integer.parseInt(group.getSelection().getActionCommand());
            boolean right = session.submit(pick);
            for (JRadioButton b : buttons) b.setEnabled(false);
            feedback.removeAll();
            feedback.add(Theme.label(right ? "정답입니다!" : "아쉽지만 오답입니다.",
                    20, right ? Theme.GREEN : Theme.RED, true));
            feedback.add(Theme.spacer(7));
            feedback.add(Theme.wrap("정답: " + q.getAnswerNumber() + "번 — " + q.getAnswerText(), 16, Theme.ORANGE));
            feedback.add(Theme.spacer(7));
            feedback.add(Theme.wrap("해설: " + q.getExplanation(), 15, Theme.WHITE));
            feedback.setVisible(true);
            submit.setEnabled(false);
            next.setEnabled(true);
            main.revalidate();
            main.repaint();
        });
        next.addActionListener(e -> {
            session.next();
            if (session.isFinished()) frame.completeQuiz(session);
            else showQuestion();
        });
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        controls.setOpaque(false);
        controls.add(submit);
        controls.add(next);
        main.add(controls);
        card.add(main, BorderLayout.CENTER);
        page.add(card, BorderLayout.CENTER);
        scroll.setViewportView(page);
        SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
    }
}
