package baseup.ui;

import baseup.model.Attempt;
import baseup.model.Question;
import baseup.model.RuleConcept;
import baseup.service.LearningService;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JProgressBar;
import javax.swing.table.DefaultTableModel;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import static baseup.ui.Theme.*;
import static baseup.ui.PageComponents.*;

/** 규칙별 진도와 제출 기록 화면. */
public final class RecordsPanel extends JPanel {
    private final LearningService service;

    public RecordsPanel(LearningService service) {
        super(new BorderLayout());
        this.service = service;
        setOpaque(false);
        add(build(), BorderLayout.CENTER);
    }

    private JComponent build() {
        JPanel p=page("MY SCOREBOOK","결과보다, 이해의 깊이.","제출한 판정과 단서, 힌트 사용 기록이 사용자별로 남습니다.");
        Theme.add(p,new StatisticsPanel(service));
        gap(p,24);
        Theme.add(p,label("규칙별 독립 해결",19,INK,true));
        gap(p,16);
        JPanel rules=new JPanel(new GridLayout(0,3,12,12));
        rules.setOpaque(false);
        for(RuleConcept r:service.catalog().rules().stream().filter(r->service.discovered(r.id())).toList()) {
            RoundedPanel c=card();
            c.setLayout(new BorderLayout(0,12));
            c.add(label(r.title(),14,INK,true),BorderLayout.NORTH);
            JProgressBar bar=new JProgressBar(0,(int)service.questionCount(r.id()));
            bar.setValue(service.mastery(r.id()));
            bar.setForeground(GREEN);
            bar.setBackground(PALE);
            bar.setBorderPainted(false);
            bar.setStringPainted(true);
            bar.setString(service.mastery(r.id())+" / "+service.questionCount(r.id()));
            c.add(bar);
            rules.add(c);
        }
        Theme.add(p,rules);
        gap(p,24);
        Theme.add(p,label("최근 제출 기록",19,INK,true));
        gap(p,14);
        DefaultTableModel model=new DefaultTableModel(new String[]{
            "풀이 시각","상황","판정","단서","힌트","시간"
        },0){
            @Override public boolean isCellEditable(int r,int c){
                return false;
            }
        };
        List<Attempt> history=new ArrayList<>(service.attempts());
        Collections.reverse(history);
        DateTimeFormatter date=DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault());
        for(Attempt a:history) model.addRow(new Object[]{
            date.format(a.at()),service.catalog().question(a.questionId()).map(Question::title).orElse("삭제된 문항 "+a.questionId()),
            a.answerCorrect()?"정답":"다시 보기",a.clueRequired()?(a.clueCorrect()?"정답":"다시 보기"):"해당 없음",a.hintUsed()?"사용":"없음",a.seconds()+"초"
        });
        JTable table=new JTable(model);
        table.setRowHeight(36);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0,0));
        table.getTableHeader().setFont(font(12,true));
        table.getTableHeader().setBackground(PALE);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(1).setPreferredWidth(230);
        table.setAutoCreateRowSorter(true);
        JScrollPane scroll=scroll(table);
        scroll.setPreferredSize(new Dimension(850,260));
        Theme.add(p,scroll);
        if(history.isEmpty()){
            gap(p,12);
            Theme.add(p,label("아직 제출 기록이 없습니다. 첫 미션에 도전해 보세요.",13,MUTED,false));
        }
        return scroll(p);
    }
}
