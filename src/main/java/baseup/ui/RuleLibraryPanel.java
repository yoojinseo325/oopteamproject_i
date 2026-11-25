package baseup.ui;

import baseup.model.RuleCategory;
import baseup.model.RuleConcept;
import baseup.service.LearningService;
import baseup.service.RuleEncyclopedia;
import baseup.service.RuleEncyclopedia.MatchField;
import baseup.service.RuleEncyclopedia.SearchHit;
import baseup.ui.Theme.ActionButton;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JList;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static baseup.ui.Theme.*;
import static baseup.ui.PageComponents.*;

/**
 * 야구 규칙 백과사전 화면. 문제 풀이 진행과 관계없이 전체 규칙을 분류별로 보고 검색한다.
 * 분류·검색 로직은 RuleEncyclopedia에 있고, 이 클래스는 화면 배치만 담당한다.
 */
public final class RuleLibraryPanel extends JPanel {
    private static final String ALL = "전체 분류";
    private final LearningService service;
    private final ScreenActions actions;
    private final RuleEncyclopedia book;
    private final String initialRuleId;

    public RuleLibraryPanel(LearningService service, ScreenActions actions) { this(service, actions, null); }

    /** initialRuleId가 있으면 그 규칙을 선택한 상태로 연다(홈 검색에서 규칙을 눌렀을 때). */
    public RuleLibraryPanel(LearningService service, ScreenActions actions, String initialRuleId) {
        super(new BorderLayout());
        this.service = service;
        this.actions = actions;
        this.book = new RuleEncyclopedia(service.catalog());
        this.initialRuleId = initialRuleId;
        setOpaque(false);
        add(build(), BorderLayout.CENTER);
    }

    private JComponent build() {
        JPanel outer=new JPanel(new BorderLayout(0,20));
        outer.setOpaque(false);
        outer.setBorder(new EmptyBorder(28,30,28,30));
        JPanel top=column();
        Theme.add(top,label("RULE ENCYCLOPEDIA",11,GREEN,true));
        gap(top,8);
        Theme.add(top,label("야구 규칙 백과사전",28,INK,true));
        gap(top,6);
        Theme.add(top,label("문제를 풀지 않아도 모든 규칙을 볼 수 있어요. 별명이나 영어(balk)로도 검색돼요.",13,MUTED,false));
        gap(top,18);

        JPanel filters=new JPanel(new BorderLayout(12,0));
        filters.setOpaque(false);
        JComboBox<Object> category=new JComboBox<>();
        category.addItem(ALL);
        book.categories().forEach(category::addItem);
        category.setFont(font(13,false));
        category.setPreferredSize(new Dimension(150,38));
        filters.add(category,BorderLayout.WEST);
        JTextField search=input("규칙 이름, 별명, 규칙 번호 검색  예) 인필드플라이, 보크, 6.02");
        filters.add(search,BorderLayout.CENTER);
        JCheckBox only=new JCheckBox("북마크만");
        only.setOpaque(false);
        filters.add(only,BorderLayout.EAST);
        Theme.add(top,filters);
        gap(top,8);
        JLabel status=label(" ",12,MUTED,false);
        Theme.add(top,status);
        outer.add(top,BorderLayout.NORTH);

        // 검색어가 맞은 위치(제목/키워드/설명...)를 목록에 보여 주기 위해 보관한다.
        Map<String,MatchField> matched=new HashMap<>();
        DefaultListModel<RuleConcept> model=new DefaultListModel<>();
        JList<RuleConcept> list=new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(78);
        list.setBackground(WHITE);
        list.setSelectionBackground(PALE);
        list.setSelectionForeground(INK);
        list.setCellRenderer((items,value,index,selected,focus)->{
            StringBuilder sub=new StringBuilder(value.category().label());
            if(service.discovered(value.id())) sub.append("  ·  학습함");
            MatchField field=matched.get(value.id());
            if(field!=null && field!=MatchField.NONE && field!=MatchField.TITLE) sub.append("  ·  ").append(field.label()).append("에서 찾음");
            JLabel l=new JLabel("<html><b>"+html(value.title())+"</b><br><font color='#697975'>"+html(sub.toString())+"</font></html>");
            l.setOpaque(true);
            l.setBackground(selected?PALE:WHITE);
            l.setForeground(INK);
            l.setBorder(new EmptyBorder(12,17,12,14));
            return l;
        });
        JScrollPane index=scroll(list);
        index.setPreferredSize(new Dimension(250,500));
        JPanel detail=new JPanel(new BorderLayout());
        detail.setOpaque(false);
        JPanel body=new JPanel(new BorderLayout(20,0));
        body.setOpaque(false);
        body.add(index,BorderLayout.WEST);
        body.add(detail,BorderLayout.CENTER);
        outer.add(body,BorderLayout.CENTER);

        Runnable[] render=new Runnable[1];
        Runnable filter=()->{
            String keep=list.getSelectedValue()==null?null:list.getSelectedValue().id();
            RuleCategory selectedCategory=category.getSelectedItem() instanceof RuleCategory c?c:null;
            List<SearchHit> hits=book.searchHits(search.getText(),selectedCategory).stream()
                    .filter(h->!only.isSelected()||service.bookmarked(h.rule().id())).toList();
            matched.clear();
            model.clear();
            for(SearchHit h:hits){
                matched.put(h.rule().id(),h.field());
                model.addElement(h.rule());
            }
            boolean searching=!search.getText().isBlank();
            status.setText(searching?"'"+search.getText().strip()+"' 검색 결과 "+hits.size()+"개 · 관련도 순"
                    :(selectedCategory==null?"전체 "+hits.size()+"개 규칙":selectedCategory.label()+" · "+selectedCategory.description()+" · "+hits.size()+"개"));
            int restore=-1;
            for(int i=0;i<model.size();i++) if(model.get(i).id().equals(keep)) restore=i;
            // 검색어가 바뀌면 가장 관련도 높은 규칙을, 아니면 보던 규칙을 그대로 보여 준다.
            if(!model.isEmpty()) list.setSelectedIndex(searching||restore<0?0:restore);
            else list.clearSelection();
            render[0].run();
        };
        render[0]=()->{
            detail.removeAll();
            RuleConcept r=list.getSelectedValue();
            if(r==null) detail.add(label("<html>검색 결과가 없어요.<br>다른 이름이나 짧은 단어로 검색해 보세요.</html>",14,MUTED,false));
            else {
                // 책처럼 넘겨보기(교수님 피드백 6): 지금 목록(분류·검색 결과) 안에서 한 쪽씩 넘긴다. ←/→ 키도 된다.
                int at=list.getSelectedIndex(), pages=model.size();
                JPanel pager=new JPanel(new BorderLayout(12,0));
                pager.setOpaque(false);
                pager.setBorder(new EmptyBorder(0,0,12,0));
                ActionButton prev=button("←  이전 쪽",false,()->list.setSelectedIndex(at-1));
                prev.setEnabled(at>0);
                prev.setName("page-prev");
                ActionButton next=button("다음 쪽  →",true,()->list.setSelectedIndex(at+1));
                next.setEnabled(at<pages-1);
                next.setName("page-next");
                JLabel page=label((at+1)+"  /  "+pages+" 쪽   ·   "+r.category().label(),14,MUTED,true);
                page.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                pager.add(prev,BorderLayout.WEST);
                pager.add(page,BorderLayout.CENTER);
                pager.add(next,BorderLayout.EAST);
                detail.add(pager,BorderLayout.NORTH);
            }
            if(r!=null) detail.add(scroll(ruleCard(r,related->{
                // 관련 규칙이 현재 목록에 없으면 필터를 풀고 이동한다.
                if(!model.contains(related)){
                    search.setText("");
                    only.setSelected(false);
                    category.setSelectedItem(ALL);
                    filter.run();
                }
                list.setSelectedValue(related,true);
            })));
            detail.revalidate();
            detail.repaint();
        };
        listen(search,filter);
        only.addActionListener(e->filter.run());
        category.addActionListener(e->filter.run());
        list.addListSelectionListener(e->{
            if(!e.getValueIsAdjusting())render[0].run();
        });
        // ←/→ 키로 쪽 넘기기(검색창에 글을 쓰는 중이 아닐 때).
        javax.swing.InputMap keys=outer.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        keys.put(javax.swing.KeyStroke.getKeyStroke("LEFT"),"page-prev");
        keys.put(javax.swing.KeyStroke.getKeyStroke("RIGHT"),"page-next");
        outer.getActionMap().put("page-prev",new javax.swing.AbstractAction(){
            public void actionPerformed(java.awt.event.ActionEvent e){ if(!search.hasFocus()&&list.getSelectedIndex()>0) list.setSelectedIndex(list.getSelectedIndex()-1); }
        });
        outer.getActionMap().put("page-next",new javax.swing.AbstractAction(){
            public void actionPerformed(java.awt.event.ActionEvent e){ if(!search.hasFocus()&&list.getSelectedIndex()<model.size()-1) list.setSelectedIndex(list.getSelectedIndex()+1); }
        });
        filter.run();
        book.find(initialRuleId==null?"":initialRuleId).ifPresent(r->list.setSelectedValue(r,true));
        return outer;
    }

    private RoundedPanel ruleCard(RuleConcept r, java.util.function.Consumer<RuleConcept> open) {
        RoundedPanel card=card();
        card.setLayout(new BorderLayout(0,18));
        JPanel info=column();
        Theme.add(info,label(r.category().label()+"  /  "+r.topic(),11,GREEN,true));
        gap(info,16);
        Theme.add(info,label(r.title(),28,INK,true));
        gap(info,14);
        JTextArea summary=paragraph(r.summary(),2);
        summary.setForeground(GREEN);
        Theme.add(info,summary);
        gap(info,14);
        Theme.add(info,paragraph(r.detail(),7));
        gap(info,18);
        long questions=service.questionCount(r.id());
        Theme.add(info,label(questions==0?"아직 연결된 문제가 없는 규칙입니다."
                :"독립 해결   "+service.mastery(r.id())+" / "+questions+" 상황",13,GREEN,true));
        gap(info,12);
        if(!r.keywords().isEmpty()){
            Theme.add(info,label("<html>함께 쓰는 말: "+html(String.join(", ",r.keywords()))+"</html>",12,MUTED,false));
            gap(info,12);
        }
        Theme.add(info,paragraph("확인 출처: "+r.source()+"\n학습을 위해 단순화한 설명입니다.",3));
        List<RuleConcept> related=book.related(r.id(),3);
        if(!related.isEmpty()){
            gap(info,14);
            Theme.add(info,label("같이 보면 좋은 규칙",12,INK,true));
            gap(info,8);
            JPanel links=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));
            links.setOpaque(false);
            for(RuleConcept other:related) links.add(button(other.title(),false,()->open.accept(other)));
            Theme.add(info,links);
        }
        card.add(info,BorderLayout.CENTER);
        JPanel buttons=new JPanel(new GridLayout(0,1,0,10));
        buttons.setOpaque(false);
        buttons.add(button(service.bookmarked(r.id())?"북마크 해제":"북마크에 추가",false,()->{
            try{
                service.toggleBookmark(r.id());
                actions.message("북마크를 저장했습니다.");
                actions.showPage("library");
            }
            catch(Exception e){
                actions.error(e);
            }
        }));
        buttons.add(button("내 말로 메모하기",false,()->actions.editNote(r)));
        card.add(buttons,BorderLayout.SOUTH);
        return card;
    }
}
