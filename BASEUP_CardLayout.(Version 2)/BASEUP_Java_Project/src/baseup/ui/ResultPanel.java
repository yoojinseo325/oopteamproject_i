package baseup.ui;

import baseup.model.QuizQuestion;
import baseup.model.QuizResult;
import baseup.service.QuizSession;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

/** 점수와 현재 퀴즈 오답 복습. */
public final class ResultPanel extends JPanel {
    public ResultPanel(MainFrame frame, QuizSession session, QuizResult result) {
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        JPanel body = Theme.panel();
        body.setLayout(new javax.swing.BoxLayout(body, javax.swing.BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(28, 55, 32, 55));
        body.add(Theme.label("퀴즈 결과 / " + QuizQuestion.categoryName(result.getCategory()), 28, Theme.WHITE, true));
        body.add(Theme.spacer(16));
        JPanel scores = Theme.panel();
        scores.setLayout(new GridLayout(1, 3, 13, 0));
        for (String[] s : new String[][]{{result.getScore()+"점", "최종 점수"},
                {result.getCorrect()+" / "+result.getTotal(), "정답"},
                {Integer.toString(result.getWrongIds().size()), "오답"}}) {
            JPanel tile = Theme.card();
            tile.setLayout(new GridLayout(2, 1, 0, 8));
            tile.add(Theme.label(s[0], 28, Theme.ORANGE, true));
            tile.add(Theme.label(s[1], 15, Theme.MUTED, false));
            scores.add(tile);
        }
        body.add(scores);
        body.add(Theme.spacer(18));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        JButton again = Theme.button("이 유형 다시 풀기", true);
        again.addActionListener(e -> frame.startQuiz(result.getCategory()));
        JButton home = Theme.button("홈으로", false);
        home.addActionListener(e -> frame.showHome());
        JButton history = Theme.button("기록 보기", false);
        history.addActionListener(e -> frame.showHistory());
        actions.add(again); actions.add(home); actions.add(history);
        body.add(actions);
        body.add(Theme.spacer(23));
        body.add(Theme.label("오답 복습", 21, Theme.WHITE, true));
        body.add(Theme.spacer(12));
        if (result.getWrongIds().isEmpty()) {
            JPanel win = Theme.card();
            win.setLayout(new BorderLayout());
            win.add(Theme.label("모든 문제를 맞혔습니다! 다른 유형에도 도전해 보세요.", 17, Theme.GREEN, true));
            body.add(win);
        } else {
            for (QuizQuestion q : session.getQuestions()) {
                if (!result.getWrongIds().contains(q.getId())) continue;
                JPanel wrong = Theme.card();
                wrong.setLayout(new javax.swing.BoxLayout(wrong, javax.swing.BoxLayout.Y_AXIS));
                wrong.add(Theme.label(q.getId()+" · "+q.getLearningTerm(), 17, Theme.ORANGE, true));
                wrong.add(Theme.spacer(6));
                wrong.add(Theme.wrap(q.getQuestion(), 15, Theme.WHITE));
                wrong.add(Theme.spacer(6));
                int chosen = session.getSelections().get(q.getId());
                wrong.add(Theme.wrap("내 답: "+chosen+"번 — "+q.getOptions()[chosen-1], 14, Theme.RED));
                wrong.add(Theme.spacer(5));
                wrong.add(Theme.wrap("정답: "+q.getAnswerNumber()+"번 — "+q.getAnswerText(), 14, Theme.GREEN));
                wrong.add(Theme.spacer(5));
                wrong.add(Theme.wrap("해설: "+q.getExplanation(), 14, Theme.MUTED));
                body.add(wrong);
                body.add(Theme.spacer(12));
            }
        }
        JScrollPane scroll = Theme.scroll(body);
        add(scroll, BorderLayout.CENTER);
    }
}
