package baseup.ui;

import baseup.model.Question;
import baseup.model.QuizSession;
import baseup.repository.Catalog;
import javax.swing.JPanel;
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
        Theme.add(body,label(catalog.rule(q.ruleId()).topic()+"  ·  "+q.level(),11,GREEN,true));
        gap(body,12);
        Theme.add(body,copy(q.situation(),14,INK,q.hasImage()&&!q.requiresClue()?2:4));
        gap(body,15);
        JTextArea prompt=copy(judge?q.prompt():q.cluePrompt(),18,INK,2);
        prompt.setFont(font(18,true));
        Theme.add(body,prompt);
        gap(body,10);
        if(judge) {
            for(int i=0;i<q.choices().size();i++) {
                final int selected=i;
                ActionButton option=button("<html>"+(i+1)+" &nbsp; "+html(q.choices().get(i))+"</html>",false,()->{
                    session.choose(selected);
                    choiceButtons.forEach((id,b)->b.mark(id==selected));
                    selectionChanged.run();
                });
                option.setName("choice-"+i);
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
            chosen.add(copy("내 판정: "+q.choices().get(session.choice()),13,GREEN,1));
            Theme.add(body,chosen);
            gap(body,13);
            chosen.setMaximumSize(new Dimension(Integer.MAX_VALUE,chosen.getPreferredSize().height));
            String picked=session.clue().isBlank()?"왼쪽 장면에서 단서를 선택하세요.":"선택한 단서: "+catalog.clues(q).stream().filter(t->t.id().equals(session.clue())).findFirst().orElseThrow().text();
            clueText = copy(picked,13,MUTED,2);
            // 선택을 바꿔도 긴 단서 설명이 잘리지 않도록 최대 높이를 확보한다.
            int rows = clueText.getRows();
            for (Question.Clue clue : catalog.clues(q)) {
                rows = Math.max(rows, copy("선택한 단서: " + clue.text(),13,MUTED,2).getRows());
            }
            clueText.setRows(rows);
            Theme.add(body,clueText);
            gap(body,12);
            Theme.add(body,text("판정과 단서를 함께 제출하면 해설이 열립니다.",12,MUTED));
        }
        gap(body,18);
        hint=button(session.hintUsed()?"힌트 다시 보기":"막혔다면, 힌트 보기",false,()->{
            session.useHint();
            JOptionPane.showMessageDialog(this,q.hint(),"코치의 힌트",JOptionPane.INFORMATION_MESSAGE);
            message.accept("힌트를 사용했습니다. 이 판단은 다음 도전에서 다시 연습해요.");
            hint.setText("힌트 다시 보기");
        });
        hint.setFont(font(12,false));
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
