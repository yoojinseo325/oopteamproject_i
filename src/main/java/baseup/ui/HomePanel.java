package baseup.ui;

import baseup.service.LearningService;
import javax.swing.JPanel;
import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.BorderLayout;

import static baseup.ui.Theme.*;
import static baseup.ui.PageComponents.*;

/** 현재 단계와 학습 요약을 보여주는 홈 화면. */
public final class HomePanel extends JPanel {
    private final LearningService service;
    private final ScreenActions actions;

    public HomePanel(LearningService service, ScreenActions actions) {
        super(new BorderLayout());
        this.service = service;
        this.actions = actions;
        setOpaque(false);
        add(build(), BorderLayout.CENTER);
    }

    private JComponent build() {
        boolean done=service.courseComplete();
        int current=service.nextStepNumber();
        JPanel p=page("YOUR NEXT BASE", "다음 판정은, 시작하면 펼쳐져요.", service.user().nickname()+"님, 한 단계씩 나만의 심판 감각을 키워 보세요.");
        RoundedPanel hero=new RoundedPanel(PALE,22);
        hero.setLayout(new BorderLayout(20,0));
        JPanel left=column();
        Theme.add(left,label(done?"ALL BASES CLEAR":"NOW PLAYING   /   STEP "+current,11,GREEN,true));
        gap(left,14);
        Theme.add(left,label(done?"모든 단계를 완주했어요!":service.step(current).goal(),25,INK,true));
        gap(left,12);
        Theme.add(left,paragraph(done?"익숙한 규칙도 다른 조건에서는 새로운 도전이 됩니다.\n지금 필요한 판단을 섞어서 다시 만나 보세요.":"어떤 상황이 기다릴지는 아직 비밀이에요.\n장면을 만나고, 판단하고, 해설을 확인하며 앞으로 나아가요.",3));
        gap(left,14);
        Theme.add(left,button(nextAction(service),true,actions::startNext));
        gap(left,12);
        Theme.add(left,label("한 번에 2~4개 상황  ·  시간제한 없음  ·  실수해도 다음 단계로",12,MUTED,false));
        hero.add(left,BorderLayout.CENTER);
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE,285));
        Theme.add(p,hero);
        gap(p,18);
        Theme.add(p,new StatisticsPanel(service));
        gap(p,24);
        Theme.add(p,label("나의 학습 경로",19,INK,true));
        gap(p,8);
        Theme.add(p,label("지금의 한 단계에 집중해요. 다음 내용은 도착했을 때 열립니다.",13,MUTED,false));
        gap(p,16);
        Theme.add(p,new LearningPathView(service, actions));
        gap(p,18);
        Theme.add(p,paragraph("따로 오답을 정리하지 않아도 괜찮아요. 헷갈렸던 판단은 다음 단계의 다른 상황 속에서 다시 연습해요.",2));
        return scroll(p);
    }
}
