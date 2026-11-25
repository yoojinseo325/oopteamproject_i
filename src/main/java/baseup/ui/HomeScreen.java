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

/** 메인 홈: BASEUP 로고, 타격 게임 + 카운트판, 오늘의 규칙 슬라이드, 다음 학습, 규칙 검색. */
final class HomeScreen extends Screen {
    HomeScreen(MainFrame app) { super(app); }

    @Override JComponent build() {
        boolean done=service.courseComplete(); int total=service.steps().size(), completed=service.completedSteps();
        int current=Math.min(service.nextStepNumber(),total);
        Ballpark.Stadium stadium=new Ballpark.Stadium(java.nio.file.Path.of("data","images","stadium_night.jpg"));
        JPanel p=column(); p.setBorder(new EmptyBorder(26,34,30,34));
        // ⓪ BASEUP 워드마크
        Team team=app.favoriteTeam();
        Color brand=team==null?new Color(0xD7263D):new Color(team.color());
        Theme.add(p,new HomeParts.Wordmark(brand,"야구 규칙, 한 베이스씩 올라가요  ·  판정하고, 이해하고, 성장하는 BASEUP"));gap(p,14);
        // 타격 게임 판 (아래 ②에서 화면에 붙인다)
        Ballpark.Glass board=new Ballpark.Glass(new Color(10,13,18,245)); board.setLayout(new BorderLayout(18,16));
        board.setBorder(new EmptyBorder(18,18,22,22));
        PitchGame game=new PitchGame(team==null?null:new Color(team.color()),app.gameState());
        board.add(game,BorderLayout.CENTER);
        // 오른쪽(또는 아래) 칸: [카운트판 묶음] + [조작·다음 목표 묶음]
        JPanel countPart=column();
        JPanel boardTitle=new JPanel(new FlowLayout(FlowLayout.LEFT,6,0));boardTitle.setOpaque(false);
        boardTitle.add(label("●",12,new Color(0xFF4D4D),true));boardTitle.add(label("PLAY BALL  ·  타격 게임",13,WHITE,true));
        boardTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE,26));Theme.add(countPart,boardTitle);gap(countPart,10);
        GameBoard count=new GameBoard(game);count.setAlignmentX(0f);count.setMinimumSize(count.getPreferredSize());countPart.add(count);gap(countPart,8);
        Theme.add(countPart,label("B 볼 · S 스트라이크 · O 아웃",12,new Color(0xB8C6D6),true));gap(countPart,3);
        Theme.add(countPart,label("R 점수 · H 안타 · IN 이닝 · 아래 마름모 = 주자",12,new Color(0xB8C6D6),false));
        JPanel actionPart=column();
        // 게임 조작: 시작/일시정지/계속(3아웃 뒤에는 다음 이닝)
        JPanel controls=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));controls.setOpaque(false);
        JButton pause=button("▶ 시작",false,game::togglePause);pause.setName("game-pause");
        controls.add(pause);controls.add(label("  클릭 · 스페이스바 = 스윙!",13,Ballpark.LAMP,true));
        controls.setMaximumSize(new Dimension(Integer.MAX_VALUE,50));Theme.add(actionPart,controls);gap(actionPart,12);
        // 이번 이닝에 만난 규칙: 칩을 누르면 그 규칙 카드로 바로 간다
        Theme.add(actionPart,label("이번 이닝에 만난 규칙  (누르면 규칙 카드로)",12,new Color(0xB8C6D6),true));gap(actionPart,6);
        JPanel chips=new JPanel(new FlowLayout(FlowLayout.LEFT,6,4));chips.setOpaque(false);
        chips.setMaximumSize(new Dimension(Integer.MAX_VALUE,76));Theme.add(actionPart,chips);gap(actionPart,14);
        Runnable sync=()->{
            pause.setText(game.controlLabel());
            List<String> met=game.metRules();
            if(met.size()==chips.getComponentCount()&&!met.isEmpty())return;
            chips.removeAll();
            if(met.isEmpty()) chips.add(label("아직 없어요 · 게임을 하면 여기에 쌓여요",12,new Color(0x8592A3),false));
            for(String m:met) chips.add(ruleChip(m));
            chips.revalidate();chips.repaint();
        };
        game.addListener(sync);sync.run();
        // 처음 온 사람을 위한 안내문
        RoundedPanel guide=new RoundedPanel(new Color(255,255,255,18),14);guide.setLayout(new BorderLayout(0,8));guide.setBorder(new EmptyBorder(12,14,12,14));
        JPanel steps=column();
        Theme.add(steps,label("처음이라면 이렇게 해 보세요",14,WHITE,true));gap(steps,6);
        for(String line:new String[]{"① 타격 게임으로 볼·스트라이크·파울 상황을 직접 겪어 봐요",
                "② 위에 쌓인 규칙 칩을 누르면 그 규칙 설명으로 가요",
                "③ 규칙 학습에서 STEP을 하나씩 풀며 판정을 배워요",
                "④ 헷갈린 장면은 퀴즈에서 다시 풀어요"}){Theme.add(steps,label(line,12,new Color(0xD3DCE7),false));gap(steps,3);}
        guide.add(steps,BorderLayout.CENTER);
        ActionButton start=button(app.nextAction(),true,app::startNext);start.teamColor(Ballpark.LAMP);
        JPanel startBox=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));startBox.setOpaque(false);startBox.add(start);guide.add(startBox,BorderLayout.SOUTH);
        guide.setMaximumSize(new Dimension(Integer.MAX_VALUE,guide.getPreferredSize().height));Theme.add(actionPart,guide);
        JPanel side=new JPanel();side.setOpaque(false);
        JPanel below=column();
        Ballpark.XpRunway runway=new Ballpark.XpRunway(service.xp(),service.catalog().questions().size()*30);
        runway.setPreferredSize(new Dimension(900,74));runway.setMaximumSize(new Dimension(Integer.MAX_VALUE,74));
        board.add(below,BorderLayout.SOUTH);
        // 창 너비에 따라 배치 바꾸기: 넓으면 오른쪽 세로 칸, 좁으면 게임 아래 가로 두 칸
        boolean[] wide={false,true};
        Runnable arrange=()->{
            boolean w=board.getWidth()==0||board.getWidth()>=1300;
            if(wide[1]==false&&wide[0]==w)return; wide[1]=false; wide[0]=w;
            side.removeAll();below.removeAll();board.remove(side);
            if(w){side.setLayout(new BoxLayout(side,BoxLayout.Y_AXIS));side.add(Box.createRigidArea(new Dimension(340,0)));
                countPart.setAlignmentX(0f);actionPart.setAlignmentX(0f);side.add(countPart);side.add(Box.createVerticalStrut(18));side.add(actionPart);
                board.add(side,BorderLayout.EAST);}
            else{side.setLayout(new GridLayout(1,2,28,0));side.add(countPart);side.add(actionPart);side.setAlignmentX(0f);
                below.add(side);below.add(Box.createVerticalStrut(16));}
            runway.setAlignmentX(0f);below.add(runway);board.revalidate();board.repaint();
        };
        arrange.run();
        board.addComponentListener(new java.awt.event.ComponentAdapter(){@Override public void componentResized(java.awt.event.ComponentEvent e){arrange.run();}});
        Color accent=brand;
        // ① 오늘의 규칙 (메인): 설명 + 오늘의 콜(바로 풀기) + 오늘의 미션(게임과 연결)
        Theme.add(p,new TodayRuleCard(app,service,accent,game));gap(p,26);
        // ② 직접 쳐 보며 배우기: 타격 게임
        JPanel gameTitle=new JPanel(new FlowLayout(FlowLayout.LEFT,10,0));gameTitle.setOpaque(false);
        gameTitle.add(HomeParts.iconBox("ball",22,new Color(0xFFC94A)));gameTitle.add(label("직접 쳐 보며 배우기",20,WHITE,true));
        gameTitle.add(label("타격 게임에서 만난 상황은 규칙 카드로 바로 이어져요",13,new Color(0xB8C6D6),false));
        gameTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE,34));Theme.add(p,gameTitle);gap(p,10);
        board.setMaximumSize(new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE));Theme.add(p,board);gap(p,26);
        // ③ 다음에 배워볼까요?
        JPanel title=new JPanel(new BorderLayout());title.setOpaque(false);
        JPanel tl=new JPanel(new FlowLayout(FlowLayout.LEFT,10,0));tl.setOpaque(false);
        tl.add(HomeParts.iconBox("star",26,accent));tl.add(label("다음에 배워볼까요?",21,WHITE,true));
        title.add(tl,BorderLayout.WEST);
        title.add(button("전체 보기   ›",false,()->app.showPage("missions")),BorderLayout.EAST);
        title.setMaximumSize(new Dimension(Integer.MAX_VALUE,46));Theme.add(p,title);gap(p,12);
        Theme.add(p,nextLessons(accent));gap(p,26);
        // ④ 규칙 검색 (비디오 판독)
        Ballpark.Glass find=new Ballpark.Glass(new Color(10,13,18,240));find.setLayout(new BorderLayout(0,12));find.setBorder(new EmptyBorder(18,22,18,22));
        JPanel findTitle=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));findTitle.setOpaque(false);
        findTitle.add(label("●",14,new Color(0xFF4D4D),true));findTitle.add(label("RULE FINDER",14,WHITE,true));
        findTitle.add(label("비디오 판독 요청  ·  헷갈리는 규칙을 검색해 보세요",13,new Color(0xB8C6D6),false));
        find.add(findTitle,BorderLayout.NORTH);
        RuleSearchBox search=new RuleSearchBox(service,app::openRule);find.add(search,BorderLayout.CENTER);
        find.setMaximumSize(new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE));Theme.add(p,find);
        JScrollPane sc=scroll(p); sc.setOpaque(false);
        // 타격 게임 중 화면이 갑자기 맨 위로 튀지 않게: 포커스·글자 변경이 보내는 자동 스크롤 요청(scrollRectToVisible)을 무시하고,
        // 사용자가 직접 굴린 스크롤(휠·스크롤바)만 따른다.
        JViewport stable=new JViewport(){ @Override public void scrollRectToVisible(Rectangle r){ } };
        stable.setView(sc.getViewport().getView()); stable.setOpaque(false); stable.setBackground(sc.getViewport().getBackground());
        sc.setViewport(stable);
        stadium.add(sc,BorderLayout.CENTER);
        return stadium;
    }
    /** 오늘의 규칙 슬라이드: 발견한 규칙을 하루 하나씩, 양옆 화살표와 아래 점으로 넘겨 본다. */
    JComponent nextLessons(Color accent) {
        int total=service.steps().size(), current=Math.min(service.nextStepNumber(),total);
        int from=Math.max(1,Math.min(current,total-2));
        JPanel row=new JPanel(new GridLayout(1,3,16,0));row.setOpaque(false);
        String[] icons={"ball","glove","diamond"};
        for(int i=0;i<3&&from+i<=total;i++) {
            int n=from+i; LearningStep step=service.step(n);
            boolean done=service.stepCompleted(n), unlocked=service.stepUnlocked(n);
            HomeParts.LessonCard card=new HomeParts.LessonCard();card.setLayout(new BorderLayout(0,12));
            JPanel top=new JPanel(new FlowLayout(FlowLayout.LEFT,10,0));top.setOpaque(false);
            top.add(HomeParts.iconBox(icons[i],34,accent));
            RoundedPanel chip=new RoundedPanel(unlocked?accent:new Color(0x9AA3AE),14);chip.setBorder(new EmptyBorder(3,10,3,10));chip.setLayout(new BorderLayout());
            chip.add(label("STEP "+n+(done?"  ✓":""),12,WHITE,true));top.add(chip);
            card.add(top,BorderLayout.NORTH);
            JPanel mid=column();
            Theme.add(mid,label(unlocked?step.goal():"아직 열리지 않은 단계",18,unlocked?INK:MUTED,true));gap(mid,6);
            Theme.add(mid,label(done?"다시 도전해 볼까요?":unlocked?"지금 도전할 차례예요!":"앞 단계를 마치면 열려요",13,MUTED,false));
            card.add(mid,BorderLayout.CENTER);
            JButton go=HomeParts.circle(true,true,()->{
                if(!unlocked){app.message("앞 단계를 먼저 마쳐 주세요.");return;}
                try { app.showQuiz(service.startStep(n),"STEP "+n); } catch(Exception e) { app.error(e); }
            });
            go.setName("lesson-"+n);go.setPreferredSize(new Dimension(36,36));
            JPanel gw=new JPanel(new FlowLayout(FlowLayout.RIGHT,0,0));gw.setOpaque(false);gw.add(go);card.add(gw,BorderLayout.SOUTH);
            row.add(card);
        }
        row.setPreferredSize(new Dimension(900,190));row.setMaximumSize(new Dimension(Integer.MAX_VALUE,190));
        return row;
    }
    /** 사이드 메뉴 4개 중 어느 칸을 켤지: 성장·규칙·메모는 마이페이지 안에 들어 있다. */
    /** 게임에서 만난 규칙 칩: 배운 규칙이면 카드로, 아직이면 몇 STEP에서 배우는지 알려 준다. */
    JComponent ruleChip(String situation) {
        String id=baseup.game.GameState.ruleIdOf(situation);
        JButton chip=new JButton(situation+(id==null?"":"  ›"));chip.setFont(Theme.font(12,true));
        chip.setFocusPainted(false);chip.setBorder(new EmptyBorder(5,10,5,10));chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boolean open=id!=null&&service.discovered(id);
        chip.setBackground(open?Ballpark.LAMP:new Color(0x2A3442));chip.setForeground(open?new Color(0x1A1A1A):WHITE);chip.setOpaque(true);
        chip.setToolTipText(id==null?"규칙 카드가 아직 없는 상황이에요":open?"규칙 카드 열기":RuleSearchBox.stepHint(service,id));
        chip.addActionListener(e->{
            if(id==null) app.message("'"+situation+"' 규칙 카드는 아직 준비 중이에요.");
            else if(service.discovered(id)) app.openRule(id);
            else { app.message("'"+situation+"'은(는) "+RuleSearchBox.stepHint(service,id)+". 규칙 학습에서 그 STEP을 풀면 카드가 열려요."); }
        });
        return chip;
    }
}
