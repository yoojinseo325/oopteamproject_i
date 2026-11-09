package baseup.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/** 프로젝트 소개와 두 모드의 시작 화면. */
public final class HomePanel extends JPanel {
    public HomePanel(MainFrame frame, int total) {
        setLayout(new BorderLayout(0, 22));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(42, 55, 42, 55));
        JPanel hero = Theme.card();
        hero.setLayout(new BorderLayout(0, 18));
        JPanel text = new JPanel(new GridLayout(4, 1, 0, 12));
        text.setOpaque(false);
        text.add(Theme.label("LEARN THE GAME · PLAY THE QUIZ", 14, Theme.GREEN, true));
        text.add(Theme.label("BASEUP", 56, Theme.ORANGE, true));
        text.add(Theme.label("이미지로 배우는 야구, 퀴즈로 확인하는 실력", 23, Theme.WHITE, true));
        text.add(Theme.label("야구 규칙과 용어부터 심판 판정까지, 카드 학습과 4지선다 퀴즈로 익혀보세요.", 15, Theme.MUTED, false));
        hero.add(text, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 2));
        actions.setOpaque(false);
        JButton study = Theme.button("학습 모드 시작", true);
        study.addActionListener(e -> frame.showCategorySelection(true));
        JButton quiz = Theme.button("퀴즈 모드 시작", false);
        quiz.addActionListener(e -> frame.showCategorySelection(false));
        actions.add(study);
        actions.add(quiz);
        hero.add(actions, BorderLayout.SOUTH);
        add(hero, BorderLayout.CENTER);
        JPanel summary = Theme.panel();
        summary.setLayout(new GridLayout(1, 3, 15, 0));
        for (String[] data : new String[][]{{Integer.toString(total), "학습·퀴즈 문제"}, {"4", "문제 유형"}, {"100점", "10문항 만점"}}) {
            JPanel tile = Theme.card();
            tile.setLayout(new GridLayout(2, 1, 0, 8));
            tile.add(Theme.label(data[0], 26, Theme.WHITE, true));
            tile.add(Theme.label(data[1], 14, Theme.MUTED, false));
            summary.add(tile);
        }
        add(summary, BorderLayout.SOUTH);
    }
}
