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

/** 나의 메모: 규칙별 메모를 검색·추가·수정·삭제한다. */
final class NotesScreen extends Screen {
    NotesScreen(MainFrame app) { super(app); }

    @Override JComponent build() {
        JPanel p=page("MY FIELD NOTES","내 말로 남기는 규칙.","원할 때만 남기는 개인 메모예요. 다음 단계로 가기 위해 작성할 필요는 없어요.");
        JPanel controls=new JPanel(new BorderLayout(12,0));controls.setOpaque(false);JTextField search=input("메모 내용이나 규칙 이름 검색");
        controls.setMaximumSize(new Dimension(Integer.MAX_VALUE,48));
        controls.add(search,BorderLayout.CENTER);controls.add(button("+ 메모 추가",true,()->noteDialog(null)),BorderLayout.EAST);Theme.add(p,controls);gap(p,20);
        JPanel results=column();Theme.add(p,results);
        Runnable refresh=()->{
            results.removeAll();List<StudyNote> list=service.notes(search.getText());
            if(list.isEmpty())Theme.add(results,label("메모가 없습니다. 기억하고 싶은 규칙을 내 말로 적어보세요.",14,MUTED,false));
            for(StudyNote note:list) {
                RoundedPanel c=card();c.setLayout(new BorderLayout(20,12));c.add(label(service.catalog().rule(note.ruleId()).title(),15,GREEN,true),BorderLayout.NORTH);
                JTextArea body=paragraph(note.text(),Math.min(15,Math.max(2,(note.text().length()+49)/50+note.text().split("\n").length)));
                c.add(body,BorderLayout.CENTER);c.add(button("삭제",false,()->{
                    if(JOptionPane.showConfirmDialog(app,"이 메모를 삭제할까요?","메모 삭제",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)
                        try{service.deleteNote(note.id());app.message("메모를 삭제했습니다.");app.showPage("notes");}catch(Exception e){app.error(e);}
                }),BorderLayout.EAST);Theme.add(results,c);gap(results,12);
            }results.revalidate();results.repaint();
        };listen(search,refresh);refresh.run();return scroll(p);
    }
    void noteDialog(RuleConcept initial) {
        List<RuleConcept> discovered=service.catalog().rules().stream().filter(r->service.discovered(r.id())).toList();
        if(discovered.isEmpty()){app.message("먼저 첫 단계에 도전해 규칙을 발견해 보세요.");return;}
        JComboBox<RuleConcept> rule=new JComboBox<>(discovered.toArray(RuleConcept[]::new));if(initial!=null)rule.setSelectedItem(initial);
        JTextArea body=new JTextArea(7,36);body.setLineWrap(true);body.setWrapStyleWord(true);body.setFont(font(14,false));
        JPanel form=new JPanel(new BorderLayout(0,12));form.add(rule,BorderLayout.NORTH);form.add(new JScrollPane(body));
        form.add(label("최대 500자 · 파일에 저장되어 다음 실행에도 남아요.",12,MUTED,false),BorderLayout.SOUTH);
        while(JOptionPane.showConfirmDialog(app,form,"내 말로 메모하기",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION) {
            try {service.addNote(((RuleConcept)rule.getSelectedItem()).id(),body.getText());app.message("메모를 저장했습니다.");app.showPage("notes");return;}
            catch(Exception e){app.error(e);}
        }
    }
}
