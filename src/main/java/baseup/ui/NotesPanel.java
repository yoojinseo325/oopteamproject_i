package baseup.ui;

import baseup.model.StudyNote;
import baseup.service.LearningService;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JOptionPane;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.util.List;
import static baseup.ui.Theme.*;
import static baseup.ui.PageComponents.*;

/** 개인 메모 조회/검색/삭제 화면. */
public final class NotesPanel extends JPanel {
    private final LearningService service;
    private final ScreenActions actions;

    public NotesPanel(LearningService service, ScreenActions actions) {
        super(new BorderLayout());
        this.service = service;
        this.actions = actions;
        setOpaque(false);
        add(build(), BorderLayout.CENTER);
    }

    private JComponent build() {
        JPanel p=page("MY FIELD NOTES","내 말로 남기는 규칙.","원할 때만 남기는 개인 메모예요. 다음 단계로 가기 위해 작성할 필요는 없어요.");
        JPanel controls=new JPanel(new BorderLayout(12,0));
        controls.setOpaque(false);
        JTextField search=input("메모 내용이나 규칙 이름 검색");
        controls.setMaximumSize(new Dimension(Integer.MAX_VALUE,48));
        controls.add(search,BorderLayout.CENTER);
        controls.add(button("+ 메모 추가",true,()->actions.editNote(null)),BorderLayout.EAST);
        Theme.add(p,controls);
        gap(p,20);
        JPanel results=column();
        Theme.add(p,results);
        Runnable refresh=()->{
            results.removeAll();
            List<StudyNote> list=service.notes(search.getText());
            if(list.isEmpty())Theme.add(results,label("메모가 없습니다. 기억하고 싶은 규칙을 내 말로 적어보세요.",14,MUTED,false));
            for(StudyNote note:list) {
                RoundedPanel c=card();
                c.setLayout(new BorderLayout(20,12));
                c.add(label(service.catalog().rule(note.ruleId()).title(),15,GREEN,true),BorderLayout.NORTH);
                JTextArea body=paragraph(note.text(),Math.min(15,Math.max(2,(note.text().length()+49)/50+note.text().split("\n").length)));
                c.add(body,BorderLayout.CENTER);
                c.add(button("삭제",false,()->{
                    if(JOptionPane.showConfirmDialog(this,"이 메모를 삭제할까요?","메모 삭제",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)
                    try{
                        service.deleteNote(note.id());
                        actions.message("메모를 삭제했습니다.");
                        actions.showPage("notes");
                    }
                    catch(Exception e){
                        actions.error(e);
                    }
                }),BorderLayout.EAST);
                Theme.add(results,c);
                gap(results,12);
            }
            results.revalidate();
            results.repaint();
        };
        listen(search,refresh);
        refresh.run();
        return scroll(p);
    }
}
