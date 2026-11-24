package baseup.ui;

import baseup.model.*;
import baseup.service.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import static baseup.ui.Theme.*;

/** 규칙 학습: STEP 목록을 보여 주고 고른 STEP을 시작한다. */
final class StudyScreen extends Screen {
    StudyScreen(MainFrame app) { super(app); }

    JPanel missionCards(int from,int to) {
        JPanel path=column();int completed=service.completedSteps();
        for(LearningStep step:service.steps()) {
            if(step.number()<from||step.number()>to) continue;
            boolean done=step.number()<=completed, unlocked=service.stepUnlocked(step.number());
            RoundedPanel item=new RoundedPanel(done?WHITE:unlocked?PALE:new Color(0xECEFEB),18);
            item.setLayout(new BorderLayout(18,0));item.setBorder(new EmptyBorder(17,20,17,20));
            RoundedPanel badge=new RoundedPanel(done?GREEN:unlocked?WHITE:LINE,48);badge.setBorder(new EmptyBorder(0,0,0,0));badge.setLayout(new BorderLayout());
            JLabel node=label(unlocked?String.valueOf(step.number()):"?",24,done?WHITE:unlocked?GREEN:MUTED,true);
            node.setHorizontalAlignment(SwingConstants.CENTER);badge.add(node);badge.setPreferredSize(new Dimension(48,48));item.add(badge,BorderLayout.WEST);
            JPanel info=column();Theme.add(info,label("STEP "+step.number()+"  /  "+(done?"완료":unlocked?"지금 도전":"아직 열리지 않은 단계"),11,unlocked?GREEN:MUTED,true));gap(info,6);
            Theme.add(info,label(unlocked?step.goal():"어떤 판정이 기다리고 있을까요?",16,unlocked?INK:MUTED,true));
            item.add(info,BorderLayout.CENTER);
            JButton action=button(done?"다시 도전":unlocked?"시작하기   →":"잠김",unlocked&&!done,()->{
                try { app.showQuiz(service.startStep(step.number()),"STEP "+step.number()); } catch(Exception e) { app.error(e); }
            });action.setEnabled(unlocked);item.add(action,BorderLayout.EAST);
            item.setMaximumSize(new Dimension(Integer.MAX_VALUE,92));Theme.add(path,item);
            if(step.number()<to){gap(path,3);Theme.add(path,label("       │",14,LINE,true));gap(path,3);}
        }
        return path;
    }
    @Override JComponent build() {
        JPanel p=page("YOUR LEARNING PATH","한 단계씩, 다음 베이스로.","새로운 상황 사이에 이전 판단이 다시 등장해요. 실수는 다음 도전을 준비하는 단서입니다.");
        if(service.courseComplete()){Theme.add(p,button(app.nextAction(),true,app::startNext));gap(p,20);}
        Theme.add(p,missionCards(1,service.steps().size()));return scroll(p);
    }
}
