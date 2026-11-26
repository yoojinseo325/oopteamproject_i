package baseup.ui;

import baseup.model.*;
import baseup.service.ReviewSelector;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import static baseup.ui.Theme.*;
import static baseup.ui.BookParts.*;

/**
 * 퀴즈 허브(QUIZ ARENA). 규칙 학습(읽기)과 문제 풀이를 나눈 화면이다(교수님 피드백 6).
 * 상황 퀴즈(장면 판정)·일반 퀴즈(개념) 카드, 오답 다시 풀기, 시즌 코스(STEP 지도)를 한곳에 모았다.
 * 단계는 "판정 문제가 들어 있는 STEP"을 상황 코스로, 나머지를 일반 코스로 나눠 보여 준다(번호와 열리는 순서는 그대로).
 */
final class QuizScreen extends Screen {
    static final Color SITUATION = new Color(0x1FA45A), GENERAL = BLUE;
    private final List<LearningStep> situationSteps = new ArrayList<>(), generalSteps = new ArrayList<>();
    private final List<Question> situationQuestions, generalQuestions;
    private final JPanel season = column();
    private RuleCategory quickCategory;

    QuizScreen(MainFrame app) {
        super(app);
        for (LearningStep s : service.steps()) (isSituation(s) ? situationSteps : generalSteps).add(s);
        situationQuestions = service.catalog().questions().stream().filter(Question::requiresClue).toList();
        generalQuestions = service.catalog().questions().stream().filter(q -> !q.requiresClue()).toList();
    }

    private boolean isSituation(LearningStep s) {
        return s.questionIds().stream().anyMatch(id -> service.catalog().question(id).map(Question::requiresClue).orElse(false));
    }
    private long doneCount(List<LearningStep> steps) { return steps.stream().filter(s -> service.stepCompleted(s.number())).count(); }

    @Override JComponent build() {
        JPanel p = column(); p.setBorder(new EmptyBorder(26, 30, 34, 30));
        Theme.add(p, label("QUIZ ARENA  ·  퀴즈", 13, ORANGE_TEXT, true)); gap(p, 10);
        Theme.add(p, label("심판이 되어, 직접 판정해요.", 31, INK, true)); gap(p, 10);
        Theme.add(p, label("경기 상황 속에서 판정하는 상황 퀴즈, 개념을 빠르게 점검하는 일반 퀴즈 중에 골라 보세요.", 14, MUTED, false)); gap(p, 22);
        JPanel cards = fixedHeight(new GridLayout(1, 2, 22, 0));
        cards.add(situationCard()); cards.add(generalCard());
        Theme.add(p, cards); gap(p, 20);
        Theme.add(p, reviewBanner()); gap(p, 34);
        Theme.add(p, season);
        buildSeason();
        JScrollPane scroll = scroll(p); scroll.getViewport().setBackground(WHITE);   // PPT처럼 흰 바탕
        if (app.scrollToSeason) { app.scrollToSeason = false; scrollToTop(season); }   // "학습 경로 보기"로 들어오면 시즌 코스부터
        return scroll;
    }

    // ---------- 상황 퀴즈 / 일반 퀴즈 카드 ----------
    private JComponent situationCard() {
        GradientCard card = new GradientCard(WHITE, new Color(0xEEF9F1), new Color(0xBFE5CB), 22);
        card.setLayout(new BorderLayout(0, 16)); card.setBorder(new EmptyBorder(20, 26, 24, 26));
        JPanel top = column();
        Theme.add(top, illustration(true)); gap(top, 14);
        Theme.add(top, titleRow("상황 퀴즈", chip("경기 장면 · 판정", GREEN_SOFT, SITUATION, null, null))); gap(top, 10);
        Theme.add(top, label("실제 경기 상황 속 심판이 되어 아웃·세이프·파울을 판정해요.", 14, new Color(0x3F4A5C), false)); gap(top, 4);
        Theme.add(top, label("판정 문제 " + situationQuestions.size() + "개  ·  상황 단계 " + situationSteps.size() + "개", 12, LIGHT_TEXT, false)); gap(top, 16);
        long done = doneCount(situationSteps);
        Theme.add(top, label("상황 코스   " + done + " / " + situationSteps.size() + " 단계 완료", 14, INK, true)); gap(top, 8);
        Theme.add(top, progressBar(done, situationSteps.size(), SITUATION));
        card.add(top, BorderLayout.NORTH);
        JPanel actions = row(10);
        Theme.ActionButton random = pillButton("무작위 상황 5문제   →", SITUATION, true, () -> startRandom(situationQuestions, "상황 퀴즈 · 무작위 5문제"));
        random.setName("quiz-random-situation"); actions.add(random);
        Theme.ActionButton course = pillButton("코스 보기", SITUATION, false, () -> showCourse("situation"));
        course.setName("quiz-course-situation"); actions.add(course);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private JComponent generalCard() {
        GradientCard card = new GradientCard(WHITE, new Color(0xEEF3FF), new Color(0xC9D7F6), 22);
        card.setLayout(new BorderLayout(0, 16)); card.setBorder(new EmptyBorder(20, 26, 24, 26));
        JPanel top = column();
        Theme.add(top, illustration(false)); gap(top, 14);
        Theme.add(top, titleRow("일반 퀴즈", chip("개념 · 용어 · 기록", BLUE_SOFT, GENERAL, null, null))); gap(top, 10);
        Theme.add(top, label("상황 없이 규칙 개념과 기록 읽는 법을 빠르게 점검해요.", 14, new Color(0x3F4A5C), false)); gap(top, 4);
        Theme.add(top, label("개념 문제 " + generalQuestions.size() + "개  ·  일반 단계 " + generalSteps.size() + "개  ·  분류를 골라 빠른 5문제도 가능", 12, LIGHT_TEXT, false)); gap(top, 12);
        JPanel cats = wrapRow(6, 6);
        Theme.add(top, cats);
        Runnable[] paint = new Runnable[1];
        paint[0] = () -> {
            cats.removeAll();
            cats.add(filterChip("전체", null, paint[0]));
            for (RuleCategory c : RuleCategory.values())
                if (generalQuestions.stream().anyMatch(q -> service.catalog().rule(q.ruleId()).category() == c)) cats.add(filterChip(c.label(), c, paint[0]));
            cats.revalidate(); cats.repaint();
        };
        paint[0].run();
        card.add(top, BorderLayout.NORTH);
        JPanel actions = row(10);
        Theme.ActionButton quick = pillButton("빠른 5문제   →", GENERAL, true, () -> {
            List<Question> pool = generalQuestions.stream().filter(q -> quickCategory == null || service.catalog().rule(q.ruleId()).category() == quickCategory).toList();
            startRandom(pool, "일반 퀴즈 · 빠른 5문제" + (quickCategory == null ? "" : " · " + quickCategory.label()));
        });
        quick.setName("quiz-quick-general"); actions.add(quick);
        boolean complete = doneCount(generalSteps) == generalSteps.size() && !generalSteps.isEmpty();
        Theme.ActionButton course = pillButton(complete ? "단계 코스 완주!" : "단계 코스 보기", GENERAL, false, () -> showCourse("general"));
        course.setName("quiz-course-general"); actions.add(course);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private JComponent filterChip(String name, RuleCategory c, Runnable repaint) {
        boolean on = Objects.equals(quickCategory, c);
        Color tone = c == null ? new Color(0x1B2433) : color(c);
        JLabel chip = on ? chip(name, new Color(0x1B2433), WHITE, null, null) : chip(name, tint(tone, .08), INK, null, tint(tone, .3));
        chip.setFont(font(12, true)); chip.setName("quick-" + (c == null ? "all" : c.code()));
        clickable(chip, () -> { quickCategory = c; repaint.run(); });
        return chip;
    }

    private static JPanel titleRow(String title, JComponent tag) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)); row.setOpaque(false);
        row.add(label(title, 25, INK, true)); row.add(tag);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private static JComponent progressBar(long done, int total, Color color) {
        JComponent bar = new JComponent() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = Ballpark.smooth(g0); int w = getWidth(), h = getHeight();
                g.setColor(tint(color, .14)); g.fill(new RoundRectangle2D.Double(0, 0, w, h, h, h));
                double ratio = total == 0 ? 0 : done / (double) total;
                if (ratio > 0) { g.setPaint(new GradientPaint(0, 0, Ballpark.mix(color, Color.WHITE, .3), w, 0, color));
                    g.fill(new RoundRectangle2D.Double(0, 0, Math.max(h, w * ratio), h, h, h)); }
                g.dispose();
            }
        };
        bar.setPreferredSize(new Dimension(300, 10)); bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
        return bar;
    }

    /** 카드 위 그림: 상황 퀴즈는 내야 다이아몬드와 "SAFE!" 말풍선, 일반 퀴즈는 O ? X 카드. */
    private static JComponent illustration(boolean situation) {
        JComponent art = new JComponent() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = Ballpark.smooth(g0); double w = getWidth(), h = getHeight(), cx = w / 2, cy = h / 2;
                if (situation) {
                    g.setColor(new Color(0xDDF2E3)); g.fill(new Ellipse2D.Double(cx - 110, cy - 52, 220, 104));
                    Path2D d = new Path2D.Double(); d.moveTo(cx, cy - 42); d.lineTo(cx + 70, cy); d.lineTo(cx, cy + 42); d.lineTo(cx - 70, cy); d.closePath();
                    g.setColor(SITUATION); g.setStroke(new BasicStroke(2.4f)); g.draw(d);
                    g.setColor(WHITE); for (double[] b : new double[][]{{0, -42}, {70, 0}, {-70, 0}}) { g.fill(new Rectangle2D.Double(cx + b[0] - 6, cy + b[1] - 6, 12, 12)); }
                    g.setColor(SITUATION); for (double[] b : new double[][]{{0, -42}, {70, 0}, {-70, 0}}) { g.draw(new Rectangle2D.Double(cx + b[0] - 6, cy + b[1] - 6, 12, 12)); }
                    g.fill(new Ellipse2D.Double(cx + 30, cy + 14, 13, 13));
                    g.setColor(YELLOW); g.fill(new RoundRectangle2D.Double(cx + 64, cy - 58, 74, 32, 30, 30));
                    Path2D tail = new Path2D.Double(); tail.moveTo(cx + 78, cy - 30); tail.lineTo(cx + 72, cy - 18); tail.lineTo(cx + 92, cy - 28); g.fill(tail);
                    g.setColor(new Color(0x4A3500)); g.setFont(Theme.font(14, true)); g.drawString("SAFE!", (float) (cx + 78), (float) (cy - 36));
                } else {
                    String[] marks = {"O", "?", "X"}; Color[] colors = {new Color(0x22A659), new Color(0xF0A020), new Color(0xE5484D)};
                    double[] angle = {-.12, 0, .14};
                    for (int i = 0; i < 3; i++) {
                        Graphics2D c = (Graphics2D) g.create(); c.translate(cx + (i - 1) * 82, cy); c.rotate(angle[i]);
                        c.setColor(new Color(0, 0, 0, 22)); c.fill(new RoundRectangle2D.Double(-30, -38, 64, 80, 14, 14));
                        c.setColor(WHITE); c.fill(new RoundRectangle2D.Double(-32, -42, 64, 80, 14, 14));
                        c.setColor(colors[i]); c.setStroke(new BasicStroke(2.4f)); c.draw(new RoundRectangle2D.Double(-32, -42, 64, 80, 14, 14));
                        c.setFont(Theme.font(30, true)); FontMetrics fm = c.getFontMetrics(); c.drawString(marks[i], -fm.stringWidth(marks[i]) / 2, 12); c.dispose();
                    }
                    g.setColor(new Color(0xA9B8E8)); for (double[] s : new double[][]{{-150, -40}, {140, -30}, {160, 20}, {-120, 30}}) g.fill(new Ellipse2D.Double(cx + s[0], cy + s[1], 5, 5));
                }
                g.dispose();
            }
        };
        art.setPreferredSize(new Dimension(320, 120)); art.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        return art;
    }

    // ---------- 오답 다시 풀기 ----------
    private JComponent reviewBanner() {
        int waiting = service.reviewQuestions().size(), now = Math.min(waiting, 4);
        GradientCard card = new GradientCard(WHITE, new Color(0xFFF1E4), new Color(0xF4D9C0), 20);
        card.setLayout(new BorderLayout(18, 0)); card.setBorder(new EmptyBorder(18, 22, 20, 24));
        JComponent icon = new JComponent() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = Ballpark.smooth(g0); int s = Math.min(getWidth(), getHeight());
                g.setColor(new Color(0xFFE2C7)); g.fill(new RoundRectangle2D.Double(0, 0, s, s, 16, 16));
                g.setColor(new Color(0xF07F1E)); g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Arc2D.Double(s * .25, s * .25, s * .5, s * .5, 60, 290, Arc2D.OPEN));
                Path2D head = new Path2D.Double(); head.moveTo(s * .63, s * .17); head.lineTo(s * .72, s * .33); head.lineTo(s * .55, s * .34); g.draw(head); g.dispose();
            }
        };
        icon.setPreferredSize(new Dimension(52, 52));
        JPanel iconBox = new JPanel(new GridBagLayout()); iconBox.setOpaque(false); iconBox.add(icon);
        card.add(iconBox, BorderLayout.WEST);
        JPanel text = column();
        Theme.add(text, label("오답 다시 풀기", 20, INK, true)); gap(text, 6);
        Theme.add(text, label(waiting == 0 ? "지금 다시 풀 문제가 없어요. 깔끔한 판정이에요!"
                : "헷갈렸던 문제 " + waiting + "개 중 " + now + "개를 다시 풀어요. 반복하면 기억에 오래 남아요.", 14, MUTED, false));
        card.add(text, BorderLayout.CENTER);
        Theme.ActionButton go = pillButton("다시 풀기   →", new Color(0xF07F1E), true, () -> {
            try { app.showQuiz(service.start(new ReviewSelector()), "오답 다시 풀기"); } catch (Exception e) { app.error(e); }
        });
        go.setName("quiz-review"); go.setEnabled(waiting > 0);
        JPanel goBox = new JPanel(new GridBagLayout()); goBox.setOpaque(false); goBox.add(go);
        card.add(goBox, BorderLayout.EAST);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
        return card;
    }

    // ---------- 시즌 코스 ----------
    private void buildSeason() {
        season.removeAll();
        boolean situation = !"general".equals(app.seasonTab);
        List<LearningStep> steps = situation ? situationSteps : generalSteps;
        JPanel head = new JPanel(new BorderLayout()); head.setOpaque(false);
        JPanel words = column();
        Theme.add(words, label("시즌 코스", 25, INK, true)); gap(words, 8);
        Theme.add(words, label("길을 따라 한 단계씩! 한 줄(단계 4개)을 끝내면 그 이닝 득점. 빛나는 베이스가 지금 도전할 단계예요.", 14, MUTED, false));
        head.add(words, BorderLayout.CENTER);
        Theme.ActionButton next = pillButton(app.nextAction(), YELLOW, true, app::startNext);
        next.setName("quiz-next-step");
        JPanel nextBox = new JPanel(new GridBagLayout()); nextBox.setOpaque(false); nextBox.add(next);
        head.add(nextBox, BorderLayout.EAST);
        head.setMaximumSize(new Dimension(Integer.MAX_VALUE, head.getPreferredSize().height));
        Theme.add(season, head); gap(season, 14);
        JPanel tabs = row(10);
        tabs.add(tab("상황 퀴즈 코스   " + doneCount(situationSteps) + " / " + situationSteps.size(), SITUATION, situation, "situation"));
        tabs.add(tab("일반 퀴즈 코스   " + doneCount(generalSteps) + " / " + generalSteps.size(), GENERAL, !situation, "general"));
        Theme.add(season, tabs); gap(season, 16);
        Color tone = situation ? SITUATION : GENERAL;
        GradientCard map = new GradientCard(tint(tone, .07), tint(tone, .12), tint(tone, .25), 22);
        map.setName("season-map"); map.setLayout(new BorderLayout(0, 6)); map.setBorder(new EmptyBorder(22, 26, 18, 26));
        JPanel title = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); title.setOpaque(false);
        title.add(label(situation ? "상황 퀴즈 시즌 지도" : "일반 퀴즈 시즌 지도", 20, Ballpark.mix(tone, Color.BLACK, .35), true));
        title.add(label("     열린 베이스를 눌러 바로 도전하세요", 13, MUTED, false));
        map.add(title, BorderLayout.NORTH);
        map.add(new SeasonMap(steps, service, tone, app::startStep), BorderLayout.CENTER);
        Theme.add(season, map);
        season.revalidate(); season.repaint();
    }

    private JComponent tab(String text, Color color, boolean on, String key) {
        Theme.ActionButton b = pillButton(text, color, on, () -> { app.seasonTab = key; buildSeason(); });
        b.setName("season-tab-" + key);
        return b;
    }

    private void showCourse(String key) {
        app.seasonTab = key; buildSeason(); scrollToTop(season);
    }

    private void startRandom(List<Question> pool, String title) {
        if (pool.isEmpty()) { app.message("고를 문제가 없어요."); return; }
        List<Question> pick = new ArrayList<>(pool); Collections.shuffle(pick);
        List<Question> five = List.copyOf(pick.subList(0, Math.min(5, pick.size())));
        try { app.showQuiz(service.start(x -> five), title); } catch (Exception e) { app.error(e); }
    }
}
