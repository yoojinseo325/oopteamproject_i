package baseup.ui;

import baseup.model.*;
import baseup.service.LearningService;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import static baseup.ui.Theme.*;

/** BASEUP 대시보드형 마이페이지. 기록의 조회·저장은 기존 LearningService에 위임한다. */
public final class MyPagePanel extends JPanel {
    private static final Color SOFT = new Color(0xA9D5C4);
    private final LearningService service;
    private final Consumer<String> openPage;
    private final Runnable accountAction;
    private final Runnable continueLearning;
    private final boolean loginEnabled;

    /** 기존 코드 및 테스트와의 호환용 생성자. */
    public MyPagePanel(LearningService service, Consumer<String> openPage,
                       Runnable accountAction, boolean loginEnabled) {
        this(service, openPage, accountAction, loginEnabled, () -> openPage.accept("missions"));
    }

    public MyPagePanel(LearningService service, Consumer<String> openPage,
                       Runnable accountAction, boolean loginEnabled, Runnable continueLearning) {
        super(new BorderLayout());
        this.service=service;
        this.openPage=openPage;
        this.accountAction=accountAction;
        this.loginEnabled=loginEnabled;
        this.continueLearning=continueLearning;
        setOpaque(false);
        add(scroll(content()),BorderLayout.CENTER);
    }

    private JPanel content() {
        JPanel body=column();
        body.setBorder(new EmptyBorder(18,30,28,30));
        Theme.add(body,label("마이페이지",30,INK,true)); gap(body,8);
        Theme.add(body,label("내 학습 기록과 즐겨찾기 메모를 한곳에서 확인하세요.",14,MUTED,false)); gap(body,25);
        Theme.add(body,topCards()); gap(body,20);
        Theme.add(body,label("학습 현황",21,INK,true)); gap(body,13);
        Theme.add(body,statistics()); gap(body,22);
        Theme.add(body,label("나의 학습 공간",21,INK,true)); gap(body,5);
        Theme.add(body,label("최근 진행한 내용으로 빠르게 돌아갈 수 있어요.",13,MUTED,false)); gap(body,15);
        Theme.add(body,quickLinks());
        return body;
    }

    private JPanel topCards() {
        JPanel pair=new JPanel(new GridLayout(1,2,16,0));
        pair.setOpaque(false);
        pair.setMaximumSize(new Dimension(Integer.MAX_VALUE,247));
        pair.setPreferredSize(new Dimension(920,247));
        pair.add(profileCard());pair.add(favoritesCard());
        return pair;
    }

    private JComponent profileCard() {
        DashboardStyle.GradientCard card=new DashboardStyle.GradientCard(22);
        card.setBorder(new EmptyBorder(18,24,18,24));
        card.setLayout(new BorderLayout(0,11));
        card.add(label("CURRENT PROFILE",11,SOFT,true),BorderLayout.NORTH);

        JPanel identity=new JPanel(new BorderLayout(17,0));identity.setOpaque(false);
        JPanel avatar=new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g=(Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(0xE9F5EF));
                int d=Math.min(getWidth(),getHeight())-2;
                g.fillOval((getWidth()-d)/2,(getHeight()-d)/2,d,d);
                g.dispose();super.paintComponent(graphics);
            }
        };
        avatar.setOpaque(false);avatar.setPreferredSize(new Dimension(65,65));
        String nickname=service.user().nickname();
        String initial=nickname.isEmpty()?"?":nickname.substring(0,1);
        avatar.add(label(initial,27,DashboardStyle.ACCENT,true));identity.add(avatar,BorderLayout.WEST);
        JPanel user=column();
        Theme.add(user,label(nickname,24,WHITE,true));gap(user,8);
        Theme.add(user,label(service.rank()+"  ·  "+service.xp()+" XP",13,new Color(0xD8EAE2),false));
        identity.add(user,BorderLayout.CENTER);card.add(identity,BorderLayout.CENTER);

        JPanel latest=new JPanel(new BorderLayout(0,10));latest.setOpaque(false);
        JSeparator separator=new JSeparator();separator.setForeground(new Color(0x639484));
        separator.setBackground(new Color(0x639484));latest.add(separator,BorderLayout.NORTH);
        JPanel bottom=new JPanel(new BorderLayout(0,7));bottom.setOpaque(false);
        bottom.add(label("최근 학습",12,WHITE,true),BorderLayout.NORTH);
        JPanel recent=new JPanel(new BorderLayout(12,0));recent.setOpaque(false);
        DashboardStyle.Surface diamond=new DashboardStyle.Surface(new Color(0x2A725A),14);
        diamond.setLayout(new GridBagLayout());diamond.setPreferredSize(new Dimension(55,55));
        diamond.add(new JLabel(new DashboardStyle.Glyph("diamond",34,new Color(0xE1F2E9))));
        recent.add(diamond,BorderLayout.WEST);
        String recentTitle=service.lastStudiedStep().map(s->"STEP "+s.number()+" · "+s.goal())
                .orElse("아직 진행한 학습이 없습니다.");
        JTextArea recentLabel=text(recentTitle,13,WHITE);recentLabel.setFont(font(13,true));recentLabel.setRows(2);
        recent.add(recentLabel,BorderLayout.CENTER);
        JButton cont=button("이어서 학습  →",false,continueLearning);
        cont.setToolTipText("다음 학습 단계의 문제를 시작합니다.");
        cont.setPreferredSize(new Dimension(130,41));
        recent.add(cont,BorderLayout.EAST);
        bottom.add(recent,BorderLayout.CENTER);latest.add(bottom,BorderLayout.CENTER);
        card.add(latest,BorderLayout.SOUTH);
        return card;
    }

    private JComponent favoritesCard() {
        DashboardStyle.Surface card=new DashboardStyle.Surface(WHITE,22);
        card.setBorder(new EmptyBorder(18,22,18,22));
        card.setLayout(new BorderLayout(0,10));
        JPanel header=new JPanel(new BorderLayout());header.setOpaque(false);
        JPanel title=new JPanel(new FlowLayout(FlowLayout.LEFT,9,0));title.setOpaque(false);
        title.add(new JLabel(new DashboardStyle.Glyph("star",25,DashboardStyle.DEEP)));
        title.add(label("즐겨찾기 메모",19,INK,true));
        header.add(title,BorderLayout.WEST);
        List<StudyNote> favorites=service.favoriteNotes();
        header.add(label(favorites.size()+"개",13,DashboardStyle.ACCENT,true),BorderLayout.EAST);
        card.add(header,BorderLayout.NORTH);

        if(favorites.isEmpty()) {
            // GridBagLayout으로 아이콘과 두 안내 문구를 개별적으로 중앙 정렬한다.
            // 기존 BoxLayout에서는 circle 패널이 가로로 늘어나 시각적 중심이 어긋날 수 있다.
            JPanel empty=new JPanel(new GridBagLayout());
            empty.setOpaque(false);
            JPanel message=new JPanel(new GridBagLayout());
            message.setOpaque(false);
            GridBagConstraints ec=new GridBagConstraints();
            ec.gridx=0;
            ec.anchor=GridBagConstraints.CENTER;
            ec.fill=GridBagConstraints.NONE;
            JPanel circle=new JPanel(new GridBagLayout()) {
                @Override protected void paintComponent(Graphics graphics) {
                    Graphics2D g=(Graphics2D)graphics.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(new Color(0xE8F3EC));
                    int diameter=Math.min(getWidth(),getHeight())-1;
                    g.fillOval((getWidth()-diameter)/2,(getHeight()-diameter)/2,diameter,diameter);
                    g.dispose();
                    super.paintComponent(graphics);
                }
            };
            circle.setOpaque(false);
            circle.setPreferredSize(new Dimension(72,72));
            circle.setMinimumSize(new Dimension(72,72));
            circle.setMaximumSize(new Dimension(72,72));
            circle.add(new JLabel(new DashboardStyle.Glyph("star",35,DashboardStyle.ACCENT)));
            ec.gridy=0;
            ec.insets=new Insets(0,0,14,0);
            message.add(circle,ec);
            JLabel info=label("즐겨찾기한 메모가 아직 없어요.",16,INK,true);
            info.setHorizontalAlignment(SwingConstants.CENTER);
            ec.gridy=1;
            ec.insets=new Insets(0,0,7,0);
            message.add(info,ec);
            JLabel hint=label("나의 메모에서 즐겨찾기를 눌러 추가하세요.",12,MUTED,false);
            hint.setHorizontalAlignment(SwingConstants.CENTER);
            ec.gridy=2;
            ec.insets=new Insets(0,0,0,0);
            message.add(hint,ec);
            empty.add(message,new GridBagConstraints());
            card.add(empty,BorderLayout.CENTER);
            return card;
        }

        DefaultTableModel model=new DefaultTableModel(new String[]{"관련 규칙","메모 내용"},0) {
            @Override public boolean isCellEditable(int r,int c){return false;}
        };
        for(StudyNote note:favorites) {
            String ruleTitle=service.catalog().rule(note.ruleId()).title();
            String summary=note.text().replace('\r',' ').replace('\n',' ');
            if(summary.length()>65)summary=summary.substring(0,65)+"…";
            model.addRow(new Object[]{ruleTitle,summary});
        }
        JTable table=new JTable(model) {
            @Override public String getToolTipText(MouseEvent event) {
                int row=rowAtPoint(event.getPoint());
                if(row<0 || row>=getRowCount())return null;
                return "<html>"+Theme.html(favorites.get(convertRowIndexToModel(row)).text())+"</html>";
            }
        };
        table.setFont(font(12,false));table.setRowHeight(34);table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0,0));table.setSelectionBackground(PALE);
        table.setSelectionForeground(INK);table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(font(12,true));table.getTableHeader().setBackground(PALE);
        table.getColumnModel().getColumn(0).setPreferredWidth(120);
        table.getColumnModel().getColumn(1).setPreferredWidth(270);
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if(e.getClickCount()==2 && table.getSelectedRow()>=0)openPage.accept("notes");
            }
        });
        table.setToolTipText("두 번 클릭하면 나의 메모로 이동합니다.");
        JScrollPane pane=new JScrollPane(table);
        pane.setBorder(BorderFactory.createLineBorder(LINE));
        pane.getViewport().setBackground(WHITE);pane.getVerticalScrollBar().setUnitIncrement(20);
        card.add(pane,BorderLayout.CENTER);
        return card;
    }

    private JPanel statistics() {
        DashboardStyle.Surface surface=new DashboardStyle.Surface(WHITE,22);
        surface.setLayout(new GridBagLayout());
        surface.setBorder(new EmptyBorder(15,22,15,22));
        surface.setMaximumSize(new Dimension(Integer.MAX_VALUE,141));
        surface.setPreferredSize(new Dimension(960,141));
        GridBagConstraints c=new GridBagConstraints();c.gridy=0;c.fill=GridBagConstraints.BOTH;
        c.weighty=1;c.insets=new Insets(0,0,0,0);
        c.gridx=0;c.weightx=1.45;surface.add(progressSummary(),c);
        c.gridx=1;c.weightx=0;c.fill=GridBagConstraints.VERTICAL;
        surface.add(divider(),c);
        c.gridx=2;c.weightx=1; c.fill=GridBagConstraints.BOTH;
        surface.add(statCell("힌트 없이 해결",service.solvedCount()+"개"),c);
        c.gridx=3;c.weightx=0;c.fill=GridBagConstraints.VERTICAL;
        surface.add(divider(),c);
        c.gridx=4;c.weightx=1;c.fill=GridBagConstraints.BOTH;
        surface.add(statCell("누적 경험치",service.xp()+" XP"),c);
        JPanel out=new JPanel(new BorderLayout());out.setOpaque(false);
        out.add(surface,BorderLayout.CENTER);
        out.setPreferredSize(new Dimension(960,141));out.setMaximumSize(new Dimension(Integer.MAX_VALUE,141));
        return out;
    }

    private JComponent divider() {
        JPanel line=new JPanel();line.setBackground(new Color(0xE3E9E5));
        line.setPreferredSize(new Dimension(1,85));
        return line;
    }
    private JPanel progressSummary() {
        JPanel p=new JPanel(new FlowLayout(FlowLayout.CENTER,14,0));p.setOpaque(false);
        p.add(new CircularProgress(service.completedSteps(),service.steps().size()));
        JPanel text=column();text.setBorder(new EmptyBorder(15,0,0,0));
        Theme.add(text,label("전체 학습 진행률",14,INK,true));gap(text,12);
        Theme.add(text,label(service.completedSteps()+" / "+service.steps().size()+"단계",13,MUTED,false));
        p.add(text);return p;
    }
    private JPanel statCell(String title,String value) {
        JPanel p=new JPanel(new GridBagLayout());p.setOpaque(false);
        JPanel contents=column();
        Theme.add(contents,label(title,15,INK,true));gap(contents,11);
        Theme.add(contents,label(value,29,DashboardStyle.DEEP,true));
        p.add(contents);return p;
    }

    private JPanel quickLinks() {
        JPanel links=new JPanel(new GridLayout(1,3,16,0));links.setOpaque(false);
        links.setPreferredSize(new Dimension(960,144));
        links.setMaximumSize(new Dimension(Integer.MAX_VALUE,144));
        String lastStep=service.lastStudiedStep().map(s->"최근 진행:\nSTEP "+s.number()+" · "+s.goal())
                .orElse("최근 진행:\n아직 진행한 학습이 없습니다.");
        String lastAttempt=service.lastAttempt().map(a -> {
            String title=service.catalog().question(a.questionId()).map(Question::title).orElse("이전 문제");
            String when=DateTimeFormatter.ofPattern("MM.dd HH:mm")
                    .withZone(ZoneId.systemDefault()).format(a.at());
            return "최근 풀이: "+title+"\n"+when+(a.understood()?" · 이해 완료":" · 다시 학습");
        }).orElse("아직 제출한 문제 풀이가 없습니다.");
        String noteInfo="작성한 메모 "+service.notes("").size()+"개 · 즐겨찾기 "+service.favoriteNotes().size()+"개";
        links.add(new QuickLinkButton("path","01  MY PATH","나의 학습 경로",lastStep,()->openPage.accept("missions")));
        links.add(new QuickLinkButton("growth","02  MY SCOREBOOK","나의 성장",lastAttempt,()->openPage.accept("records")));
        links.add(new QuickLinkButton("notes","03  MY NOTES","나의 메모",noteInfo,()->openPage.accept("notes")));
        return links;
    }

    /** 카드 전체를 클릭할 수 있고 키보드 Enter/Space로도 동작한다. */
    private static final class QuickLinkButton extends JButton {
        private final String glyph,kicker,title,summary;
        QuickLinkButton(String glyph,String kicker,String title,String summary,Runnable action) {
            super("바로가기: "+title);
            this.glyph=glyph;this.kicker=kicker;this.title=title;this.summary=summary;
            setOpaque(false);setContentAreaFilled(false);setFocusPainted(false);
            setBorder(new EmptyBorder(0,0,0,0));setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(title+" 화면 열기");
            addActionListener(e->action.run());
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g=(Graphics2D)graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(),h=getHeight();
            g.setColor(isEnabled()?(getModel().isRollover()?new Color(0xF0F8F3):WHITE):new Color(0xF7F7F7));
            g.fillRoundRect(0,0,w-1,h-1,21,21);
            if(hasFocus()) {g.setColor(DashboardStyle.ACCENT);g.setStroke(new BasicStroke(2));
                g.drawRoundRect(2,2,w-5,h-5,19,19);}
            // 세 카드의 아이콘 배경과 실제 그림을 동일한 중심선에 배치한다.
            final int boxSize=66;
            int iconX=18;
            int iconY=(h-boxSize)/2;
            g.setColor(DashboardStyle.MINT);
            g.fillRoundRect(iconX,iconY,boxSize,boxSize,14,14);
            final int artSize=42;
            MyPageIcons.paint(g,glyph,iconX+(boxSize-artSize)/2,iconY+(boxSize-artSize)/2,artSize);
            int tx=iconX+boxSize+16;
            int mid=h/2;
            g.setFont(font(11,true));g.setColor(DashboardStyle.ACCENT);
            g.drawString(kicker,tx,mid-30);
            g.setFont(font(18,true));g.setColor(INK);g.drawString(title,tx,mid-6);
            g.setFont(font(12,false));g.setColor(MUTED);
            drawWrapped(g,summary,tx,mid+17,Math.max(70,w-tx-15),3);
            g.dispose();
        }
        private static void drawWrapped(Graphics2D g,String value,int x,int firstY,int maxWidth,int maxLines) {
            FontMetrics fm=g.getFontMetrics();
            java.util.List<String> lines=new java.util.ArrayList<>();
            for(String raw:value.split("\\n",-1)) {
                StringBuilder row=new StringBuilder();
                for(int i=0;i<raw.length();i++) {
                    char ch=raw.charAt(i);
                    if(row.length()>0 && fm.stringWidth(row.toString()+ch)>maxWidth) {
                        lines.add(row.toString());row.setLength(0);
                    }
                    row.append(ch);
                }
                lines.add(row.toString());
            }
            for(int i=0;i<Math.min(maxLines,lines.size());i++) {
                String line=lines.get(i);
                if(i==maxLines-1 && lines.size()>maxLines) {
                    while(!line.isEmpty() && fm.stringWidth(line+"…")>maxWidth)line=line.substring(0,line.length()-1);
                    line+="…";
                }
                g.drawString(line,x,firstY+i*17);
            }
        }
    }
}
