package baseup.ui;

import baseup.model.Matchup;
import baseup.model.Question;
import baseup.model.QuizSession;
import baseup.model.Team;
import baseup.service.LearningService;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.util.function.Consumer;
import static baseup.ui.Theme.*;

/** 단계별 입력 화면. 채점 및 저장을 직접 구현하지 않는다. */
public final class QuizPanel extends JPanel {
    private final LearningService service;
    private final QuizSession session;
    private final Runnable complete;
    private final Runnable exit;
    private final Consumer<String> message;
    private final Consumer<Exception> error;
    private final QuestionImages images = new QuestionImages();
    private QuestionVisual visual;
    private QuizAnswerPanel answerPanel;
    private ActionButton primaryAction;

    public QuizPanel(LearningService service,QuizSession session,Runnable complete,Runnable exit,
    Consumer<String> message,Consumer<Exception> error) {
        this.service=service;
        this.session=session;
        this.complete=complete;
        this.exit=exit;
        this.message=message;
        this.error=error;
        setLayout(new BorderLayout());
        setOpaque(false);
        render();
    }
    private void render() {
        removeAll();
        answerPanel = null;
        primaryAction = createAction();
        Question q=session.current();
        boolean feedback=session.stage()==QuizSession.Stage.FEEDBACK;
        JPanel page=column();
        page.setBorder(new EmptyBorder(25,28,25,28));
        JPanel heading=new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JPanel title=column();
        // 큰 글씨에서도 잘리지 않도록 HTML 라벨로 그린다(Swing 라벨은 폭이 부족하면 "…"로 자른다).
        Theme.add(title,label("<html>"+(session.stepNumber()>0?"STEP "+session.stepNumber()+"&nbsp;&nbsp;/&nbsp;&nbsp;":"CHALLENGE&nbsp;&nbsp;/&nbsp;&nbsp;")+String.format("%02d",session.index()+1)+" / "+String.format("%02d",session.size())+"&nbsp;&nbsp;·&nbsp;&nbsp;"+html(q.level())+"</html>",13,GREEN,true));
        gap(title,8);
        Theme.add(title,label(q.title(),30,INK,true));
        heading.add(title,BorderLayout.CENTER);
        heading.add(button("미션 나가기",false,exit),BorderLayout.EAST);
        heading.setMaximumSize(new Dimension(Integer.MAX_VALUE,74));
        Theme.add(page,heading);
        gap(page,17);
        JProgressBar progress=new JProgressBar(0,session.size());
        progress.setValue(session.index()+(feedback?1:0));
        progress.setForeground(GREEN);
        progress.setBackground(LINE);
        progress.setBorderPainted(false);
        progress.setMaximumSize(new Dimension(Integer.MAX_VALUE,5));
        progress.setPreferredSize(new Dimension(900,5));
        Theme.add(page,progress);
        gap(page,16);
        JPanel stages=new JPanel(new GridLayout(1,q.requiresClue()?3:2,12,0));
        stages.setOpaque(false);
        String[] steps=q.requiresClue()?new String[]{
            "01  장면 보고 판정","02  단서 찾기","03  해설 확인"
        }
        :new String[]{
            "01  그림 보고 선택","02  해설 확인"
        };
        int current=feedback?steps.length-1:session.stage()==QuizSession.Stage.CLUE?1:0;
        for(int i=0;i<steps.length;i++) {
            JLabel l=label(steps[i],15,i==current?GREEN:MUTED,i==current);
            l.setBorder(new EmptyBorder(6,0,6,0));
            stages.add(l);
        }
        Theme.add(page,stages);
        gap(page,14);
        if(!q.hasImage()) {
            Matchup matchup=service.catalog().matchup(q.id());
            JPanel teams=new JPanel(new GridLayout(1,2,12,0));
            teams.setOpaque(false);
            teams.add(teamBadge(matchup.offense(),"공격 · R"));
            teams.add(teamBadge(matchup.defense(),"수비 · D"));
            teams.setMaximumSize(new Dimension(Integer.MAX_VALUE,46));
            Theme.add(page,teams);
            gap(page,6);
            Theme.add(page,label("<html>KBO 선수와 함께하는 가상 학습 상황</html>",14,MUTED,false));
            gap(page,12);
        }
        else {
            Theme.add(page,label("<html>PICTURE PLAY&nbsp;&nbsp;/&nbsp;&nbsp;그림 속 장면에서 판단의 기준을 찾아요</html>",14,MUTED,false));
            gap(page,12);
        }
        JPanel columns=new JPanel(new GridBagLayout());
        columns.setOpaque(false);
        GridBagConstraints c=new GridBagConstraints();
        c.fill=GridBagConstraints.BOTH;
        c.weighty=1;
        c.weightx=0.62;
        c.gridx=0;
        c.insets=new Insets(0,0,0,18);
        int height=q.hasImage()&&q.requiresClue()?720:640; // 교수님 피드백 7: 그림을 더 크게
        visual = q.hasImage()
        ? new ImageQuestionPanel(service.catalog(),session,this::chooseClue,images,error)
        : new SceneQuestionPanel(service.catalog(),session,this::chooseClue,message);
        visual.setMinimumSize(new Dimension(0,0));
        visual.setPreferredSize(new Dimension(0,height));
        columns.add(visual,c);
        c.gridx=1;
        c.weightx=0.38;
        c.insets=new Insets(0,0,0,0);
        JPanel question;
        if (feedback) {
            question = new QuizFeedbackPanel(service.catalog(), session);
        }
        else {
            answerPanel = new QuizAnswerPanel(service.catalog(), session, this::refreshAction, message);
            question = answerPanel;
        }
        height=Math.max(height,question.getPreferredSize().height+16);
        visual.setPreferredSize(new Dimension(0,height));
        question.setMinimumSize(new Dimension(0,0));
        question.setPreferredSize(new Dimension(0,height));
        columns.add(question,c);
        columns.setPreferredSize(new Dimension(950,height));
        columns.setMinimumSize(new Dimension(0,height));
        Theme.add(page,columns);
        add(scroll(page),BorderLayout.CENTER);
        JPanel actions=new JPanel(new BorderLayout(18,0));
        actions.setBackground(WHITE);
        actions.setBorder(new EmptyBorder(12,28,12,28));
        actions.add(label(feedback?"해설을 확인하고 다음 장면으로 가요.":"선택한 뒤 아래 버튼을 눌러주세요.",14,MUTED,false),BorderLayout.CENTER);
        primaryAction.setPreferredSize(new Dimension(360,54));
        actions.add(primaryAction,BorderLayout.EAST);
        add(actions,BorderLayout.SOUTH);
        revalidate();
        repaint();
    }
    private JComponent teamBadge(Team team,String role) {
        Color color=new Color(team.color());
        RoundedPanel badge=new RoundedPanel(color,12);
        badge.setBorder(new EmptyBorder(10,14,10,14));
        badge.setLayout(new BorderLayout());
        badge.add(label(team.name(),17,contrast(color),true),BorderLayout.WEST);
        badge.add(label(role,14,contrast(color),true),BorderLayout.EAST);
        return badge;
    }

    private ActionButton createAction() {
        if (session.stage() == QuizSession.Stage.FEEDBACK) {
            return button(session.index()+1==session.size()?"미션 결과 보기   →":"다음 상황으로   →", true, this::advance);
        }
        boolean judge = session.stage() == QuizSession.Stage.JUDGE;
        String title = judge ? (session.current().requiresClue() ? "판정 선택 완료 · 단서 찾기   →" : "답 제출하기   →")
        : "판정과 단서 제출하기   →";
        ActionButton action = button(title, true, this::submit);
        action.setEnabled(judge ? session.choice() >= 0 : !session.clue().isBlank());
        return action;
    }

    private void refreshAction() {
        primaryAction.setEnabled(session.stage() == QuizSession.Stage.JUDGE
        ? session.choice() >= 0 : !session.clue().isBlank());
    }

    private void chooseClue(String id) {
        session.chooseClue(id);
        visual.selectClue(id);
        answerPanel.clueChanged();
        refreshAction();
    }

    private void submit() {
        try {
            if (session.stage() == QuizSession.Stage.JUDGE && session.current().requiresClue()) {
                session.beginClue();
            }
            else {
                service.submit(session);
                message.accept("학습 기록을 저장했습니다.");
            }
            render();
        }
        catch (Exception e) {
            error.accept(e);
        }
    }

    private void advance() {
        try {
            service.advance(session);
            if (session.stage() == QuizSession.Stage.COMPLETE) complete.run();
            else render();
        }
        catch (Exception e) {
            error.accept(e);
        }
    }
}
