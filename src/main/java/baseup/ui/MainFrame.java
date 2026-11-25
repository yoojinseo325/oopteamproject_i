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
 * 창 틀(사이드바·위쪽 막대·아래 상태줄)과 페이지 이동, 퀴즈 진행만 담당한다.
 * 각 페이지 화면은 Screen을 상속한 클래스(HomeScreen, StudyScreen, QuizScreen, MyPageScreen …)가 만든다.
 * 데이터 변경은 LearningService에 위임한다.
 */
public final class MainFrame extends JFrame implements ScreenActions {
    private final LearningService service;
    private final CardLayout cards = new CardLayout();
    private final JPanel workspace = new JPanel(cards);
    private final Map<String, JButton> navigation = new LinkedHashMap<>();
    private final JLabel location = label("학습 홈", 15, INK, true);
    private final JLabel xp = label("", 13, GREEN, true);
    private final JLabel status = label("학습 준비 완료 · 기록은 이 컴퓨터에 저장됩니다", 12, MUTED, false);
    private final JButton profile = button("로그인", false, () -> { if (loggedIn()) accountAction(); else this.loginAction.run(); });
    /** 로그아웃 후 할 일(App이 로그인 창을 다시 띄운다). null이면 로그인 없이 실행(테스트·미리보기)한 것이다. */
    private final Runnable logoutAction;
    private final LearningDialogs dialogs;
    /** 로그인 버튼이 눌렸을 때 할 일. 로그인 페이지 담당 팀원이 setLoginAction(...)으로 연결한다. */
    private Runnable loginAction = () -> message("로그인 페이지는 준비 중이에요.");
    public void setLoginAction(Runnable action) { loginAction = action; }
    LearningService service() { return service; }
    // 메인 화면은 메뉴를 오갈 때마다 새로 만들어지므로, 이어져야 하는 것은 여기(창)에 보관한다
    private final baseup.game.GameState gameState = new baseup.game.GameState();   // 타격 게임 경기 기록
    private int todaySlide;                                                         // 오늘의 규칙 슬라이드 위치
    baseup.game.GameState gameState() { return gameState; }
    int todaySlide() { return todaySlide; }
    void setTodaySlide(int index) { todaySlide = index; }
    QuizSession session;
    private String currentPage = "home";

    /** 로그인 없이 실행(테스트·미리보기)할 때 쓰는 생성자. */
    public MainFrame(LearningService service) { this(service, null); }

    public MainFrame(LearningService service, Runnable logoutAction) {
        super("BASEUP · 야구를 읽는 첫걸음 | V1 + V2 통합"); this.service=service; this.logoutAction=logoutAction;
        dialogs = new LearningDialogs(this, service, this);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { if (leaveQuiz()) { dispose(); System.exit(0); } }
        });
        setLayout(new BorderLayout()); side=sidebar(); super.add(side,BorderLayout.WEST);
        JPanel main=new JPanel(new BorderLayout()); main.setBackground(BG);
        JPanel top=new JPanel(new BorderLayout()); top.setBackground(Ballpark.BOARD); top.setBorder(new EmptyBorder(16,30,16,30));
        location.setForeground(WHITE); xp.setForeground(Ballpark.LAMP);
        top.add(location,BorderLayout.WEST); top.add(xp,BorderLayout.EAST); main.add(top,BorderLayout.NORTH);
        main.add(workspace,BorderLayout.CENTER);
        JPanel footer=new JPanel(new BorderLayout()); footer.setBackground(BG); footer.setBorder(new EmptyBorder(9,30,12,30));
        footer.add(status,BorderLayout.WEST); footer.add(label("BASEUP / V2.1",11,MUTED,true),BorderLayout.EAST);
        main.add(footer,BorderLayout.SOUTH); super.add(main,BorderLayout.CENTER);
        setMinimumSize(new Dimension(1100,760));
        Dimension screen=Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(1360,screen.width-70),Math.min(940,screen.height-70));
        setLocationRelativeTo(null); showPage("home");
        showIntro();
    }
    /**
     * 프로그램을 처음 켰을 때 16초 인트로(IntroAnimation)를 창 전체 위(글래스 패인)에 띄운다.
     * 끝나거나 클릭·키 입력으로 건너뛰면 인트로가 0.5초 동안 투명해지며 메인 화면이 드러난다.
     * 끄려면 -Dbaseup.intro=off 로 실행한다. 화면이 없는 테스트 환경에서는 띄우지 않는다.
     */
    private void showIntro() {
        if (GraphicsEnvironment.isHeadless() || "off".equals(System.getProperty("baseup.intro"))) return;
        IntroAnimation intro = new IntroAnimation(() -> getGlassPane().setVisible(false));
        setGlassPane(intro);
        intro.setVisible(true);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowOpened(java.awt.event.WindowEvent e) { intro.start(); }
        });
    }
    private JPanel side;
    /** 왼쪽 사이드바: 응원팀을 고르면 배경·띠·카드가 그 팀 색으로 바뀐다. */
    private JPanel sidebar() {
        Team team=favoriteTeam();
        Sidebar.Background panel=new Sidebar.Background(team==null?null:new Color(team.color()),team==null?null:new Color(team.accent()));
        panel.setPreferredSize(new Dimension(236,700)); panel.setBorder(new EmptyBorder(24,16,20,16));
        JPanel body=column();
        Theme.add(body,new Sidebar.Emblem()); gap(body,20);
        RoundedPanel teamCard=new RoundedPanel(new Color(0,0,0,70),16);teamCard.setBorder(new EmptyBorder(12,12,12,12));teamCard.setLayout(new BorderLayout(12,0));
        if(team!=null) {
            teamCard.add(new Ballpark.ImageSlot("logos",team.id(),"LOGO",64,64),BorderLayout.WEST);
            JPanel who=column();Theme.add(who,label("MY TEAM",10,new Color(255,255,255,200),true));gap(who,3);Theme.add(who,label(team.name(),15,WHITE,true));gap(who,6);
            JButton change=button("팀 바꾸기",false,this::pickTeam);change.setFont(Theme.font(11,true));change.setBorder(new EmptyBorder(5,10,5,10));Theme.add(who,change);
            teamCard.add(who,BorderLayout.CENTER);
            teamCard.add(nextGameLine(team),BorderLayout.SOUTH);
        } else {
            JPanel who=column();Theme.add(who,label("MY TEAM",10,new Color(0x9FB0C6),true));gap(who,6);
            Theme.add(who,label("응원팀을 골라 보세요",13,WHITE,true));gap(who,8);
            Theme.add(who,button("응원팀 고르기",false,this::pickTeam));teamCard.add(who,BorderLayout.CENTER);
        }
        teamCard.setMaximumSize(new Dimension(Integer.MAX_VALUE,teamCard.getPreferredSize().height));Theme.add(body,teamCard);gap(body,22);
        Theme.add(body,label("   MY CLUBHOUSE",11,new Color(255,255,255,190),true)); gap(body,8);
        String[][] menus={{"home","메인 홈","home"},{"missions","규칙 학습","mitt"},{"quiz","퀴즈","camera"},
                {"library","규칙 백과사전","rulebook"},{"mypage","마이페이지","person"}};
        navigation.clear();
        for(String[] menu:menus) {
            Sidebar.NavButton b=new Sidebar.NavButton(menu[1],menu[2]);
            b.addActionListener(e -> navigate(menu[0])); navigation.put(menu[0],b); Theme.add(body,b); gap(body,2);
        }
        panel.add(body,BorderLayout.NORTH);
        // 마스코트 자리: data/mascots/팀ID.png 를 넣으면 사이드바 아래쪽에 크게 뜬다
        JPanel mascot=new JPanel(new GridBagLayout());mascot.setOpaque(false);
        if(team!=null){
            JPanel stack=column();
            Ballpark.ImageSlot slot=new Ballpark.ImageSlot("mascots",team.id(),"MASCOT",190,230);slot.setAlignmentX(.5f);stack.add(slot);
            JLabel name=label(team.name(),22,WHITE,true);name.setFont(name.getFont().deriveFont(Font.BOLD|Font.ITALIC));name.setAlignmentX(.5f);
            stack.add(Box.createVerticalStrut(8));stack.add(name);
            GridBagConstraints c=new GridBagConstraints();c.anchor=GridBagConstraints.SOUTH;c.weighty=1;mascot.add(stack,c);
        }
        panel.add(mascot,BorderLayout.CENTER);
        JPanel bottom=column(); gap(bottom,10);
        profile.setMaximumSize(new Dimension(Integer.MAX_VALUE,46)); Theme.add(bottom,profile); panel.add(bottom,BorderLayout.SOUTH);
        return panel;
    }
    /** 사이드바 팀 카드 아래: 다음 경기 + 최근 결과 한 줄 (data/games.tsv). */
    private JComponent nextGameLine(Team team) {
        JPanel box=column(); box.setBorder(new EmptyBorder(10,0,0,0));
        try {
            List<GameResult> mine=new ArrayList<>(TeamSchedule.games().stream().filter(g->g.involves(team.id())).toList());
            mine.sort(Comparator.comparing(GameResult::date).thenComparing(GameResult::time));
            GameResult next=mine.stream().filter(g->!g.played()).findFirst().orElse(null);
            List<GameResult> done=mine.stream().filter(GameResult::played).toList();
            Theme.add(box,label("나의 다음 일정",10,new Color(255,255,255,200),true));gap(box,3);
            Theme.add(box,label(next==null?"예정된 경기 없음":TeamSchedule.shortDate(next.date())+" "+next.time()+"  vs "+TeamSchedule.teamName(service,next.opponent(team.id())),12,WHITE,true));
            if(!done.isEmpty()){
                StringBuilder recent=new StringBuilder("최근  ");
                for(int i=done.size()-1;i>=Math.max(0,done.size()-2);i--){GameResult g=done.get(i);int me=g.myScore(team.id()),them=g.theirScore(team.id());
                    recent.append(me>them?"승 ":me<them?"패 ":"무 ").append(me).append(":").append(them).append("   ");}
                gap(box,3);Theme.add(box,label(recent.toString().strip(),12,Ballpark.LAMP,true));
            }
        } catch(java.io.IOException e) { Theme.add(box,label("games.tsv 확인 필요",11,WHITE,false)); }
        return box;
    }
    /** 응원팀이 바뀌면 사이드바를 새로 만들어 끼운다. */
    private void rebuildSidebar() {
        getContentPane().remove(side); side=sidebar(); super.add(side,BorderLayout.WEST);
        getContentPane().revalidate(); getContentPane().repaint();
    }
    void navigate(String page) { if (leaveQuiz()) { session=null; showPage(page); } }
    boolean leaveQuiz() {
        if (session==null || session.stage()==QuizSession.Stage.COMPLETE) return true;
        return JOptionPane.showConfirmDialog(this,"미션을 나갈까요?\n이미 제출한 판정은 저장됩니다. 아직 제출하지 않은 선택은 저장되지 않습니다.\n완료하지 않은 단계는 처음부터 다시 시작해요.",
                "미션 나가기",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION;
    }
    private void refreshHeader() {
        xp.setText(service.rank()+"   /   "+service.xp()+" XP"); profile.setText(loggedIn()?service.user().nickname()+"  · 로그아웃":"로그인");
    }
    @Override public void showPage(String page) {
        currentPage=page; refreshHeader();
        for(var entry:navigation.entrySet()) {
            ((Sidebar.NavButton)entry.getValue()).active(entry.getKey().equals(navKey(page)));
        }
        JComponent content=switch(page) {
            case "missions" -> new StudyScreen(this).build(); case "quiz" -> new QuizScreen(this).build();
            case "library" -> { String rule=pendingRule; pendingRule=null; yield new RuleLibraryPanel(service, this, rule); }
            case "mypage" -> new MyPagePanel(service, this::navigate, this::accountAction, loggedIn(), this::startNext);
            case "records", "notes" -> new MyPageScreen(this, page).build(); default -> new HomeScreen(this).build();
        };
        location.setText(switch(navKey(page)) {case "missions"->"규칙 학습";case "quiz"->"퀴즈";case "library"->"규칙 백과사전";case "mypage"->"마이페이지";default->"학습 홈";});
        workspace.removeAll(); workspace.add(content,page); cards.show(workspace,page); workspace.revalidate(); workspace.repaint();
    }
    String nextAction() { return service.courseComplete() ? "새로운 조합으로 도전   →" : "STEP " + service.nextStepNumber() + " 시작하기   →"; }
    @Override public void startNext() {
        try { showQuiz(service.startNext(), service.courseComplete()?"계속 도전":"STEP "+service.nextStepNumber()); }
        catch(Exception e) { error(e); }
    }
    String pendingRule; RuleSearchBox homeSearch;
    /** 응원팀. 지금은 이 실행 동안만 기억한다(저장은 백엔드 UserProfile에 칸이 생기면 연결). */
    private Team favoriteTeam;
    Team favoriteTeam() {
        if (favoriteTeam == null && System.getProperty("baseup.team") != null)
            favoriteTeam = service.catalog().teams().stream().filter(t -> t.id().equals(System.getProperty("baseup.team"))).findFirst().orElse(null);
        return favoriteTeam;
    }
    /** 메인 검색에서 고른 규칙을 '발견한 규칙' 화면에서 선택된 상태로 연다. */
    void openRule(String ruleId) { pendingRule=ruleId; showPage("library"); }
    private void pickTeam() {
        List<Team> teams=service.catalog().teams();
        String[] names=teams.stream().map(Team::name).toArray(String[]::new);
        Object chosen=JOptionPane.showInputDialog(this,"어느 팀을 응원하세요?","응원팀 고르기",JOptionPane.PLAIN_MESSAGE,null,
                names,favoriteTeam()==null?names[0]:favoriteTeam().name());
        for(Team t:teams) if(t.name().equals(chosen)){favoriteTeam=t;rebuildSidebar();showPage("home");}
    }
    void showQuiz(QuizSession next,String title) {
        session=next;location.setText("심판 미션  /  "+title);
        navigation.values().forEach(b->((Sidebar.NavButton)b).active(false));
        QuizPanel quiz=new QuizPanel(service,session,this::completed,()->{if(leaveQuiz()){session=null;showPage("missions");}},this::message,this::error);
        workspace.removeAll();workspace.add(quiz,"quiz");cards.show(workspace,"quiz");workspace.revalidate();workspace.repaint();
    }
    private void completed() {
        refreshHeader();int correct=(int)session.results().stream().filter(Attempt::understood).count();
        int independent=(int)session.results().stream().filter(Attempt::independent).count();
        JPanel p=Screen.page("MISSION COMPLETE","판정 하나만큼, 성장했어요.",session.stepNumber()>0?"STEP "+session.stepNumber()+" 완료 · "+(service.courseComplete()?"모든 단계를 완주했어요!":"다음 단계가 열렸어요."):"새로운 상황에서도 판단의 기준을 떠올려 보세요.");
        RoundedPanel result=card();result.setLayout(new BorderLayout(20,10));
        JPanel words=column();Theme.add(words,label(independent==session.size()?"NICE CALL!":"GOOD PRACTICE!",34,SUCCESS,true));gap(words,16);
        Theme.add(words,label("이해한 문제   "+correct+" / "+session.size(),22,INK,true));gap(words,12);
        Theme.add(words,label("힌트 없이 해결한 상황   "+independent+"개",15,MUTED,false));gap(words,12);
        Theme.add(words,Screen.paragraph("헷갈렸던 판단은 다음 도전의 다른 상황 속에서 다시 만나요.\n익숙해진 판단은 간격을 두고 돌아옵니다. 따로 정리할 숙제는 없어요.",2));
        result.add(words,BorderLayout.CENTER);Theme.add(p,result);gap(p,20);
        for(Attempt a:session.results()) {
            Question q=service.catalog().question(a.questionId()).orElseThrow();
            RoundedPanel row=card();row.setLayout(new BorderLayout());row.add(label(q.title(),15,INK,true),BorderLayout.WEST);
            row.add(label(a.independent()?"독립 해결 +30 XP까지":a.understood()?"힌트와 함께 이해":"다음 도전에서 이어 연습",13,a.understood()?SUCCESS:RED,true),BorderLayout.EAST);
            Theme.add(p,row);gap(p,10);
        }
        JPanel actions=row(12);actions.add(button(nextAction(),true,()->{session=null;startNext();}));
        actions.add(button("학습 경로 보기",false,()->{session=null;showPage("missions");}));Theme.add(p,actions);
        workspace.removeAll();workspace.add(scroll(p),"result");cards.show(workspace,"result");workspace.revalidate();workspace.repaint();
        message("미션 완료 · 모든 제출 기록을 저장했습니다.");
    }
    private static String navKey(String page) {
        return switch (page) { case "records", "notes" -> "mypage"; default -> page; };
    }
    void profilesDialog() {
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
    boolean loggedIn() { return logoutAction != null; }
    /** 로그인해서 실행했으면 로그아웃, 아니면 기존 사용자 전환. */
    void accountAction() {
        if (!loggedIn()) { profilesDialog(); return; }
        if (!leaveQuiz()) return;
        if (JOptionPane.showConfirmDialog(this, "로그아웃하고 다른 계정으로 로그인할까요?\n학습 기록은 이 컴퓨터에 저장됩니다.",
                "로그아웃", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        session = null;
        dispose();
        logoutAction.run();
    }
    @Override public void startStep(int number) {
        try { showQuiz(service.startStep(number), "STEP " + number); } catch (Exception e) { error(e); }
    }
    @Override public void editNote(RuleConcept initial) { dialogs.editNote(initial); }
    @Override public void message(String text) { status.setText(text);refreshHeader(); }
    @Override public void error(Exception e) { JOptionPane.showMessageDialog(this,e.getMessage(),"확인해 주세요",JOptionPane.WARNING_MESSAGE); }
}
