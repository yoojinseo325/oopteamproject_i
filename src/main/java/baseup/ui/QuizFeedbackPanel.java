package baseup.ui;

import baseup.model.Attempt;
import baseup.model.Question;
import baseup.model.QuizSession;
import baseup.repository.Catalog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;

import static baseup.ui.Theme.*;
import static baseup.ui.QuizText.copy;

/** 저장된 제출 결과와 해설만 표시한다. */
final class QuizFeedbackPanel extends JPanel {
    QuizFeedbackPanel(Catalog catalog, QuizSession session) {
        setLayout(new BorderLayout());
        setOpaque(false);
        Question q = session.current();
        RoundedPanel card = card();
        card.setBorder(new EmptyBorder(20,22,20,22));
        card.setLayout(new BorderLayout(0,12));
        JPanel body = column();
        Theme.add(body,label(catalog.rule(q.ruleId()).topic()+"  ·  "+q.level(),13,GREEN,true));
        gap(body,12);
        Attempt a=session.results().get(session.results().size()-1);
        Theme.add(body,label(a.understood()?"좋은 판정이에요!":"결정적인 조건을 확인해요.",24,a.understood()?SUCCESS:RED,true));
        gap(body,12);
        Theme.add(body,label("답 "+(a.answerCorrect()?"정답":"오답")+(q.requiresClue()?"   /   단서 "+(a.clueCorrect()?"정답":"오답"):"")+(a.hintUsed()?"   /   힌트 사용":""),14,MUTED,true));
        gap(body,13);
        Theme.add(body,copy(q.explanation(),17,INK,3));
        gap(body,10);
        if(!q.misconception().isBlank()){
            Theme.add(body,copy(q.misconception(),15,MUTED,2));
            gap(body,10);
        }
        if(!session.typedAnswer().isBlank()) {
            // 단답형: 내가 입력한 답과, 정답으로 인정하는 말을 함께 보여 준다.
            Theme.add(body,copy("내 답: "+session.typedAnswer(),15,a.answerCorrect()?SUCCESS:RED,1));
            gap(body,7);
            Theme.add(body,copy("정답으로 인정하는 말: "+String.join(", ",catalog.shortAnswers(q.id())),15,GREEN,1));
        }
        else Theme.add(body,copy("정답: "+q.choices().get(q.answer()),15,GREEN,1));
        gap(body,7);
        if(q.requiresClue()) {
            String clue=catalog.clues(q).stream().filter(t->t.id().equals(q.clueTarget())).findFirst().orElseThrow().text();
            Theme.add(body,copy("핵심 단서: "+clue,15,GREEN,1));
        }
        card.add(body, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
    }
}
