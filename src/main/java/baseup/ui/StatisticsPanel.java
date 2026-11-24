package baseup.ui;

import baseup.service.LearningService;
import baseup.service.LearningStatistics;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.BorderLayout;

import static baseup.ui.Theme.*;
import static baseup.ui.PageComponents.*;

/** 사용자 학습 성취 요약. */
public final class StatisticsPanel extends JPanel {
    private final LearningService service;

    public StatisticsPanel(LearningService service) {
        super(new BorderLayout());
        this.service = service;
        setOpaque(false);
        setMaximumSize(new Dimension(Integer.MAX_VALUE,125));
        add(build(), BorderLayout.CENTER);
    }

    private JPanel build() {
        LearningStatistics statistics = service.statistics();
        JPanel row=new JPanel(new GridLayout(1,3,15,0));
        row.setOpaque(false);
        row.add(stat("완료한 단계",service.completedSteps()+" / "+service.steps().size(),"한 단계씩 열리는 나의 경로"));
        row.add(stat("스스로 해결한 상황",statistics.solvedCount()+"개","답과 필요한 단서 정답 · 힌트 없이"));
        row.add(stat("차곡차곡 쌓인 경험",statistics.xp()+" XP",statistics.rank()));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE,125));
        return row;
    }

    private JPanel stat(String caption,String number,String sub) {
        RoundedPanel card=card();
        card.setLayout(new BorderLayout(0,9));
        card.add(label(caption,12,MUTED,false),BorderLayout.NORTH);
        card.add(label(number,28,INK,true),BorderLayout.CENTER);
        card.add(label(sub,11,MUTED,false),BorderLayout.SOUTH);
        return card;
    }
}
