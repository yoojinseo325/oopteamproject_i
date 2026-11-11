package baseup.ui;

import baseup.model.*;
import baseup.service.LearningService;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
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
    private boolean before = true;
    private FieldPanel field;
    private JLabel caption;
    private ActionButton submit;
    private ActionButton primaryAction;
    private final Map<String,ActionButton> clueButtons=new LinkedHashMap<>();
    private final Map<Integer,ActionButton> choiceButtons=new LinkedHashMap<>();

    public QuizPanel(LearningService service,QuizSession session,Runnable complete,Runnable exit,
                     Consumer<String> message,Consumer<Exception> error) {
        this.service=service;this.session=session;this.complete=complete;this.exit=exit;this.message=message;this.error=error;
        setLayout(new BorderLayout());setOpaque(false);render();
    }
    private void render() {
        removeAll();clueButtons.clear();choiceButtons.clear();field=null;
        Question q=session.current();boolean feedback=session.stage()==QuizSession.Stage.FEEDBACK;
        JPanel page=column();page.setBorder(new EmptyBorder(25,28,25,28));
        JPanel heading=new JPanel(new BorderLayout());heading.setOpaque(false);
        JPanel title=column();Theme.add(title,label((session.stepNumber()>0?"STEP "+session.stepNumber()+"   /   ":"CHALLENGE   /   ")+String.format("%02d",session.index()+1)+" / "+String.format("%02d",session.size())+"   ·   "+q.level(),11,GREEN,true));gap(title,8);
        Theme.add(title,label(q.title(),27,INK,true));heading.add(title,BorderLayout.CENTER);
        heading.add(button("미션 나가기",false,exit),BorderLayout.EAST);heading.setMaximumSize(new Dimension(Integer.MAX_VALUE,65));Theme.add(page,heading);gap(page,17);
        JProgressBar progress=new JProgressBar(0,session.size());progress.setValue(session.index()+(feedback?1:0));
        progress.setForeground(GREEN);progress.setBackground(LINE);progress.setBorderPainted(false);
        progress.setMaximumSize(new Dimension(Integer.MAX_VALUE,5));progress.setPreferredSize(new Dimension(900,5));Theme.add(page,progress);gap(page,16);
        JPanel stages=new JPanel(new GridLayout(1,q.requiresClue()?3:2,12,0));stages.setOpaque(false);
        String[] steps=q.requiresClue()?new String[]{"01  장면 보고 판정","02  단서 찾기","03  해설 확인"}:new String[]{"01  그림 보고 선택","02  해설 확인"};
        int current=feedback?steps.length-1:session.stage()==QuizSession.Stage.CLUE?1:0;
        for(int i=0;i<steps.length;i++) {JLabel l=label(steps[i],12,i==current?GREEN:MUTED,i==current);l.setBorder(new EmptyBorder(6,0,6,0));stages.add(l);}
        Theme.add(page,stages);gap(page,14);
        if(!q.hasImage()) {
        Matchup matchup=service.catalog().matchup(q.id());
        JPanel teams=new JPanel(new GridLayout(1,2,12,0));teams.setOpaque(false);
        teams.add(teamBadge(matchup.offense(),"공격 · R"));teams.add(teamBadge(matchup.defense(),"수비 · D"));
        teams.setMaximumSize(new Dimension(Integer.MAX_VALUE,46));Theme.add(page,teams);gap(page,6);
        Theme.add(page,label("KBO 선수와 함께하는 가상 학습 상황",11,MUTED,false));gap(page,12);
        } else {
            Theme.add(page,label("PICTURE PLAY   /   그림 속 장면에서 판단의 기준을 찾아요",11,MUTED,false));gap(page,12);
        }
        JPanel columns=new JPanel(new GridBagLayout());columns.setOpaque(false);
        GridBagConstraints c=new GridBagConstraints();c.fill=GridBagConstraints.BOTH;c.weighty=1;c.weightx=0.56;c.gridx=0;c.insets=new Insets(0,0,0,18);
        int height=q.hasImage()&&q.requiresClue()?600:530;
        JPanel visual=q.hasImage()?imageCard(q):sceneCard(q);visual.setMinimumSize(new Dimension(0,0));visual.setPreferredSize(new Dimension(0,height));columns.add(visual,c);
        c.gridx=1;c.weightx=0.44;c.insets=new Insets(0,0,0,0);JPanel question=questionCard(q);
        height=Math.max(height,question.getPreferredSize().height+16);visual.setPreferredSize(new Dimension(0,height));
        question.setMinimumSize(new Dimension(0,0));question.setPreferredSize(new Dimension(0,height));columns.add(question,c);
        columns.setPreferredSize(new Dimension(950,height));columns.setMinimumSize(new Dimension(0,height));Theme.add(page,columns);
        add(scroll(page),BorderLayout.CENTER);
        JPanel actions=new JPanel(new BorderLayout(18,0));actions.setBackground(WHITE);actions.setBorder(new EmptyBorder(12,28,12,28));
        actions.add(label(feedback?"해설을 확인하고 다음 장면으로 가요.":"선택한 뒤 아래 버튼을 눌러주세요.",12,MUTED,false),BorderLayout.CENTER);
        primaryAction.setPreferredSize(new Dimension(350,48));actions.add(primaryAction,BorderLayout.EAST);add(actions,BorderLayout.SOUTH);
        revalidate();repaint();
    }
    private JPanel imageCard(Question q) {
        RoundedPanel card=card();card.setBorder(new EmptyBorder(18,18,18,18));card.setLayout(new BorderLayout(0,12));
        JPanel top=new JPanel(new BorderLayout());top.setOpaque(false);
        top.add(label("PICTURE VIEW   /   장면을 관찰하세요",11,GREEN,true));
        ActionButton zoom=button("크게 보기",false,()->{
            try {
                JDialog dialog=new JDialog(SwingUtilities.getWindowAncestor(this),"장면 크게 보기",Dialog.ModalityType.APPLICATION_MODAL);
                dialog.add(new ImageView(service.catalog().imagePath(q),q.title()));
                Dimension screen=Toolkit.getDefaultToolkit().getScreenSize();dialog.setSize(Math.min(1100,screen.width-80),Math.min(800,screen.height-80));
                dialog.setLocationRelativeTo(this);dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);dialog.setVisible(true);
            } catch(Exception e) { error.accept(e); }
        });zoom.setFont(font(11,false));top.add(zoom,BorderLayout.EAST);card.add(top,BorderLayout.NORTH);
        try { card.add(new ImageView(service.catalog().imagePath(q),q.title()),BorderLayout.CENTER); }
        catch(Exception e) { card.add(label("그림을 불러오지 못했습니다.",14,RED,true),BorderLayout.CENTER); }
        JPanel bottom=column();Theme.add(bottom,copy(q.imageGuide(),12,MUTED,1));gap(bottom,12);
        if(session.stage()==QuizSession.Stage.CLUE) {
            Theme.add(bottom,label("그림과 설명을 보고, 결정적인 조건을 골라요.",12,GREEN,true));gap(bottom,10);
            for(Question.Clue clue:q.clueOptions()) {
                ActionButton b=button("<html>"+html(clue.text())+"</html>",false,()->chooseClue(clue.id()));
                b.setName("clue-"+clue.id());b.setFont(font(12,true));b.setHorizontalAlignment(SwingConstants.LEFT);
                int h=Math.max(48,24+20*(int)Math.ceil(b.getFontMetrics(b.getFont()).stringWidth(clue.text())/300.0));
                b.setPreferredSize(new Dimension(300,h));b.setMinimumSize(new Dimension(0,h));b.setMaximumSize(new Dimension(Integer.MAX_VALUE,h));b.mark(clue.id().equals(session.clue()));
                clueButtons.put(clue.id(),b);Theme.add(bottom,b);gap(bottom,7);
            }
        } else if(session.stage()==QuizSession.Stage.FEEDBACK) {
            Theme.add(bottom,copy("발견한 규칙  ·  "+service.catalog().rule(q.ruleId()).title(),14,GREEN,1));gap(bottom,7);
            Theme.add(bottom,copy(service.catalog().rule(q.ruleId()).summary(),13,INK,2));
        } else Theme.add(bottom,copy("정답과 규칙 카드는 답을 제출하면 열려요. 막히면 코치의 힌트를 확인하세요.",12,MUTED,2));
        card.add(bottom,BorderLayout.SOUTH);return card;
    }
    private JComponent teamBadge(Team team,String role) {
        Color color=new Color(team.color());
        RoundedPanel badge=new RoundedPanel(color,12);badge.setBorder(new EmptyBorder(10,14,10,14));badge.setLayout(new BorderLayout());
        badge.add(label(team.name(),15,contrast(color),true),BorderLayout.WEST);
        badge.add(label(role,12,contrast(color),true),BorderLayout.EAST);
        return badge;
    }
    private JPanel sceneCard(Question q) {
        RoundedPanel card=card();card.setBorder(new EmptyBorder(20,20,20,20));card.setLayout(new BorderLayout(0,12));
        JPanel top=column();Theme.add(top,label("FIELD VIEW   /   장면을 관찰하세요",11,GREEN,true));gap(top,14);
        JPanel toggle=new JPanel(new GridLayout(1,2,8,0));toggle.setOpaque(false);
        ActionButton first=button("장면 1 · 상황 시작",false,()->setScene(true));
        ActionButton second=button("장면 2 · 판정 순간",false,()->setScene(false));
        first.setName("before");second.setName("after");toggle.add(first);toggle.add(second);Theme.add(top,toggle);card.add(top,BorderLayout.NORTH);
        if(session.stage()==QuizSession.Stage.CLUE)before=true;
        Scene scene=service.catalog().scene(before?q.beforeScene():q.afterScene());
        field=new FieldPanel(scene);field.setPreferredSize(new Dimension(500,350));first.mark(before);second.mark(!before);
        field.putClientProperty("first",first);field.putClientProperty("second",second);
        if(session.stage()==QuizSession.Stage.CLUE)field.onSelect(this::chooseClue);
        if(session.stage()==QuizSession.Stage.FEEDBACK) {before=true;field.scene(service.catalog().scene(q.beforeScene()));field.selected(q.clueTarget());first.mark(true);second.mark(false);}
        JPanel middle=new JPanel(new BorderLayout(0,10));middle.setOpaque(false);middle.add(field,BorderLayout.CENTER);
        caption=label("<html>"+html(service.catalog().scene(before?q.beforeScene():q.afterScene()).caption())+"</html>",12,MUTED,false);
        caption.setPreferredSize(new Dimension(300,45));middle.add(caption,BorderLayout.SOUTH);card.add(middle,BorderLayout.CENTER);
        JPanel bottom=column();Theme.add(bottom,label("R 공격 · D 수비  /  선수 색상 = 소속 구단  /  ! 상황 정보",11,MUTED,false));gap(bottom,15);
        if(session.stage()==QuizSession.Stage.CLUE) {
            Theme.add(bottom,label("그림 속 대상을 클릭하거나 아래에서 선택하세요.",12,GREEN,true));gap(bottom,9);
            JPanel options=new JPanel(new GridLayout(0,2,7,7));options.setOpaque(false);
            for(Scene.Token token:service.catalog().scene(q.beforeScene()).tokens()) {
                String copy=token.player()==null?html(token.label()):html(token.player().displayName())+"<br>"+html(token.label());
                ActionButton b=button("<html><center>"+copy+"</center></html>",false,()->chooseClue(token.id()));b.setFont(font(11,true));b.setBorder(new EmptyBorder(8,5,8,5));
                b.setName("clue-"+token.id());b.getAccessibleContext().setAccessibleName(token.displayLabel());
                if(token.player()!=null)b.teamColor(new Color(token.player().team().color()));
                b.mark(session.clue().equals(token.id()));options.add(b);clueButtons.put(token.id(),b);
            }Theme.add(bottom,options);field.selected(session.clue());
        }else {
            RoundedPanel note=new RoundedPanel(PALE,12);note.setLayout(new BorderLayout());note.setBorder(new EmptyBorder(14,14,14,14));
            JTextArea copy=text(session.stage()==QuizSession.Stage.FEEDBACK?"노란 테두리의 단서가 판정을 바꿉니다. 장면을 바꾸며 조건을 비교해 보세요.":"장면 1과 장면 2를 번갈아 보세요. 그림은 상황을 단순화한 도식이며, 공의 보유·접촉 시점은 오른쪽 설명을 함께 확인해요.",12,MUTED);copy.setRows(3);note.add(copy);Theme.add(bottom,note);
        }card.add(bottom,BorderLayout.SOUTH);return card;
    }
    private JPanel questionCard(Question q) {
        RoundedPanel card=card();card.setBorder(new EmptyBorder(20,22,20,22));card.setLayout(new BorderLayout(0,12));
        JPanel body=column();Theme.add(body,label(service.catalog().rule(q.ruleId()).topic()+"  ·  "+q.level(),11,GREEN,true));gap(body,12);
        if(session.stage()!=QuizSession.Stage.FEEDBACK) {
            Theme.add(body,copy(q.situation(),14,INK,q.hasImage()&&!q.requiresClue()?2:4));gap(body,15);
        }
        boolean judge=session.stage()==QuizSession.Stage.JUDGE;
        if(session.stage()==QuizSession.Stage.FEEDBACK) {
            Attempt a=session.results().get(session.results().size()-1);
            Theme.add(body,label(a.understood()?"좋은 판정이에요!":"결정적인 조건을 확인해요.",21,a.understood()?GREEN:RED,true));gap(body,12);
            Theme.add(body,label("답 "+(a.answerCorrect()?"정답":"오답")+(q.requiresClue()?"   /   단서 "+(a.clueCorrect()?"정답":"오답"):"")+(a.hintUsed()?"   /   힌트 사용":""),12,MUTED,true));gap(body,13);
            Theme.add(body,copy(q.explanation(),14,INK,3));gap(body,10);
            if(!q.misconception().isBlank()){Theme.add(body,copy(q.misconception(),13,MUTED,2));gap(body,10);}
            Theme.add(body,copy("정답: "+q.choices().get(q.answer()),13,GREEN,1));gap(body,7);
            if(q.requiresClue()) {
            String clue=service.catalog().clues(q).stream().filter(t->t.id().equals(q.clueTarget())).findFirst().orElseThrow().text();
            Theme.add(body,copy("핵심 단서: "+clue,13,GREEN,1));
            }
            primaryAction=button(session.index()+1==session.size()?"미션 결과 보기   →":"다음 상황으로   →",true,()->{
                try { service.advance(session);before=true;if(session.stage()==QuizSession.Stage.COMPLETE)complete.run();else render(); }
                catch(Exception e) { error.accept(e); }
            });
        }else {
            JTextArea prompt=copy(judge?q.prompt():q.cluePrompt(),18,INK,2);prompt.setFont(font(18,true));Theme.add(body,prompt);gap(body,10);
            if(judge) {
                for(int i=0;i<q.choices().size();i++) {
                    final int selected=i;
                    ActionButton option=button("<html>"+(i+1)+" &nbsp; "+html(q.choices().get(i))+"</html>",false,()->{
                        session.choose(selected);choiceButtons.forEach((id,b)->b.mark(id==selected));submit.setEnabled(true);
                    });option.setName("choice-"+i);option.setHorizontalAlignment(SwingConstants.LEFT);
                    int h=Math.max(56,24+22*(int)Math.ceil(option.getFontMetrics(option.getFont()).stringWidth(q.choices().get(i))/240.0));
                    option.setPreferredSize(new Dimension(300,h));option.setMinimumSize(new Dimension(0,h));option.setMaximumSize(new Dimension(Integer.MAX_VALUE,h));
                    option.mark(session.choice()==i);choiceButtons.put(i,option);Theme.add(body,option);gap(body,9);
                }
            }else {
                RoundedPanel chosen=new RoundedPanel(PALE,12);chosen.setLayout(new BorderLayout());chosen.setBorder(new EmptyBorder(12,12,12,12));
                chosen.add(copy("내 판정: "+q.choices().get(session.choice()),13,GREEN,1));Theme.add(body,chosen);gap(body,13);
                chosen.setMaximumSize(new Dimension(Integer.MAX_VALUE,chosen.getPreferredSize().height));
                String picked=session.clue().isBlank()?"왼쪽 장면에서 단서를 선택하세요.":"선택한 단서: "+service.catalog().clues(q).stream().filter(t->t.id().equals(session.clue())).findFirst().orElseThrow().text();
                Theme.add(body,copy(picked,13,MUTED,2));gap(body,12);
                Theme.add(body,text("판정과 단서를 함께 제출하면 해설이 열립니다.",12,MUTED));
            }
            gap(body,18);
            ActionButton hint=button(session.hintUsed()?"힌트 다시 보기":"막혔다면, 힌트 보기",false,()->{
                session.useHint();JOptionPane.showMessageDialog(this,q.hint(),"코치의 힌트",JOptionPane.INFORMATION_MESSAGE);
                message.accept("힌트를 사용했습니다. 이 판단은 다음 도전에서 다시 연습해요.");render();
            });hint.setFont(font(12,false));Theme.add(body,hint);
            submit=button(judge?(q.requiresClue()?"판정 선택 완료 · 단서 찾기   →":"답 제출하기   →"):"판정과 단서 제출하기   →",true,()->{
                try{
                    if(judge&&q.requiresClue()){session.beginClue();before=true;render();}
                    else {service.submit(session);message.accept("학습 기록을 저장했습니다.");render();}
                }catch(Exception e){error.accept(e);}
            });submit.setEnabled(judge?session.choice()>=0:!session.clue().isBlank());primaryAction=submit;
        }
        card.add(body,BorderLayout.CENTER);return card;
    }
    private JTextArea copy(String value,int size,Color color,int minimumRows) {
        JTextArea area=text(value,size,color);
        // 최소 창에서도 긴 상황과 질문을 잘라 버리지 않도록 본문 높이를 확보한다.
        FontMetrics metrics=area.getFontMetrics(font(size,true));int rows=0;
        for(String line:value.split("\n",-1))rows+=Math.max(1,(int)Math.ceil(metrics.stringWidth(line)/240.0));
        area.setRows(Math.max(minimumRows,rows));
        area.setMinimumSize(new Dimension(0,area.getRows()*metrics.getHeight()));return area;
    }
    private void setScene(boolean showBefore) {
        if(session.stage()==QuizSession.Stage.CLUE && !showBefore) {
            message.accept("단서는 장면 1에서 찾아주세요. 판정 순간은 단서 제출 후 다시 볼 수 있어요.");return;
        }
        before=showBefore;Question q=session.current();Scene scene=service.catalog().scene(before?q.beforeScene():q.afterScene());field.scene(scene);
        if(before)field.selected(session.stage()==QuizSession.Stage.FEEDBACK?q.clueTarget():session.clue());
        ((ActionButton)field.getClientProperty("first")).mark(before);((ActionButton)field.getClientProperty("second")).mark(!before);
        caption.setText("<html>"+html(scene.caption())+"</html>");
    }
    private void chooseClue(String id) {
        session.chooseClue(id);render();
    }
}
