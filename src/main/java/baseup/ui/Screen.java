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

/**
 * 모든 화면(페이지)의 부모 클래스.
 * 화면마다 build()로 자기 화면을 만들고, 페이지 이동·메시지 같은 공통 기능은 MainFrame(app)에 부탁한다.
 */
abstract class Screen {
    protected final MainFrame app;
    protected final LearningService service;

    Screen(MainFrame app) { this.app = app; this.service = app.service(); }

    /** 이 화면을 만들어서 돌려준다. */
    abstract JComponent build();

    static JPanel page(String kicker,String title,String subtitle) {
        JPanel p=column(); p.setBorder(new EmptyBorder(28,30,28,30));
        Theme.add(p,label(kicker,11,GREEN,true));gap(p,9);Theme.add(p,label(title,29,INK,true));gap(p,9);
        Theme.add(p,label(subtitle,14,MUTED,false));gap(p,24);return p;
    }
    static JTextArea paragraph(String value,int rows) { JTextArea a=text(value,14,MUTED);a.setRows(rows);return a; }
    static void listen(JTextField field,Runnable update) {
        field.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){update.run();}public void removeUpdate(DocumentEvent e){update.run();}public void changedUpdate(DocumentEvent e){update.run();}});
    }
}
