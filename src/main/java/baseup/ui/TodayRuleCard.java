package baseup.ui;

import baseup.model.LearningStep;
import baseup.model.Question;
import baseup.model.RuleConcept;
import baseup.service.LearningService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static baseup.ui.Theme.*;

/**
 * 메인 홈의 주인공 카드: 오늘의 규칙.
 *
 *  - 왼쪽: 규칙 그림 / 가운데: 규칙 설명 / 오른쪽: 오늘의 콜(그 규칙 문제를 바로 풀어 보는 연습)
 *  - 아래: 오늘의 미션(아래 타격 게임에서 이 규칙 상황 만나기) + 다음 규칙까지 남은 시간
 *  - 하루 5장 슬라이드: 배운 규칙을 날짜별로 돌려 가며 고르고, 모자라면 다음에 배울 규칙을 '맛보기'로 채운다.
 *  - 9초마다 자동으로 넘어가고, 마우스를 올리거나 오늘의 콜을 푸는 동안에는 멈춘다.
 * 연습 문제는 기록(XP)에 남기지 않는다.
 */
final class TodayRuleCard extends JPanel {
    private final MainFrame app;
    private final LearningService service;
    private final Color accent;
    private final PitchGame game;
    private final List<RuleConcept> rules = new ArrayList<>();
    static final int SLIDES = 5;                   // 하루에 보여 줄 규칙 수
    private final java.util.Set<String> previewIds = new java.util.HashSet<>();   // 아직 안 배운(맛보기) 규칙
    private boolean preview;                       // 지금 슬라이드가 맛보기인지
    private final JLabel previewTag = tag("맛보기", new Color(0xFFB800), INK);
    private final Timer autoplay = new Timer(9000, e -> advance());
    private long pausedUntil; private JComponent card;
    private int index;
    private boolean missionDone, wasNarrow, wasWide;
    private boolean narrow() { return getWidth() > 0 && getWidth() < 1350; }
    /** 전체화면처럼 아주 넓을 때: 오른쪽에 오늘의 5장 목록, 설명 아래 정보 칩을 더 보여 준다 */
    private boolean wide() { return getWidth() >= 1800; }

    private final JPanel body = new JPanel(new BorderLayout(22, 0));
    private final JLabel clock = label("", 13, MUTED, true);
    private final JLabel counter = label("", 13, MUTED, true);
    private final JLabel mission = label("", 14, INK, true);
    private final JLabel missionState = label("", 13, WHITE, true);
    private final HomeParts.Dots dots;
    private final Timer ticker = new Timer(1000, e -> tick());

    TodayRuleCard(MainFrame app, LearningService service, Color accent, PitchGame game) {
        super(new BorderLayout(0, 10));
        this.app = app; this.service = service; this.accent = accent; this.game = game;
        setOpaque(false);
        pickToday();
        index = rules.isEmpty() ? 0 : app.todaySlide() % rules.size();   // 다른 메뉴에 갔다 와도 보던 장부터
        dots = new HomeParts.Dots(accent);

        Ballpark.Glass card = new Ballpark.Glass(new Color(255, 255, 255, 248));
        card.setLayout(new BorderLayout(0, 14)); card.setBorder(new EmptyBorder(20, 26, 18, 26));
        card.add(header(), BorderLayout.NORTH);
        this.card = card;
        body.setOpaque(false); card.add(body, BorderLayout.CENTER);
        card.add(missionBar(), BorderLayout.SOUTH);

        JPanel stage = new JPanel(new BorderLayout(14, 0)); stage.setOpaque(false);
        stage.add(card, BorderLayout.CENTER);
        if (rules.size() > 1) {
            stage.add(arrow(false), BorderLayout.WEST);
            stage.add(arrow(true), BorderLayout.EAST);
        }
        add(stage, BorderLayout.CENTER);
        add(dots, BorderLayout.SOUTH);
        if (game != null) game.addListener(this::checkMission);
        render();
        // 창 너비가 넓음↔좁음으로 바뀌면 그림·오늘의 콜 크기를 다시 맞춘다
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) { if (narrow() != wasNarrow || wide() != wasWide) { tallest = 0; render(); } }
        });
    }

    @Override public void addNotify() { super.addNotify(); ticker.start(); tick(); if (rules.size() > 1) autoplay.start(); }
    @Override public void removeNotify() { ticker.stop(); autoplay.stop(); super.removeNotify(); }

    /**
     * 오늘의 5장 고르기: 첫 장은 전체 40개 규칙 중 오늘의 규칙(DailyRuleService, 기능 2),
     * 나머지는 배운 규칙(날짜마다 시작 위치가 바뀜) → 모자라면 STEP 순서대로 다음에 배울 규칙.
     */
    private void pickToday() {
        rules.add(new baseup.service.DailyRuleService(service.catalog(), java.time.Clock.systemDefaultZone()).today());
        List<RuleConcept> learned = new ArrayList<>();
        service.catalog().rules().stream().filter(r -> service.discovered(r.id())).forEach(learned::add);
        int start = learned.isEmpty() ? 0 : LocalDate.now().getDayOfYear() % learned.size();
        for (int i = 0; i < learned.size() && rules.size() < SLIDES; i++) {
            RuleConcept r = learned.get((start + i) % learned.size());
            if (!rules.contains(r)) rules.add(r);
        }
        for (RuleConcept r : upcomingRules()) {
            if (rules.size() >= SLIDES) break;
            if (!rules.contains(r)) { rules.add(r); previewIds.add(r.id()); }
        }
    }
    /** 다음 슬라이드로 (자동 넘김). 마우스가 올라가 있거나 방금 문제를 풀었으면 쉰다. */
    private void advance() {
        if (mouseOnCard() || System.currentTimeMillis() < pausedUntil || rules.size() < 2) return;
        index = (index + 1) % rules.size(); render();
    }
    @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
    /**
     * 슬라이드가 자동으로 넘어갈 때 카드 높이가 줄었다 늘었다 하면 아래 타격 게임이 위아래로 밀린다.
     * 그래서 지금까지 가장 컸던 높이를 유지한다(창 너비가 바뀌어 배치가 달라질 때만 다시 잰다).
     */
    private int tallest;
    @Override public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        tallest = Math.max(tallest, d.height);
        return new Dimension(d.width, tallest);
    }

    // ---------- 위: 제목 줄 ----------
    private JComponent header() {
        JPanel head = new JPanel(new BorderLayout()); head.setOpaque(false);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); left.setOpaque(false);
        left.add(HomeParts.iconBox("ball", 24, accent));
        left.add(label("오늘의 규칙", 18, INK, true));
        left.add(previewTag);
        head.add(left, BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0)); right.setOpaque(false);
        right.add(clock); right.add(counter);
        head.add(right, BorderLayout.EAST);
        return head;
    }

    // ---------- 가운데: 그림 + 설명 + 오늘의 콜 ----------
    private void render() {
        body.removeAll();
        if (rules.isEmpty()) { body.add(label("규칙 데이터를 불러오지 못했어요.", 16, MUTED, false)); return; }
        RuleConcept r = rules.get(index);
        app.setTodaySlide(index);
        preview = previewIds.contains(r.id()); previewTag.setVisible(preview);
        missionDone = false; wasNarrow = narrow(); wasWide = wide();
        int picW = wasNarrow ? 270 : 470, picH = wasNarrow ? 270 : wasWide ? 340 : 310, callW = wasNarrow ? 330 : 430;

        JPanel pic = new JPanel(new BorderLayout()); pic.setOpaque(false);
        pic.setPreferredSize(new Dimension(picW, picH));
        pic.add(new HomeParts.SlidePicture(picture(r)));
        body.add(pic, BorderLayout.WEST);

        JPanel middle = new JPanel(new BorderLayout(22, 0)); middle.setOpaque(false);
        JPanel text = column();
        Theme.add(text, tag(r.topic(), new Color(0xEEF3F0), GREEN)); gap(text, 10);
        Theme.add(text, label(r.title(), wasNarrow ? 30 : 36, INK, true)); gap(text, 12);
        String more = r.detail().startsWith(r.summary()) ? r.detail() : r.summary() + "\n" + r.detail();
        JTextArea about = Screen.paragraph(more, 3); about.setFont(Theme.font(wasNarrow ? 15 : 17, false)); Theme.add(text, about); gap(text, 12);
        // 흔한 착각: 그 규칙 문제에 적힌 '많이 헷갈리는 점' 한 줄
        // (없으면 '알아두기' 힌트)
        String myth = null, mythTitle = "흔한 착각";
        for (Question x : service.catalog().questions()) if (x.ruleId().equals(r.id()) && !x.misconception().isBlank()) { myth = x.misconception(); break; }
        if (myth == null) for (Question x : service.catalog().questions()) if (x.ruleId().equals(r.id()) && !x.hint().isBlank() && !more.contains(x.hint())) { myth = x.hint(); mythTitle = "알아두기"; break; }
        if (myth != null && (more.contains(myth) || myth.contains(r.summary()))) myth = null;   // 위 설명과 같은 말이면 생략
        if (myth != null) {
            Theme.RoundedPanel box = new Theme.RoundedPanel(new Color(0xFFF6DA), 12);
            box.setLayout(new BorderLayout(10, 0)); box.setBorder(new EmptyBorder(8, 12, 8, 12));
            box.add(label(mythTitle, 12, new Color(0xA36A00), true), BorderLayout.WEST);
            JTextArea m = Theme.text(myth, 13, INK); m.setOpaque(false); box.add(m, BorderLayout.CENTER);
            Theme.add(text, box); gap(text, 14);
        }
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); actions.setOpaque(false);
        if (preview) {
            actions.add(HomeParts.pill("규칙 학습에서 배우기", accent, app::startNext));
            if (!wasWide) {                                       // 넓은 화면은 위 정보 칩에 이미 나옴
                actions.add(Box.createHorizontalStrut(12));
                actions.add(label(RuleSearchBox.stepHint(service, r.id()), 13, MUTED, true));
            }
        }
        else actions.add(HomeParts.pill("규칙 카드 보기", accent, () -> app.openRule(r.id())));
        if (wasWide) { Theme.add(text, facts(r)); gap(text, 16); }
        Theme.add(text, actions);
        middle.add(text, BorderLayout.CENTER);
        JComponent call = todaysCall(r); call.setPreferredSize(new Dimension(callW, call.getPreferredSize().height));
        middle.add(call, BorderLayout.EAST);
        body.add(middle, BorderLayout.CENTER);
        if (wasWide) body.add(playlist(), BorderLayout.EAST);

        counter.setText(rules.size() > 1 ? (index + 1) + " / " + rules.size() : "");
        dots.set(rules.size(), index);
        String situation = situationOf(r.id());
        mission.setText(situation == null
                ? "오늘의 미션  ·  오른쪽 '오늘의 콜'을 맞혀 보세요"
                : "오늘의 미션  ·  아래 타격 게임에서 '" + situation + "' 상황을 만나 보세요");
        setMission(false);
        if (situation != null && game != null && game.metRules().contains(situation)) setMission(true);
        body.revalidate(); body.repaint();
    }

    /** 오늘의 콜: 이 규칙 문제 하나를 바로 풀어 본다 (연습, 기록 안 남음). */
    private JComponent todaysCall(RuleConcept r) {
        Theme.RoundedPanel box = new Theme.RoundedPanel(new Color(0x111823), 18);
        box.setLayout(new BorderLayout(0, 10)); box.setBorder(new EmptyBorder(16, 18, 16, 18));
        // 이 규칙의 문제가 여러 개면 날짜마다 다른 문제가 나온다
        List<Question> pool = service.catalog().questions().stream().filter(x -> x.ruleId().equals(r.id()))
                .filter(x -> x.choices().size() >= 2).toList();
        Question q = pool.isEmpty() ? null : pool.get(LocalDate.now().getDayOfYear() % pool.size());
        JPanel top = column();
        JPanel title = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0)); title.setOpaque(false);
        title.add(label("●", 11, new Color(0xFF4D4D), true)); title.add(label("오늘의 콜  ·  심판이라면?", 14, Ballpark.LAMP, true));
        title.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24)); Theme.add(top, title); gap(top, 8);
        if (q == null) {
            Theme.add(top, Theme.text("이 규칙은 아직 연습 문제가 없어요. 규칙 카드에서 자세히 읽어 보세요.", 13, new Color(0xC9D3DE)));
            box.add(top, BorderLayout.NORTH); return box;
        }
        String scene = q.hasImage() || q.situation().startsWith("그림과") ? "" : q.situation();
        if (!scene.isBlank()) { JTextArea s = Theme.text(scene, 13, new Color(0xC9D3DE)); s.setOpaque(false); Theme.add(top, s); gap(top, 6); }
        JTextArea ask = Theme.text(q.prompt(), 15, WHITE); ask.setOpaque(false); ask.setFont(Theme.font(15, true)); Theme.add(top, ask);
        box.add(top, BorderLayout.NORTH);

        JTextArea feedback = Theme.text("", 13, WHITE); feedback.setOpaque(false);
        JPanel choices = new JPanel(new GridLayout(0, q.choices().size() <= 2 ? 2 : 2, 8, 8)); choices.setOpaque(false);
        List<JButton> buttons = new ArrayList<>();
        for (int i = 0; i < q.choices().size(); i++) {
            int pick = i;
            JButton b = new JButton(q.choices().get(i));
            b.setFont(Theme.font(14, true)); b.setFocusPainted(false); b.setOpaque(true);
            b.setBackground(new Color(0x243042)); b.setForeground(WHITE); b.setBorder(new EmptyBorder(9, 10, 9, 10));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.addActionListener(e -> {
                boolean right = q.isAnswerCorrect(pick);
                pausedUntil = System.currentTimeMillis() + 30_000;          // 해설 읽을 시간 30초
                for (int k = 0; k < buttons.size(); k++) {
                    JButton x = buttons.get(k); x.setEnabled(false);
                    if (k == q.answer()) { x.setBackground(new Color(0x1F8A4C)); x.setEnabled(true); }
                    else if (k == pick) x.setBackground(new Color(0xB3261E));
                }
                feedback.setForeground(right ? new Color(0x7CF0A6) : new Color(0xFF9A8F));
                feedback.setText((right ? "✔ 정답! " : "✘ 아쉬워요. ") + q.explanation());
                if (situationOf(r.id()) == null) setMission(right);
                box.revalidate();
            });
            buttons.add(b); choices.add(b);
        }
        JPanel bottom = column(); Theme.add(bottom, choices); gap(bottom, 8); Theme.add(bottom, feedback);
        box.add(bottom, BorderLayout.CENTER);
        return box;
    }

    /** 규칙 정보 칩: 몇 STEP에서 배우는지, 연습 문제 수, 타격 게임에 나오는지 */
    private JComponent facts(RuleConcept r) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); row.setOpaque(false);
        int step = stepOf(r.id());
        long count = service.catalog().questions().stream().filter(q -> q.ruleId().equals(r.id())).count();
        if (step > 0) row.add(fact("STEP " + step + (preview ? "에서 배워요" : "에서 배웠어요")));
        if (count > 0) row.add(fact("연습 문제 " + count + "개"));
        if (situationOf(r.id()) != null) row.add(fact("타격 게임에 나와요"));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        return row;
    }
    private JLabel fact(String text) {
        JLabel l = label(text, 13, INK, true); l.setOpaque(true);
        l.setBackground(new Color(0xEEF1F5)); l.setBorder(new EmptyBorder(6, 12, 6, 12));
        return l;
    }
    /** 이 규칙이 처음 나오는 STEP 번호 (없으면 0) */
    private int stepOf(String ruleId) {
        for (LearningStep step : service.steps())
            for (String qid : step.questionIds())
                if (service.catalog().question(qid).map(q -> q.ruleId().equals(ruleId)).orElse(false)) return step.number();
        return 0;
    }

    /** 오늘의 5장 목록: 누르면 그 장으로 바로 간다 */
    private JComponent playlist() {
        JPanel list = column(); list.setPreferredSize(new Dimension(290, 10));
        Theme.add(list, label("오늘의 " + rules.size() + "장", 15, INK, true)); gap(list, 10);
        for (int i = 0; i < rules.size(); i++) {
            int at = i; RuleConcept r = rules.get(i); boolean now = i == index;
            JButton b = new JButton("<html><b>" + (i + 1) + "</b>&nbsp;&nbsp;" + r.title()
                    + (previewIds.contains(r.id()) ? "&nbsp;&nbsp;<span style='color:#B07A00'>맛보기</span>" : "") + "</html>");
            b.setFont(Theme.font(14, now)); b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setFocusPainted(false); b.setOpaque(true); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.setBackground(now ? new Color(0x111823) : new Color(0xF3F5F8)); b.setForeground(now ? WHITE : INK);
            b.setBorder(new EmptyBorder(11, 14, 11, 14));
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46)); b.setAlignmentX(0f);
            b.addActionListener(e -> { index = at; pausedUntil = System.currentTimeMillis() + 15_000; render(); });
            list.add(b); gap(list, 6);
        }
        return list;
    }

    // ---------- 아래: 오늘의 미션 ----------
    private JComponent missionBar() {
        Theme.RoundedPanel bar = new Theme.RoundedPanel(new Color(0xF3F6F4), 14);
        bar.setLayout(new BorderLayout(10, 0)); bar.setBorder(new EmptyBorder(9, 14, 9, 10));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); left.setOpaque(false);
        left.add(HomeParts.iconBox("star", 18, accent)); left.add(mission);
        bar.add(left, BorderLayout.CENTER);
        missionState.setOpaque(true); missionState.setBorder(new EmptyBorder(5, 12, 5, 12));
        bar.add(missionState, BorderLayout.EAST);
        return bar;
    }
    private void setMission(boolean done) {
        missionDone = done;
        missionState.setText(done ? "미션 성공!" : "진행 중");
        missionState.setBackground(done ? new Color(0x1F8A4C) : new Color(0x9AA6A2));
    }
    private void checkMission() {
        if (missionDone || rules.isEmpty()) return;
        String situation = situationOf(rules.get(index).id());
        if (situation != null && game.metRules().contains(situation)) SwingUtilities.invokeLater(() -> setMission(true));
    }

    // ---------- 시계: 날짜 + 다음 규칙까지 남은 시간 ----------
    private void tick() {
        LocalDateTime now = LocalDateTime.now();
        Duration left = Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay());
        String day = "월화수목금토일".charAt(now.getDayOfWeek().getValue() - 1) + "";
        clock.setText(now.getMonthValue() + "월 " + now.getDayOfMonth() + "일 (" + day + ")  ·  다음 규칙까지 "
                + String.format("%02d:%02d:%02d", left.toHours(), left.toMinutesPart(), left.toSecondsPart()));
    }

    // ---------- 도우미 ----------
    private JComponent arrow(boolean next) {
        JButton b = HomeParts.circle(next, true, () -> { index = (index + (next ? 1 : rules.size() - 1)) % rules.size(); pausedUntil = System.currentTimeMillis() + 15_000; render(); });
        b.setPreferredSize(new Dimension(48, 48));
        JPanel wrap = new JPanel(new GridBagLayout()); wrap.setOpaque(false); wrap.add(b);
        return wrap;
    }
    private static JLabel tag(String text, Color bg, Color fg) {
        JLabel l = label(text, 12, fg, true); l.setOpaque(true); l.setBackground(bg); l.setBorder(new EmptyBorder(3, 9, 3, 9));
        return l;
    }
    /** 규칙 그림: 그 규칙을 다루는 그림 문제가 있으면 그 그림, 없으면 야구장 사진. */
    private Path picture(RuleConcept r) {
        for (Question q : service.catalog().questions())
            if (q.ruleId().equals(r.id()) && q.hasImage()) { try { return service.catalog().imagePath(q); } catch (Exception e) { break; } }
        return Path.of("data", "images", "stadium_night.jpg");
    }
    /** 지금 마우스가 카드 위에 있는지 (읽는 중이면 넘기지 않기) */
    private boolean mouseOnCard() {
        try {
            if (card == null || !card.isShowing()) return false;
            Point p = MouseInfo.getPointerInfo().getLocation();
            SwingUtilities.convertPointFromScreen(p, card);
            return card.contains(p);
        } catch (Exception e) { return false; }      // 화면이 없는 환경(테스트)에서는 그냥 넘김
    }
    /** 아직 안 배운 규칙을 STEP 순서대로 (맛보기용) */
    private List<RuleConcept> upcomingRules() {
        List<RuleConcept> out = new ArrayList<>();
        for (LearningStep step : service.steps())
            for (String qid : step.questionIds())
                service.catalog().question(qid).ifPresent(q -> service.catalog().rules().stream()
                        .filter(r -> r.id().equals(q.ruleId()) && !service.discovered(r.id()) && !out.contains(r))
                        .findFirst().ifPresent(out::add));
        return out;
    }
    /** 규칙 id → 타격 게임에서 나오는 상황 이름 (없으면 null) */
    private static String situationOf(String ruleId) {
        for (String s : baseup.game.GameState.SITUATIONS) if (ruleId.equals(baseup.game.GameState.ruleIdOf(s))) return s;
        return null;
    }
}
