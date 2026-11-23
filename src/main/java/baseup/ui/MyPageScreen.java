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

/** 마이페이지: 나의 성장 · 발견한 규칙 · 나의 메모를 탭으로 묶는다. */
final class MyPageScreen extends Screen {
    /** 마이페이지: 팀원이 만든 나의 성장·발견한 규칙·나의 메모 화면을 탭으로 묶는다. */
    private final String tab;
    MyPageScreen(MainFrame app, String tab) { super(app); this.tab = tab; }
    @Override JComponent build() {
        JPanel p=new JPanel(new BorderLayout());p.setOpaque(false);p.setBorder(new EmptyBorder(18,24,10,24));
        JPanel head=new JPanel(new BorderLayout());head.setOpaque(false);head.setBorder(new EmptyBorder(0,6,12,6));
        head.add(label(service.user().nickname()+"님의 라커룸   ·   "+service.rank()+"  "+service.xp()+" XP",18,INK,true),BorderLayout.WEST);
        head.add(button("프로필 바꾸기",false,app::profilesDialog),BorderLayout.EAST);p.add(head,BorderLayout.NORTH);
        JTabbedPane tabs=new JTabbedPane();tabs.setFont(Theme.font(14,true));
        tabs.addTab("나의 성장",new RecordsScreen(app).build());tabs.addTab("발견한 규칙",new LibraryScreen(app).build());tabs.addTab("나의 메모",new NotesScreen(app).build());
        tabs.setSelectedIndex(switch(tab){case "library"->1;case "notes"->2;default->0;});
        p.add(tabs,BorderLayout.CENTER);return p;
    }
}
