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

/** 나의 성장: 완료한 단계, 스스로 해결한 상황, XP, 최근 제출 기록. */
final class RecordsScreen extends Screen {
    RecordsScreen(MainFrame app) { super(app); }

    @Override JComponent build() {
        JPanel p=page("MY SCOREBOOK","결과보다, 이해의 깊이.","제출한 판정과 단서, 힌트 사용 기록이 사용자별로 남습니다.");Theme.add(p,stats());gap(p,24);
        Theme.add(p,label("규칙별 독립 해결",19,INK,true));gap(p,16);
        JPanel rules=new JPanel(new GridLayout(0,3,12,12));rules.setOpaque(false);
        for(RuleConcept r:service.catalog().rules().stream().filter(r->service.discovered(r.id())).toList()) {
            RoundedPanel c=card();c.setLayout(new BorderLayout(0,12));c.add(label(r.title(),14,INK,true),BorderLayout.NORTH);
            JProgressBar bar=new JProgressBar(0,(int)service.questionCount(r.id()));bar.setValue(service.mastery(r.id()));
            bar.setForeground(GREEN);bar.setBackground(PALE);bar.setBorderPainted(false);bar.setStringPainted(true);bar.setString(service.mastery(r.id())+" / "+service.questionCount(r.id()));c.add(bar);rules.add(c);
        }Theme.add(p,rules);gap(p,24);Theme.add(p,label("최근 제출 기록",19,INK,true));gap(p,14);
        DefaultTableModel model=new DefaultTableModel(new String[]{"풀이 시각","상황","판정","단서","힌트","시간"},0){@Override public boolean isCellEditable(int r,int c){return false;}};
        List<Attempt> history=new ArrayList<>(service.attempts());Collections.reverse(history);
        DateTimeFormatter date=DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault());
        for(Attempt a:history) model.addRow(new Object[]{date.format(a.at()),service.catalog().question(a.questionId()).map(Question::title).orElse("삭제된 문항 "+a.questionId()),
                a.answerCorrect()?"정답":"다시 보기",a.clueRequired()?(a.clueCorrect()?"정답":"다시 보기"):"해당 없음",a.hintUsed()?"사용":"없음",a.seconds()+"초"});
        JTable table=new JTable(model);table.setRowHeight(36);table.setShowGrid(false);table.setIntercellSpacing(new Dimension(0,0));
        table.getTableHeader().setFont(font(12,true));table.getTableHeader().setBackground(PALE);table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(1).setPreferredWidth(230);table.setAutoCreateRowSorter(true);
        JScrollPane scroll=scroll(table);scroll.setPreferredSize(new Dimension(850,260));Theme.add(p,scroll);
        if(history.isEmpty()){gap(p,12);Theme.add(p,label("아직 제출 기록이 없습니다. 첫 미션에 도전해 보세요.",13,MUTED,false));}
        return scroll(p);
    }
    JPanel stats() {
        JPanel row=new JPanel(new GridLayout(1,3,15,0)); row.setOpaque(false);
        row.add(stat("완료한 단계",service.completedSteps()+" / "+service.steps().size(),"한 단계씩 열리는 나의 경로"));
        row.add(stat("스스로 해결한 상황",service.solvedCount()+"개","답과 필요한 단서 정답 · 힌트 없이"));
        row.add(stat("차곡차곡 쌓인 경험",service.xp()+" XP",service.rank()));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE,125));return row;
    }
    JPanel stat(String caption,String number,String sub) {
        RoundedPanel card=card();card.setLayout(new BorderLayout(0,9));card.add(label(caption,12,MUTED,false),BorderLayout.NORTH);
        card.add(label(number,28,INK,true),BorderLayout.CENTER);card.add(label(sub,11,MUTED,false),BorderLayout.SOUTH);return card;
    }
}
