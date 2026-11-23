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

/** 발견한 규칙: 배운 규칙을 검색하고, 북마크하고, 메모를 남긴다. */
final class LibraryScreen extends Screen {
    LibraryScreen(MainFrame app) { super(app); }

    @Override JComponent build() {
        JPanel outer=new JPanel(new BorderLayout(0,20));outer.setOpaque(false);outer.setBorder(new EmptyBorder(28,30,28,30));
        JPanel top=column();Theme.add(top,label("RULEBOOK",11,GREEN,true));gap(top,8);
        Theme.add(top,label("플레이하며 발견한 규칙.",28,INK,true));gap(top,18);
        JPanel filters=new JPanel(new BorderLayout(12,0));filters.setOpaque(false);
        JTextField search=input("규칙 이름이나 설명 검색");filters.add(search,BorderLayout.CENTER);
        JCheckBox only=new JCheckBox("북마크만");only.setOpaque(false);filters.add(only,BorderLayout.EAST);Theme.add(top,filters);
        outer.add(top,BorderLayout.NORTH);
        DefaultListModel<RuleConcept> model=new DefaultListModel<>();JList<RuleConcept> list=new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);list.setFixedCellHeight(78);
        list.setBackground(WHITE);list.setSelectionBackground(PALE);list.setSelectionForeground(INK);
        list.setCellRenderer((items,value,index,selected,focus)->{
            JLabel l=new JLabel("<html><b>"+html(value.title())+"</b><br><font color='#697975'>"+html(value.topic())+"</font></html>");
            l.setOpaque(true);l.setBackground(selected?PALE:WHITE);l.setForeground(INK);l.setBorder(new EmptyBorder(12,17,12,14));return l;
        });
        JScrollPane index=scroll(list);index.setPreferredSize(new Dimension(235,500));
        JPanel detail=new JPanel(new BorderLayout());detail.setOpaque(false);
        JPanel body=new JPanel(new BorderLayout(20,0));body.setOpaque(false);body.add(index,BorderLayout.WEST);body.add(detail,BorderLayout.CENTER);outer.add(body,BorderLayout.CENTER);
        Runnable render=()->{
            detail.removeAll();RuleConcept r=list.getSelectedValue();
            if(r==null) detail.add(label("<html>아직 발견한 규칙이 없거나 검색 결과가 없어요.<br>단계에서 판정을 제출하면 이곳에 규칙이 열립니다.</html>",14,MUTED,false));
            else {
                RoundedPanel card=card();card.setLayout(new BorderLayout(0,18));JPanel info=column();
                Theme.add(info,label(r.topic()+"  /  RULE CARD",11,GREEN,true));gap(info,16);Theme.add(info,label(r.title(),28,INK,true));gap(info,14);
                JTextArea summary=paragraph(r.summary(),2);summary.setForeground(GREEN);Theme.add(info,summary);gap(info,14);
                Theme.add(info,paragraph(r.detail(),7));gap(info,18);
                Theme.add(info,label("독립 해결   "+service.mastery(r.id())+" / "+service.questionCount(r.id())+" 상황",13,GREEN,true));gap(info,16);
                Theme.add(info,paragraph("확인 출처: "+r.source()+"\n학습을 위해 단순화한 상황입니다.",3));
                card.add(info,BorderLayout.CENTER);
                JPanel actions=new JPanel(new GridLayout(0,1,0,10));actions.setOpaque(false);
                actions.add(button(service.bookmarked(r.id())?"북마크 해제":"북마크에 추가",false,()->{
                    try{service.toggleBookmark(r.id());app.message("북마크를 저장했습니다.");app.showPage("library");}catch(Exception e){app.error(e);}
                }));
                actions.add(button("내 말로 메모하기",false,()->new NotesScreen(app).noteDialog(r)));card.add(actions,BorderLayout.SOUTH);detail.add(scroll(card));
            }
            detail.revalidate();detail.repaint();
        };
        Runnable filter=()->{model.clear();service.catalog().searchRules(search.getText()).stream().filter(r->service.discovered(r.id())).filter(r->!only.isSelected()||service.bookmarked(r.id())).forEach(model::addElement);if(!model.isEmpty())list.setSelectedIndex(0);render.run();};
        listen(search,filter);only.addActionListener(e->filter.run());list.addListSelectionListener(e->{if(!e.getValueIsAdjusting())render.run();});filter.run();
        if(app.pendingRule!=null){for(int i=0;i<model.size();i++)if(model.get(i).id().equals(app.pendingRule))list.setSelectedIndex(i);app.pendingRule=null;}
        return outer;
    }
}
