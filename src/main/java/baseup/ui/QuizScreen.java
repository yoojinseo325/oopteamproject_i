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

/** 퀴즈: 다음 STEP 판정 + 틀린 문제 다시 풀기. */
final class QuizScreen extends Screen {
    QuizScreen(MainFrame app) { super(app); }

    /** 비디오 판독: 다음 STEP 판정 + 틀린 문제 다시 판정. */
    @Override JComponent build() {
        JPanel p=page("QUIZ","퀴즈","장면을 보고 심판처럼 판정해 보세요.");
        JPanel cards=new JPanel(new GridLayout(1,2,18,0));cards.setOpaque(false);
        cards.add(quizCard("다음 판정",service.courseComplete()?"모든 STEP을 마쳤어요. 새 조합으로 도전!":"STEP "+service.nextStepNumber()+" 장면을 판정해요.",
                app.nextAction(),true,app::startNext));
        int review=service.reviewQuestions().size();
        cards.add(quizCard("다시 풀기",review==0?"틀린 장면이 없어요. 깔끔한 판정!":"헷갈렸던 장면 "+Math.min(review,4)+"개를 다시 봐요.",
                "다시 풀기   →",false,review==0?null:()->{
                    try{app.showQuiz(service.start(new baseup.service.ReviewSelector()),"다시 풀기");}catch(Exception e){app.error(e);}}));
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE,190));Theme.add(p,cards);
        return scroll(p);
    }
    JComponent quizCard(String title,String body,String action,boolean primary,Runnable run) {
        RoundedPanel card=new RoundedPanel(Color.WHITE,18);card.setLayout(new BorderLayout(0,14));card.setBorder(new EmptyBorder(22,24,22,24));
        JPanel text=column();Theme.add(text,label(title,20,INK,true));gap(text,8);Theme.add(text,label(body,14,MUTED,false));
        card.add(text,BorderLayout.CENTER);
        JButton go=button(action,primary,run==null?()->{}:run);go.setEnabled(run!=null);
        JPanel box=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));box.setOpaque(false);box.add(go);card.add(box,BorderLayout.SOUTH);
        return card;
    }
}
