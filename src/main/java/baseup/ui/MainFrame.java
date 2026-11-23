package baseup.ui;

import baseup.model.*;
import baseup.service.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import static baseup.ui.Theme.*;

/** 탐색과 화면 조립만 담당한다. 데이터 변경은 LearningService에 위임한다. */
public final class MainFrame extends JFrame {
    private final LearningService service;
    private final CardLayout cards = new CardLayout();
    private final JPanel workspace = new JPanel(cards);
    private final Map<String, JButton> navigation = new LinkedHashMap<>();
    private JPanel myPageSubmenu;
    private DashboardStyle.NavButton accountNav;
    private final JLabel location = label("학습 홈", 15, INK, true);
    private final JLabel xp = label("", 13, GREEN, true);
    private final JLabel status = label("학습 준비 완료 · 기록은 이 컴퓨터에 저장됩니다", 12, MUTED, false);
    private final JButton profile;
    private final Runnable logoutAction;
    private QuizSession session;
    private String currentPage = "home";
    private final Deque<String> backHistory = new ArrayDeque<>();
    private final Deque<String> forwardHistory = new ArrayDeque<>();
    private final JButton backButton = new JButton("←");
    private final JButton forwardButton = new JButton("→");
    private AWTEventListener mouseHistoryListener;

    // 기존 테스트와 독립 UI 미리보기 호환용 생성자
    public MainFrame(LearningService service) { this(service, null); }

    public MainFrame(LearningService service, Runnable logoutAction) {
        super("BASEUP · 야구를 읽는 첫걸음 | V1 + V2 통합");
        this.service=service;this.logoutAction=logoutAction;
        this.profile=button("",false,this::handleProfileAction);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { if (leaveQuiz()) { dispose(); System.exit(0); } }
        });
        setLayout(new BorderLayout()); super.add(sidebar(),BorderLayout.WEST);
        JPanel main=new JPanel(new BorderLayout()); main.setBackground(BG);
        JPanel top=new JPanel(new BorderLayout()); top.setBackground(DashboardStyle.CANVAS); top.setBorder(new EmptyBorder(13,30,13,30));
        JPanel breadcrumbs = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        breadcrumbs.setOpaque(false);
        for (JButton arrow : new JButton[]{backButton, forwardButton}) {
            arrow.setFont(font(18, true)); arrow.setFocusPainted(false);
            arrow.setOpaque(false); arrow.setContentAreaFilled(false);
            arrow.setBorder(new EmptyBorder(3, 8, 3, 8));
            arrow.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        backButton.setToolTipText("뒤로가기 (Alt+← / 마우스 뒤로가기 버튼)");
        forwardButton.setToolTipText("앞으로가기 (Alt+→ / 마우스 앞으로가기 버튼)");
        backButton.addActionListener(e -> goBack());
        forwardButton.addActionListener(e -> goForward());
        breadcrumbs.add(backButton); breadcrumbs.add(forwardButton); breadcrumbs.add(location);
        top.add(breadcrumbs,BorderLayout.WEST); top.add(xp,BorderLayout.EAST); main.add(top,BorderLayout.NORTH);
        main.add(workspace,BorderLayout.CENTER);
        JPanel footer=new JPanel(new BorderLayout()); footer.setBackground(BG); footer.setBorder(new EmptyBorder(9,30,12,30));
        footer.add(status,BorderLayout.WEST); footer.add(label("BASEUP / V2.1",11,MUTED,true),BorderLayout.EAST);
        main.add(footer,BorderLayout.SOUTH); super.add(main,BorderLayout.CENTER);
        setMinimumSize(new Dimension(1100,760));
        Dimension screen=Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(1360,screen.width-70),Math.min(940,screen.height-70));
        setLocationRelativeTo(null); showPage("home");
        installHistoryControls();
    }
    private JPanel sidebar() {
        DashboardStyle.Sidebar side=new DashboardStyle.Sidebar();
        side.setLayout(new BorderLayout());
        side.setPreferredSize(new Dimension(236,700));
        side.setBorder(new EmptyBorder(30,18,22,18));
        JPanel body=column();
        Theme.add(body,label("BASEUP",30,WHITE,true));gap(body,6);
        Theme.add(body,label("야구를 읽는 첫걸음",13,new Color(0xB1D1C3),false));gap(body,42);
        Theme.add(body,label("MY CLUBHOUSE",10,new Color(0xA7CDBE),true));gap(body,17);
        Theme.add(body,navButton("home","학습 홈","home",false));gap(body,5);
        Theme.add(body,navButton("library","발견한 규칙","rule",false));gap(body,5);
        Theme.add(body,navButton("mypage","마이페이지","user",false));gap(body,5);
        myPageSubmenu=column();
        Theme.add(myPageSubmenu,navButton("missions","나의 학습 경로","flag",true));gap(myPageSubmenu,1);
        Theme.add(myPageSubmenu,navButton("records","나의 성장","growth",true));gap(myPageSubmenu,1);
        Theme.add(myPageSubmenu,navButton("notes","나의 메모","notes",true));gap(myPageSubmenu,1);
        myPageSubmenu.setVisible(false);
        Theme.add(body,myPageSubmenu);gap(body,5);
        accountNav=new DashboardStyle.NavButton("내 정보","gear",false);
        accountNav.addActionListener(e->navigate("account"));
        Theme.add(body,accountNav);
        side.add(body,BorderLayout.NORTH);
        JPanel bottom=column();
        Theme.add(bottom,label("서두르지 않아도 괜찮아요.",12,new Color(0xBAD8CB),false));gap(bottom,5);
        Theme.add(bottom,label("한 번의 판정이, 하나의 배움.",12,new Color(0xBAD8CB),false));gap(bottom,17);
        profile.setMaximumSize(new Dimension(Integer.MAX_VALUE,46));Theme.add(bottom,profile);
        side.add(bottom,BorderLayout.SOUTH);
        return side;
    }

    private JButton navButton(String page,String title,String icon,boolean nested) {
        DashboardStyle.NavButton button=new DashboardStyle.NavButton(title,icon,nested);
        button.addActionListener(e->navigate(page));
        navigation.put(page,button);
        return button;
    }

    private static boolean isMyPageSection(String page) {
        return page.equals("mypage") || page.equals("missions") || page.equals("records") || page.equals("notes");
    }
    private void navigate(String page) {
        if (page.equals(currentPage) && session == null) return;
        if (!leaveQuiz()) return;
        if (!page.equals(currentPage)) { backHistory.push(currentPage); forwardHistory.clear(); }
        session = null;
        showPage(page);
    }

    private void goBack() {
        if (backHistory.isEmpty() || !leaveQuiz()) return;
        forwardHistory.push(currentPage);
        session = null;
        showPage(backHistory.pop());
    }

    private void goForward() {
        if (forwardHistory.isEmpty() || !leaveQuiz()) return;
        backHistory.push(currentPage);
        session = null;
        showPage(forwardHistory.pop());
    }

    private void installHistoryControls() {
        JRootPane root = getRootPane();
        InputMap keys = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actions = root.getActionMap();
        keys.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.ALT_DOWN_MASK), "history-back");
        keys.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.ALT_DOWN_MASK), "history-forward");
        actions.put("history-back", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { goBack(); }
        });
        actions.put("history-forward", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { goForward(); }
        });
        // Windows 등에서 JVM이 별도 마우스 버튼을 전달하는 경우에만 작동.
        // 장치/드라이버에 따라 전달되지 않을 수 있으므로 화면 버튼과 Alt+화살표도 제공한다.
        mouseHistoryListener = event -> {
            if (!(event instanceof MouseEvent mouse) || mouse.getID() != MouseEvent.MOUSE_PRESSED) return;
            int mouseButton = mouse.getButton();
            if (mouseButton != 4 && mouseButton != 5) return;
            if (!(mouse.getSource() instanceof Component source)
                    || SwingUtilities.getWindowAncestor(source) != this) return;
            SwingUtilities.invokeLater(() -> {
                if (!isDisplayable()) return;
                if (mouseButton == 4) goBack(); else goForward();
            });
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(mouseHistoryListener, AWTEvent.MOUSE_EVENT_MASK);
    }

    @Override public void dispose() {
        if (mouseHistoryListener != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(mouseHistoryListener);
            mouseHistoryListener = null;
        }
        super.dispose();
    }
    private boolean leaveQuiz() {
        if (session==null || session.stage()==QuizSession.Stage.COMPLETE) return true;
        return JOptionPane.showConfirmDialog(this,"미션을 나갈까요?\n이미 제출한 판정은 저장됩니다. 아직 제출하지 않은 선택은 저장되지 않습니다.\n완료하지 않은 단계는 처음부터 다시 시작해요.",
                "미션 나가기",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION;
    }
    private void refreshHeader() {
        xp.setText(service.rank()+"   /   "+service.xp()+" XP"); profile.setText(service.user().nickname()+"  · 내 정보");
    }
    private void showPage(String page) {
        currentPage=page; refreshHeader();
        backButton.setEnabled(!backHistory.isEmpty());
        forwardButton.setEnabled(!forwardHistory.isEmpty());
        if (myPageSubmenu != null) myPageSubmenu.setVisible(isMyPageSection(page));
        for(var entry:navigation.entrySet()) {
            boolean chosen=entry.getKey().equals(page)
                    || (entry.getKey().equals("mypage") && isMyPageSection(page));
            if(entry.getValue() instanceof DashboardStyle.NavButton nav) nav.setSelectedPage(chosen);
        }
        if(accountNav!=null)accountNav.setSelectedPage("account".equals(page));
        JComponent content=switch(page) {
            case "mypage" -> new MyPagePanel(service,this::navigate,this::handleAccountAction,logoutAction!=null,this::startNext);
            case "account" -> accountPage();
            case "missions" -> missions(); case "library" -> library();
            case "records" -> records(); case "notes" -> notes(); default -> home();
        };
        location.setText(switch(page) {
            case "mypage" -> "마이페이지";
            case "missions" -> "마이페이지  /  나의 학습 경로";
            case "records" -> "마이페이지  /  나의 성장";
            case "notes" -> "마이페이지  /  나의 메모";
            case "library" -> "발견한 규칙";
            case "account" -> "내 정보";
            default -> "학습 홈";
        });
        workspace.removeAll(); workspace.add(content,page); cards.show(workspace,page); workspace.revalidate(); workspace.repaint();
    }
    private JPanel page(String kicker,String title,String subtitle) {
        JPanel p=column(); p.setBorder(new EmptyBorder(28,30,28,30));
        Theme.add(p,label(kicker,11,GREEN,true));gap(p,9);Theme.add(p,label(title,29,INK,true));gap(p,9);
        Theme.add(p,label(subtitle,14,MUTED,false));gap(p,24);return p;
    }
    private JTextArea paragraph(String value,int rows) { JTextArea a=text(value,14,MUTED);a.setRows(rows);return a; }
    private String nextAction() { return service.courseComplete() ? "새로운 조합으로 도전   →" : "STEP " + service.nextStepNumber() + " 시작하기   →"; }
    private void startNext() {
        try { showQuiz(service.startNext(), service.courseComplete()?"계속 도전":"STEP "+service.nextStepNumber()); }
        catch(Exception e) { error(e); }
    }
    private JComponent home() {
        boolean done=service.courseComplete(); int current=service.nextStepNumber();
        JPanel p=page("YOUR NEXT BASE", "다음 판정은, 시작하면 펼쳐져요.", service.user().nickname()+"님, 한 단계씩 나만의 심판 감각을 키워 보세요.");
        RoundedPanel hero=new RoundedPanel(PALE,22); hero.setLayout(new BorderLayout(20,0));
        JPanel left=column(); Theme.add(left,label(done?"ALL BASES CLEAR":"NOW PLAYING   /   STEP "+current,11,GREEN,true));gap(left,14);
        Theme.add(left,label(done?"모든 단계를 완주했어요!":service.step(current).goal(),25,INK,true));gap(left,12);
        Theme.add(left,paragraph(done?"익숙한 규칙도 다른 조건에서는 새로운 도전이 됩니다.\n지금 필요한 판단을 섞어서 다시 만나 보세요.":"어떤 상황이 기다릴지는 아직 비밀이에요.\n장면을 만나고, 판단하고, 해설을 확인하며 앞으로 나아가요.",3));gap(left,14);
        Theme.add(left,button(nextAction(),true,this::startNext));gap(left,12);
        Theme.add(left,label("한 번에 2~4개 상황  ·  시간제한 없음  ·  실수해도 다음 단계로",12,MUTED,false));
        hero.add(left,BorderLayout.CENTER);hero.setMaximumSize(new Dimension(Integer.MAX_VALUE,285));Theme.add(p,hero);gap(p,18);
        Theme.add(p,stats());gap(p,24);
        Theme.add(p,label("나의 학습 경로",19,INK,true));gap(p,8);
        Theme.add(p,label("지금의 한 단계에 집중해요. 다음 내용은 도착했을 때 열립니다.",13,MUTED,false));gap(p,16);
        Theme.add(p,missionCards());gap(p,18);
        Theme.add(p,paragraph("따로 오답을 정리하지 않아도 괜찮아요. 헷갈렸던 판단은 다음 단계의 다른 상황 속에서 다시 연습해요.",2));
        return scroll(p);
    }
    private JPanel stats() {
        JPanel row=new JPanel(new GridLayout(1,3,15,0)); row.setOpaque(false);
        row.add(stat("완료한 단계",service.completedSteps()+" / "+service.steps().size(),"한 단계씩 열리는 나의 경로"));
        row.add(stat("스스로 해결한 상황",service.solvedCount()+"개","답과 필요한 단서 정답 · 힌트 없이"));
        row.add(stat("차곡차곡 쌓인 경험",service.xp()+" XP",service.rank()));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE,125));return row;
    }
    private JPanel stat(String caption,String number,String sub) {
        RoundedPanel card=card();card.setLayout(new BorderLayout(0,9));card.add(label(caption,12,MUTED,false),BorderLayout.NORTH);
        card.add(label(number,28,INK,true),BorderLayout.CENTER);card.add(label(sub,11,MUTED,false),BorderLayout.SOUTH);return card;
    }
    private JPanel missionCards() {
        JPanel path=column();
        for(LearningStep step:service.steps()) {
            boolean done=service.stepCompleted(step.number()), unlocked=service.stepUnlocked(step.number());
            RoundedPanel item=new RoundedPanel(done?WHITE:unlocked?PALE:new Color(0xECEFEB),18);
            item.setLayout(new BorderLayout(18,0));item.setBorder(new EmptyBorder(17,20,17,20));
            RoundedPanel badge=new RoundedPanel(done?GREEN:unlocked?WHITE:LINE,48);badge.setBorder(new EmptyBorder(0,0,0,0));badge.setLayout(new BorderLayout());
            JLabel node=label(unlocked?String.valueOf(step.number()):"?",24,done?WHITE:unlocked?GREEN:MUTED,true);
            node.setHorizontalAlignment(SwingConstants.CENTER);badge.add(node);badge.setPreferredSize(new Dimension(48,48));item.add(badge,BorderLayout.WEST);
            JPanel info=column();Theme.add(info,label("STEP "+step.number()+"  /  "+(done?"완료":unlocked?"지금 도전":"아직 열리지 않은 단계"),11,unlocked?GREEN:MUTED,true));gap(info,6);
            Theme.add(info,label(unlocked?step.goal():"어떤 판정이 기다리고 있을까요?",16,unlocked?INK:MUTED,true));
            item.add(info,BorderLayout.CENTER);
            JButton action=button(done?"다시 도전":unlocked?"시작하기   →":"잠김",unlocked&&!done,()->{
                try { showQuiz(service.startStep(step.number()),"STEP "+step.number()); } catch(Exception e) { error(e); }
            });action.setEnabled(unlocked);item.add(action,BorderLayout.EAST);
            item.setMaximumSize(new Dimension(Integer.MAX_VALUE,92));Theme.add(path,item);
            if(step.number()<service.steps().size()){gap(path,3);Theme.add(path,label("       │",14,LINE,true));gap(path,3);}
        }
        return path;
    }
    private JComponent missions() {
        JPanel p=page("YOUR LEARNING PATH","한 단계씩, 다음 베이스로.","새로운 상황 사이에 이전 판단이 다시 등장해요. 실수는 다음 도전을 준비하는 단서입니다.");
        if(service.courseComplete()){Theme.add(p,button(nextAction(),true,this::startNext));gap(p,20);}
        Theme.add(p,missionCards());return scroll(p);
    }
    private void showQuiz(QuizSession next,String title) {
        session=next;location.setText("심판 미션  /  "+title);
        navigation.values().forEach(b->{
            if(b instanceof DashboardStyle.NavButton nav) nav.setSelectedPage(false);
            else b.setBackground(NAVY);
        });
        if(accountNav!=null)accountNav.setSelectedPage(false);
        QuizPanel quiz=new QuizPanel(service,session,this::completed,()->{if(leaveQuiz()){session=null;showPage("missions");}},this::message,this::error);
        workspace.removeAll();workspace.add(quiz,"quiz");cards.show(workspace,"quiz");workspace.revalidate();workspace.repaint();
    }
    private void completed() {
        refreshHeader();int correct=(int)session.results().stream().filter(Attempt::understood).count();
        int independent=(int)session.results().stream().filter(Attempt::independent).count();
        JPanel p=page("MISSION COMPLETE","판정 하나만큼, 성장했어요.",session.stepNumber()>0?"STEP "+session.stepNumber()+" 완료 · "+(service.courseComplete()?"모든 단계를 완주했어요!":"다음 단계가 열렸어요."):"새로운 상황에서도 판단의 기준을 떠올려 보세요.");
        RoundedPanel result=card();result.setLayout(new BorderLayout(20,10));
        JPanel words=column();Theme.add(words,label(independent==session.size()?"NICE CALL!":"GOOD PRACTICE!",34,GREEN,true));gap(words,16);
        Theme.add(words,label("이해한 문제   "+correct+" / "+session.size(),22,INK,true));gap(words,12);
        Theme.add(words,label("힌트 없이 해결한 상황   "+independent+"개",15,MUTED,false));gap(words,12);
        Theme.add(words,paragraph("헷갈렸던 판단은 다음 도전의 다른 상황 속에서 다시 만나요.\n익숙해진 판단은 간격을 두고 돌아옵니다. 따로 정리할 숙제는 없어요.",2));
        result.add(words,BorderLayout.CENTER);Theme.add(p,result);gap(p,20);
        for(Attempt a:session.results()) {
            Question q=service.catalog().question(a.questionId()).orElseThrow();
            RoundedPanel row=card();row.setLayout(new BorderLayout());row.add(label(q.title(),15,INK,true),BorderLayout.WEST);
            row.add(label(a.independent()?"독립 해결 +30 XP까지":a.understood()?"힌트와 함께 이해":"다음 도전에서 이어 연습",13,a.understood()?GREEN:RED,true),BorderLayout.EAST);
            Theme.add(p,row);gap(p,10);
        }
        JPanel actions=row(12);actions.add(button(nextAction(),true,()->{session=null;startNext();}));
        actions.add(button("학습 경로 보기",false,()->{session=null;showPage("missions");}));Theme.add(p,actions);
        workspace.removeAll();workspace.add(scroll(p),"result");cards.show(workspace,"result");workspace.revalidate();workspace.repaint();
        message("미션 완료 · 모든 제출 기록을 저장했습니다.");
    }
    private JComponent library() {
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
                    try{service.toggleBookmark(r.id());message("북마크를 저장했습니다.");showPage("library");}catch(Exception e){error(e);}
                }));
                actions.add(button("내 말로 메모하기",false,()->noteDialog(r)));card.add(actions,BorderLayout.SOUTH);detail.add(scroll(card));
            }
            detail.revalidate();detail.repaint();
        };
        Runnable filter=()->{model.clear();service.catalog().searchRules(search.getText()).stream().filter(r->service.discovered(r.id())).filter(r->!only.isSelected()||service.bookmarked(r.id())).forEach(model::addElement);if(!model.isEmpty())list.setSelectedIndex(0);render.run();};
        listen(search,filter);only.addActionListener(e->filter.run());list.addListSelectionListener(e->{if(!e.getValueIsAdjusting())render.run();});filter.run();
        return outer;
    }
    private JComponent records() {
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
    private JComponent notes() {
        JPanel p=page("MY FIELD NOTES","내 말로 남기는 규칙.","원할 때만 남기는 개인 메모예요. 다음 단계로 가기 위해 작성할 필요는 없어요.");
        JPanel controls=new JPanel(new BorderLayout(12,0));controls.setOpaque(false);JTextField search=input("메모 내용이나 규칙 이름 검색");
        controls.setMaximumSize(new Dimension(Integer.MAX_VALUE,48));
        JPanel right=new JPanel(new FlowLayout(FlowLayout.RIGHT,12,0));right.setOpaque(false);
        JCheckBox favoriteOnly=new JCheckBox("★ 즐겨찾기만"); favoriteOnly.setOpaque(false);
        right.add(favoriteOnly);right.add(button("+ 메모 추가",true,()->noteDialog(null)));
        controls.add(search,BorderLayout.CENTER);controls.add(right,BorderLayout.EAST);Theme.add(p,controls);gap(p,20);
        JPanel results=column();Theme.add(p,results);
        Runnable refresh=()->{
            results.removeAll();List<StudyNote> list=service.notes(search.getText()).stream()
                    .filter(n->!favoriteOnly.isSelected()||service.noteFavorited(n.id())).toList();
            if(list.isEmpty())Theme.add(results,label("메모가 없습니다. 메모를 추가하거나 즐겨찾기를 확인해 보세요.",14,MUTED,false));
            for(StudyNote note:list) {
                RoundedPanel c=card();c.setLayout(new BorderLayout(20,12));
                c.add(label(service.catalog().rule(note.ruleId()).title(),15,GREEN,true),BorderLayout.NORTH);
                JTextArea body=paragraph(note.text(),Math.min(15,Math.max(2,(note.text().length()+49)/50+note.text().split("\n").length)));
                c.add(body,BorderLayout.CENTER);
                JPanel actions=column();
                Theme.add(actions,button(service.noteFavorited(note.id())?"★ 해제":"☆ 즐겨찾기",false,()->{
                    try{service.toggleNoteFavorite(note.id());showPage("notes");}catch(Exception e){error(e);}
                }));gap(actions,8);
                Theme.add(actions,button("수정",false,()->editNoteDialog(note)));gap(actions,8);
                Theme.add(actions,button("삭제",false,()->{
                    if(JOptionPane.showConfirmDialog(this,"이 메모를 삭제할까요?","메모 삭제",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)
                        try{service.deleteNote(note.id());message("메모를 삭제했습니다.");showPage("notes");}catch(Exception e){error(e);}
                }));
                c.add(actions,BorderLayout.EAST);Theme.add(results,c);gap(results,12);
            }results.revalidate();results.repaint();
        };listen(search,refresh);favoriteOnly.addActionListener(e->refresh.run());refresh.run();return scroll(p);
    }
    private void editNoteDialog(StudyNote existing) {
        JTextArea body=new JTextArea(existing.text(),7,36);
        body.setLineWrap(true);body.setWrapStyleWord(true);body.setFont(font(14,false));
        JPanel form=new JPanel(new BorderLayout(0,12));
        form.add(label("관련 규칙: "+service.catalog().rule(existing.ruleId()).title(),13,GREEN,true),BorderLayout.NORTH);
        form.add(new JScrollPane(body),BorderLayout.CENTER);
        form.add(label("메모 내용 수정 · 최대 500자",12,MUTED,false),BorderLayout.SOUTH);
        while(JOptionPane.showConfirmDialog(this,form,"메모 수정",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION) {
            try{service.updateNote(existing.id(),body.getText());message("메모를 수정했습니다.");showPage("notes");return;}
            catch(Exception e){error(e);}
        }
    }
    private void noteDialog(RuleConcept initial) {
        List<RuleConcept> discovered=service.catalog().rules().stream().filter(r->service.discovered(r.id())).toList();
        if(discovered.isEmpty()){message("먼저 첫 단계에 도전해 규칙을 발견해 보세요.");return;}
        JComboBox<RuleConcept> rule=new JComboBox<>(discovered.toArray(RuleConcept[]::new));if(initial!=null)rule.setSelectedItem(initial);
        JTextArea body=new JTextArea(7,36);body.setLineWrap(true);body.setWrapStyleWord(true);body.setFont(font(14,false));
        JPanel form=new JPanel(new BorderLayout(0,12));form.add(rule,BorderLayout.NORTH);form.add(new JScrollPane(body));
        form.add(label("최대 500자 · 파일에 저장되어 다음 실행에도 남아요.",12,MUTED,false),BorderLayout.SOUTH);
        while(JOptionPane.showConfirmDialog(this,form,"내 말로 메모하기",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION) {
            try {service.addNote(((RuleConcept)rule.getSelectedItem()).id(),body.getText());message("메모를 저장했습니다.");showPage("notes");return;}
            catch(Exception e){error(e);}
        }
    }
    /** 개인 정보 페이지는 로그인 서비스의 기존 데이터만 표시하며, 실제 변경 저장 기능은 추가하지 않는다. */
    private JComponent accountPage() {
        JPanel page=page("MY CLUBHOUSE / ACCOUNT","내 정보","현재 사용자와 로컬 학습 기록을 확인할 수 있어요.");
        DashboardStyle.Surface info=new DashboardStyle.Surface(WHITE,22);
        info.setLayout(new BorderLayout(0,18));
        info.setBorder(new EmptyBorder(27,30,27,30));
        info.setMaximumSize(new Dimension(Integer.MAX_VALUE,260));
        JPanel labels=column();
        Theme.add(labels,label("CURRENT ACCOUNT",12,GREEN,true));gap(labels,15);
        Theme.add(labels,label(service.user().nickname(),27,INK,true));gap(labels,12);
        Theme.add(labels,label(service.rank()+"  ·  "+service.xp()+" XP",15,MUTED,false));gap(labels,14);
        Theme.add(labels,label("이 계정의 학습 기록은 현재 컴퓨터에 저장됩니다.",13,MUTED,false));
        info.add(labels,BorderLayout.CENTER);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT,12,0));actions.setOpaque(false);
        actions.add(button("마이페이지로 돌아가기",true,()->navigate("mypage")));
        actions.add(button(logoutAction!=null?"로그아웃":"사용자 전환",false,this::handleAccountAction));
        info.add(actions,BorderLayout.SOUTH);
        Theme.add(page,info);
        return scroll(page);
    }
    private void handleProfileAction() { navigate("account"); }
    private void handleAccountAction() {
        if (logoutAction == null) { profilesDialog(); return; } // 기존 단독 실행 호환
        if (JOptionPane.showConfirmDialog(this,
                "로그아웃하고 다른 계정으로 로그인할까요?\n학습 기록은 이 컴퓨터에 저장됩니다.",
                "로그아웃",JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        dispose();
        logoutAction.run();
    }
    private void profilesDialog() {
        if(!leaveQuiz())return;
        session=null;
        JComboBox<UserProfile> people=new JComboBox<>(service.profiles().toArray(UserProfile[]::new));people.setSelectedItem(service.user());
        Object[] options={"선택한 사용자로 전환","새 사용자 추가","취소"};
        int answer=JOptionPane.showOptionDialog(this,people,"학습할 사용자",JOptionPane.DEFAULT_OPTION,JOptionPane.PLAIN_MESSAGE,null,options,options[0]);
        if(answer==0){service.selectUser(((UserProfile)people.getSelectedItem()).id());message("사용자를 전환했습니다.");}
        else if(answer==1){
            String nickname=JOptionPane.showInputDialog(this,"닉네임을 입력하세요. (1~16자)");
            if(nickname!=null)try{service.addProfile(nickname);message("새 사용자를 저장했습니다.");}catch(Exception e){error(e);}
        }
        showPage("home");
    }
    private void message(String text) { status.setText(text);refreshHeader(); }
    private void error(Exception e) { JOptionPane.showMessageDialog(this,e.getMessage(),"확인해 주세요",JOptionPane.WARNING_MESSAGE); }
    private static void listen(JTextField field,Runnable update) {
        field.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){update.run();}public void removeUpdate(DocumentEvent e){update.run();}public void changedUpdate(DocumentEvent e){update.run();}});
    }
}
