package baseup.ui;

import baseup.service.LearningService;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import static baseup.ui.Theme.*;

/** 페이지 공통 제목과 문단. 데이터나 탐색 상태를 보관하지 않는다. */
final class PageComponents {
    private PageComponents() {
    }

    static JPanel page(String kicker,String title,String subtitle) {
        JPanel p=column();
        p.setBorder(new EmptyBorder(28,30,28,30));
        Theme.add(p,label(kicker,11,GREEN,true));
        gap(p,9);
        Theme.add(p,label(title,29,INK,true));
        gap(p,9);
        Theme.add(p,label(subtitle,14,MUTED,false));
        gap(p,24);
        return p;
    }

    static JTextArea paragraph(String value,int rows) {
        JTextArea a=text(value,14,MUTED);
        a.setRows(rows);
        return a;
    }

    static String nextAction(LearningService service) {
        return service.courseComplete() ? "새로운 조합으로 도전   →" : "STEP " + service.nextStepNumber() + " 시작하기   →";
    }

    static void listen(JTextField field,Runnable update) {
        field.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent e){
                update.run();
            }
            public void removeUpdate(DocumentEvent e){
                update.run();
            }
            public void changedUpdate(DocumentEvent e){
                update.run();
            }
        });
    }
}
