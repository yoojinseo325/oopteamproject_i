package baseup.ui;

import baseup.model.Question;
import baseup.model.QuizSession;
import baseup.repository.Catalog;
import baseup.service.AnswerChecker;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.JTextArea;
import javax.swing.JOptionPane;
import javax.swing.border.EmptyBorder;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.function.Consumer;
import static baseup.ui.Theme.*;
import static baseup.ui.QuizText.copy;
import static baseup.ui.PageComponents.listen;

/** 판정 선택과 근거 안내. 제출과 화면 단계 전환은 QuizPanel에 알린다. */
final class QuizAnswerPanel extends JPanel {
    private final Catalog catalog;
    private final QuizSession session;
    private final Map<Integer, ActionButton> choiceButtons = new LinkedHashMap<>();
    private JTextArea clueText;
    private ActionButton hint;

    QuizAnswerPanel(Catalog catalog, QuizSession session, Runnable selectionChanged, Consumer<String> message) {
        this.catalog = catalog;
        this.session = session;
        setLayout(new BorderLayout());
        setOpaque(false);
        Question q = session.current();
        boolean judge = session.stage() == QuizSession.Stage.JUDGE;
        RoundedPanel card=card();
        card.setBorder(new EmptyBorder(20,22,20,22));
        card.setLayout(new BorderLayout(0,12));
        JPanel body=column();
        Theme.add(body,label(catalog.rule(q.ruleId()).topic()+"  ·  "+q.level(),13,GREEN,true));
        gap(body,12);
        Theme.add(body,copy(q.situation(),17,INK,q.hasImage()&&!q.requiresClue()?2:4));
        gap(body,15);
        JTextArea prompt=copy(judge?q.prompt():q.cluePrompt(),22,INK,2);
        prompt.setFont(font(22,true));
        Theme.add(body,prompt);
        gap(body,10);
        AnswerChecker checker=new AnswerChecker(catalog);
        if(judge && checker.isShortAnswer(q)) {
            // 단답형(교수님 피드백 8): 보기를 보여 주지 않고 직접 입력한다. 띄어쓰기·영어·별명도 정답으로 인정한다.
            JTextArea guide=copy("직접 입력해서 답해요 · 띄어쓰기나 영어 이름도 괜찮아요",15,ACCENT,2);
            guide.setFont(font(15,true));
            Theme.add(body,guide);
            gap(body,10);
            JTextField answer=input("정답을 입력하세요");
            answer.setName("short-answer");
            answer.setFont(font(20,true));
            answer.setText(session.typedAnswer());
            answer.setMaximumSize(new Dimension(Integer.MAX_VALUE,58));
            listen(answer,()->{
                if(answer.getText().isBlank()) session.clearAnswer();
                else checker.apply(session,answer.getText());
                selectionChanged.run();
            });
            Theme.add(body,answer);
            gap(body,8);
            Theme.add(body,copy("입력한 뒤 아래 '답 제출하기'를 눌러요. 채점은 제출한 뒤에 공개돼요.",14,MUTED,2));
        }
        else if(judge) {
            for(int i=0;i<q.choices().size();i++) {
                final int selected=i;
                ActionButton option=button("<html>"+(i+1)+" &nbsp; "+html(q.choices().get(i))+"</html>",false,()->{
                    session.choose(selected);
                    choiceButtons.forEach((id,b)->b.mark(id==selected));
                    selectionChanged.run();
                });
                option.setName("choice-"+i);
                option.setFont(font(17,true));
                option.setHorizontalAlignment(SwingConstants.LEFT);
                int h=Math.max(56,24+22*(int)Math.ceil(option.getFontMetrics(option.getFont()).stringWidth(q.choices().get(i))/240.0));
                option.setPreferredSize(new Dimension(300,h));
                option.setMinimumSize(new Dimension(0,h));
                option.setMaximumSize(new Dimension(Integer.MAX_VALUE,h));
                option.mark(session.choice()==i);
                choiceButtons.put(i,option);
                Theme.add(body,option);
                gap(body,9);
            }
        }
        else {
            RoundedPanel chosen=new RoundedPanel(PALE,12);
            chosen.setLayout(new BorderLayout());
            chosen.setBorder(new EmptyBorder(12,12,12,12));
            chosen.add(copy("내 판정: "+(session.typedAnswer().isBlank()?q.choices().get(session.choice()):session.typedAnswer()),15,GREEN,1));
            Theme.add(body,chosen);
            gap(body,13);
            chosen.setMaximumSize(new Dimension(Integer.MAX_VALUE,chosen.getPreferredSize().height));
            String picked=session.clue().isBlank()?"왼쪽 장면에서 단서를 선택하세요.":"선택한 단서: "+catalog.clues(q).stream().filter(t->t.id().equals(session.clue())).findFirst().orElseThrow().text();
            clueText = copy(picked,15,MUTED,2);
            // 선택을 바꿔도 긴 단서 설명이 잘리지 않도록 최대 높이를 확보한다.
            int rows = clueText.getRows();
            for (Question.Clue clue : catalog.clues(q)) {
                rows = Math.max(rows, copy("선택한 단서: " + clue.text(),15,MUTED,2).getRows());
            }
            clueText.setRows(rows);
            Theme.add(body,clueText);
            gap(body,12);
            Theme.add(body,text("판정과 단서를 함께 제출하면 해설이 열립니다.",14,MUTED));
        }
        gap(body,18);
        hint=button(session.hintUsed()?"힌트 다시 보기":"막혔다면, 힌트 보기",false,()->{
            session.useHint();
            JOptionPane.showMessageDialog(this,q.hint(),"코치의 힌트",JOptionPane.INFORMATION_MESSAGE);
            message.accept("힌트를 사용했습니다. 이 판단은 다음 도전에서 다시 연습해요.");
            hint.setText("힌트 다시 보기");
        });
        hint.setFont(font(14,false));
        Theme.add(body,hint);
        card.add(body, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
    }

    void clueChanged() {
        String description = catalog.clues(session.current()).stream()
        .filter(clue -> clue.id().equals(session.clue())).findFirst().orElseThrow().text();
        clueText.setText("선택한 단서: " + description);
    }
}
