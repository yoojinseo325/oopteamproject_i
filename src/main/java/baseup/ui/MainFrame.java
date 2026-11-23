package baseup.ui;

import baseup.model.QuizSession;
import baseup.model.RuleConcept;
import baseup.service.LearningService;
import baseup.service.LearningStatistics;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.Cursor;
import java.awt.CardLayout;
import java.util.Map;
import java.util.LinkedHashMap;
import static baseup.ui.Theme.*;

/** 공통 창, 탐색, 진행 중인 미션의 수명만 관리한다. 각 페이지의 구성은 전용 패널에 위임한다. */
public final class MainFrame extends JFrame implements ScreenActions {
    private final LearningService service;
    private final CardLayout cards = new CardLayout();
    private final JPanel workspace = new JPanel(cards);
    private final Map<String, JButton> navigation = new LinkedHashMap<>();
    private final JLabel location = label("학습 홈", 15, INK, true);
    private final JLabel xp = label("", 13, GREEN, true);
    private final JLabel status = label("학습 준비 완료 · 기록은 이 컴퓨터에 저장됩니다", 12, MUTED, false);
    private final JButton profile = button("", false, this::profilesDialog);
    private QuizSession session;
    private final LearningDialogs dialogs;

    public MainFrame(LearningService service) {
        super("BASEUP · 야구를 읽는 첫걸음 | V1 + V2 통합");
        this.service=service;
        dialogs = new LearningDialogs(this, service, this);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                if (leaveQuiz()) {
                    dispose();
                    System.exit(0);
                }
            }
        });
        setLayout(new BorderLayout());
        super.add(sidebar(),BorderLayout.WEST);
        JPanel main=new JPanel(new BorderLayout());
        main.setBackground(BG);
        JPanel top=new JPanel(new BorderLayout());
        top.setBackground(WHITE);
        top.setBorder(new EmptyBorder(18,30,18,30));
        top.add(location,BorderLayout.WEST);
        top.add(xp,BorderLayout.EAST);
        main.add(top,BorderLayout.NORTH);
        main.add(workspace,BorderLayout.CENTER);
        JPanel footer=new JPanel(new BorderLayout());
        footer.setBackground(BG);
        footer.setBorder(new EmptyBorder(9,30,12,30));
        footer.add(status,BorderLayout.WEST);
        footer.add(label("BASEUP / V2.1",11,MUTED,true),BorderLayout.EAST);
        main.add(footer,BorderLayout.SOUTH);
        super.add(main,BorderLayout.CENTER);
        setMinimumSize(new Dimension(1100,760));
        Dimension screen=Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(1360,screen.width-70),Math.min(940,screen.height-70));
        setLocationRelativeTo(null);
        showPage("home");
    }
    private JPanel sidebar() {
        JPanel side=new JPanel(new BorderLayout());
        side.setBackground(NAVY);
        side.setPreferredSize(new Dimension(210,700));
        side.setBorder(new EmptyBorder(30,20,24,20));
        JPanel body=column();
        Theme.add(body,label("BASEUP",29,WHITE,true));
        gap(body,4);
        Theme.add(body,label("야구를 읽는 첫걸음",12,new Color(0x9EB8AE),false));
        gap(body,42);
        Theme.add(body,label("MY CLUBHOUSE",10,new Color(0x7F9B91),true));
        gap(body,14);
        String[][] menus={
            {
                "home","01   학습 홈"
            },{
                "missions","02   나의 학습 경로"
            },{
                "library","03   규칙 백과사전"
            },
            {
                "records","04   나의 성장"
            },{
                "notes","05   나의 메모"
            }
        };
        for(String[] menu:menus) {
            JButton b=new JButton(menu[1]);
            b.setFont(font(14,true));
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setForeground(new Color(0xBDCDC6));
            b.setBackground(NAVY);
            b.setOpaque(true);
            b.setBorder(new EmptyBorder(14,14,14,8));
            b.setFocusPainted(true);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE,49));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.addActionListener(e -> navigate(menu[0]));
            navigation.put(menu[0],b);
            Theme.add(body,b);
            gap(body,5);
        }
        side.add(body,BorderLayout.NORTH);
        JPanel bottom=column();
        Theme.add(bottom,label("서두르지 않아도 괜찮아요.",12,new Color(0xB9CBC2),false));
        gap(bottom,5);
        Theme.add(bottom,label("한 번의 판정이, 하나의 배움.",12,new Color(0xB9CBC2),false));
        gap(bottom,22);
        profile.setMaximumSize(new Dimension(Integer.MAX_VALUE,48));
        Theme.add(bottom,profile);
        side.add(bottom,BorderLayout.SOUTH);
        return side;
    }
    private void navigate(String page) {
        if (leaveQuiz()) {
            session=null;
            showPage(page);
        }
    }
    private boolean leaveQuiz() {
        if (session==null || session.stage()==QuizSession.Stage.COMPLETE) return true;
        return JOptionPane.showConfirmDialog(this,"미션을 나갈까요?\n이미 제출한 판정은 저장됩니다. 아직 제출하지 않은 선택은 저장되지 않습니다.\n완료하지 않은 단계는 처음부터 다시 시작해요.",
        "미션 나가기",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION;
    }
    private void refreshHeader() {
        LearningStatistics stats = service.statistics();
        xp.setText(stats.rank()+"   /   "+stats.xp()+" XP");
        profile.setText(service.user().nickname()+"  · 변경");
    }
    @Override public void showPage(String page) {
        refreshHeader();
        for(var entry:navigation.entrySet()) {
            entry.getValue().setBackground(entry.getKey().equals(page)?new Color(0x285044):NAVY);
            entry.getValue().setForeground(entry.getKey().equals(page)?WHITE:new Color(0xBDCDC6));
        }
        JComponent content=switch(page) {
            case "missions" -> new LearningPathPanel(service, this);
            case "library" -> new RuleLibraryPanel(service, this);
            case "records" -> new RecordsPanel(service);
            case "notes" -> new NotesPanel(service, this);
            default -> new HomePanel(service, this);
        };
        location.setText(switch(page) {
            case "missions"->"나의 학습 경로";
            case "library"->"규칙 백과사전";
            case "records"->"나의 성장";
            case "notes"->"나의 메모";
            default->"학습 홈";
        });
        workspace.removeAll();
        workspace.add(content,page);
        cards.show(workspace,page);
        workspace.revalidate();
        workspace.repaint();
    }
    @Override public void startNext() {
        try {
            showQuiz(service.startNext(), service.courseComplete()?"계속 도전":"STEP "+service.nextStepNumber());
        }
        catch(Exception e) {
            error(e);
        }
    }
    private void showQuiz(QuizSession next,String title) {
        session=next;
        location.setText("심판 미션  /  "+title);
        navigation.values().forEach(b->b.setBackground(NAVY));
        QuizPanel quiz=new QuizPanel(service,session,this::completed,()->{
            if(leaveQuiz()){
                session=null;
                showPage("missions");
            }
        },this::message,this::error);
        workspace.removeAll();
        workspace.add(quiz,"quiz");
        cards.show(workspace,"quiz");
        workspace.revalidate();
        workspace.repaint();
    }
    private void completed() {
        refreshHeader();
        workspace.removeAll();
        workspace.add(new MissionResultPanel(service, this, session), "result");
        cards.show(workspace, "result");
        workspace.revalidate();
        workspace.repaint();
        message("미션 완료 · 모든 제출 기록을 저장했습니다.");
    }
    private void profilesDialog() {
        if (!leaveQuiz()) return;
        session = null;
        dialogs.selectProfile();
    }

    @Override public void editNote(RuleConcept initial) {
        dialogs.editNote(initial);
    }

    @Override public void startStep(int number) {
        try {
            showQuiz(service.startStep(number), "STEP " + number);
        }
        catch (Exception e) {
            error(e);
        }
    }
    @Override public void message(String text) {
        status.setText(text);
        refreshHeader();
    }
    @Override public void error(Exception e) {
        JOptionPane.showMessageDialog(this,e.getMessage(),"확인해 주세요",JOptionPane.WARNING_MESSAGE);
    }
}
