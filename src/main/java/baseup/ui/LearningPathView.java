package baseup.ui;

import baseup.model.LearningStep;
import baseup.service.LearningService;
import baseup.service.LearningProgress;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.BorderLayout;

import static baseup.ui.Theme.*;
import static baseup.ui.PageComponents.*;

/** 홈과 경로 화면이 공유하는 단계 목록. */
public final class LearningPathView extends JPanel {
    private final LearningService service;
    private final ScreenActions actions;

    public LearningPathView(LearningService service, ScreenActions actions) {
        super(new BorderLayout());
        this.service = service;
        this.actions = actions;
        setOpaque(false);
        add(build(), BorderLayout.CENTER);
    }

    private JPanel build() {
        LearningProgress progress = service.progress();
        JPanel path=column();
        for(LearningStep step:service.steps()) {
            boolean done=progress.stepCompleted(step.number()), unlocked=progress.stepUnlocked(step.number());
            RoundedPanel item=new RoundedPanel(done?WHITE:unlocked?PALE:new Color(0xECEFEB),18);
            item.setLayout(new BorderLayout(18,0));
            item.setBorder(new EmptyBorder(17,20,17,20));
            RoundedPanel badge=new RoundedPanel(done?GREEN:unlocked?WHITE:LINE,48);
            badge.setBorder(new EmptyBorder(0,0,0,0));
            badge.setLayout(new BorderLayout());
            JLabel node=label(unlocked?String.valueOf(step.number()):"?",24,done?WHITE:unlocked?GREEN:MUTED,true);
            node.setHorizontalAlignment(SwingConstants.CENTER);
            badge.add(node);
            badge.setPreferredSize(new Dimension(48,48));
            item.add(badge,BorderLayout.WEST);
            JPanel info=column();
            Theme.add(info,label("STEP "+step.number()+"  /  "+(done?"완료":unlocked?"지금 도전":"아직 열리지 않은 단계"),11,unlocked?GREEN:MUTED,true));
            gap(info,6);
            Theme.add(info,label(unlocked?step.goal():"어떤 판정이 기다리고 있을까요?",16,unlocked?INK:MUTED,true));
            item.add(info,BorderLayout.CENTER);
            JButton action=button(done?"다시 도전":unlocked?"시작하기   →":"잠김",unlocked&&!done,()->{
                actions.startStep(step.number());
            });
            action.setEnabled(unlocked);
            item.add(action,BorderLayout.EAST);
            item.setMaximumSize(new Dimension(Integer.MAX_VALUE,92));
            Theme.add(path,item);
            if(step.number()<service.steps().size()){
                gap(path,3);
                Theme.add(path,label("       │",14,LINE,true));
                gap(path,3);
            }
        }
        return path;
    }
}
