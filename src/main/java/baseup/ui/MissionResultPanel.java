package baseup.ui;

import baseup.model.Attempt;
import baseup.model.Question;
import baseup.model.QuizSession;
import baseup.service.LearningService;
import javax.swing.JPanel;
import javax.swing.JComponent;
import java.awt.BorderLayout;

import static baseup.ui.Theme.*;
import static baseup.ui.PageComponents.*;

/** 완료된 미션의 결과 화면. */
public final class MissionResultPanel extends JPanel {
    private final LearningService service;
    private final ScreenActions actions;
    private final QuizSession session;

    public MissionResultPanel(LearningService service, ScreenActions actions, QuizSession session) {
        super(new BorderLayout());
        this.service = service;
        this.actions = actions;
        this.session = session;
        setOpaque(false);
        add(build(), BorderLayout.CENTER);
    }

    private JComponent build() {
        int correct=(int)session.results().stream().filter(Attempt::understood).count();
        int independent=(int)session.results().stream().filter(Attempt::independent).count();
        JPanel p=page("MISSION COMPLETE","판정 하나만큼, 성장했어요.",session.stepNumber()>0?"STEP "+session.stepNumber()+" 완료 · "+(service.courseComplete()?"모든 단계를 완주했어요!":"다음 단계가 열렸어요."):"새로운 상황에서도 판단의 기준을 떠올려 보세요.");
        RoundedPanel result=card();
        result.setLayout(new BorderLayout(20,10));
        JPanel words=column();
        Theme.add(words,label(independent==session.size()?"NICE CALL!":"GOOD PRACTICE!",34,GREEN,true));
        gap(words,16);
        Theme.add(words,label("이해한 문제   "+correct+" / "+session.size(),22,INK,true));
        gap(words,12);
        Theme.add(words,label("힌트 없이 해결한 상황   "+independent+"개",15,MUTED,false));
        gap(words,12);
        Theme.add(words,paragraph("헷갈렸던 판단은 다음 도전의 다른 상황 속에서 다시 만나요.\n익숙해진 판단은 간격을 두고 돌아옵니다. 따로 정리할 숙제는 없어요.",2));
        result.add(words,BorderLayout.CENTER);
        Theme.add(p,result);
        gap(p,20);
        for(Attempt a:session.results()) {
            Question q=service.catalog().question(a.questionId()).orElseThrow();
            RoundedPanel row=card();
            row.setLayout(new BorderLayout());
            row.add(label(q.title(),15,INK,true),BorderLayout.WEST);
            row.add(label(a.independent()?"독립 해결 +30 XP까지":a.understood()?"힌트와 함께 이해":"다음 도전에서 이어 연습",13,a.understood()?GREEN:RED,true),BorderLayout.EAST);
            Theme.add(p,row);
            gap(p,10);
        }
        JPanel buttons=row(12);
        buttons.add(button(nextAction(service),true,actions::startNext));
        buttons.add(button("학습 경로 보기",false,()->actions.showPage("missions")));
        Theme.add(p,buttons);
        return scroll(p);
    }
}
