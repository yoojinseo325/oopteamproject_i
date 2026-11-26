package baseup.ui;

import baseup.model.*;
import baseup.service.RuleEncyclopedia;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.util.*;
import java.util.List;
import static baseup.ui.Theme.*;
import static baseup.ui.BookParts.*;

/**
 * 규칙 한 장을 책처럼 읽는 화면(교수님 피드백 6). 이전·다음 규칙 버튼과 ←/→ 키로 넘긴다.
 * 왼쪽: 분류 색 카드(한 줄 핵심, 퀴즈 풀기, 저장). 오른쪽: 자세한 설명, 근거, 연결된 퀴즈, 같이 보면 좋은 규칙.
 */
final class RuleDetailScreen extends Screen {
    private final RuleConcept rule;
    private final List<String> trail;
    private final int index;

    RuleDetailScreen(MainFrame app) {
        super(app);
        rule = service.catalog().rule(app.pendingRule);
        List<String> t = app.ruleTrail == null ? List.of() : app.ruleTrail;
        if (!t.contains(rule.id())) { t = service.catalog().rules().stream().map(RuleConcept::id).toList(); app.ruleTrailLabel = "전체 규칙"; }
        trail = t; index = trail.indexOf(rule.id());
    }

    @Override JComponent build() {
        JPanel p = column(); p.setBorder(new EmptyBorder(22, 30, 30, 30));
        Theme.add(p, topBar()); gap(p, 18);
        JPanel body = new JPanel(new GridBagLayout()); body.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints(); c.gridy = 0; c.fill = GridBagConstraints.BOTH; c.anchor = GridBagConstraints.NORTH;
        c.gridx = 0; c.weightx = .6; c.weighty = 1; body.add(heroCard(), c);
        c.gridx = 1; c.weightx = .4; c.insets = new Insets(0, 30, 0, 0); body.add(sideColumn(), c);
        Theme.add(p, body);
        // ←/→ 키로 쪽 넘기기(검색칸처럼 글자를 입력하는 곳이 없는 화면이라 바로 동작한다)
        bind(p, KeyEvent.VK_LEFT, "rule-prev", () -> go(-1));
        bind(p, KeyEvent.VK_RIGHT, "rule-next", () -> go(1));
        JScrollPane page = scroll(p); page.getViewport().setBackground(WHITE);   // PPT처럼 흰 바탕
        return page;
    }

    private JComponent topBar() {
        JPanel bar = new JPanel(new BorderLayout()); bar.setOpaque(false);
        JPanel left = row(14);
        Theme.ActionButton back = pillButton("←  규칙 사전", new Color(0x5B6678), false, () -> app.showPage("library"));
        back.setName("rule-back"); left.add(back);
        left.add(label((index + 1) + " / " + trail.size() + "   ·   " + app.ruleTrailLabel, 14, MUTED, true));
        bar.add(left, BorderLayout.WEST);
        JPanel right = row(10);
        Theme.ActionButton prev = pillButton("‹  이전 규칙", new Color(0x5B6678), false, () -> go(-1));
        prev.setName("page-prev"); prev.setEnabled(index > 0); right.add(prev);
        Theme.ActionButton next = pillButton("다음 규칙  ›", BLUE, true, () -> go(1));
        next.setName("page-next"); next.setEnabled(index < trail.size() - 1); right.add(next);
        bar.add(right, BorderLayout.EAST);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, bar.getPreferredSize().height));
        return bar;
    }

    private void go(int step) {
        int to = index + step;
        if (to < 0 || to >= trail.size()) return;
        app.openRule(trail.get(to), trail, app.ruleTrailLabel);
    }

    private JComponent heroCard() {
        Color tone = color(rule.category());
        GradientCard card = new GradientCard(Ballpark.mix(tone, WHITE, .14), Ballpark.mix(tone, Color.BLACK, .14), null, 26) {
            @Override void decorate(Graphics2D g, int w, int h) {   // 큰 원과 공 실밥 무늬
                g.setColor(new Color(255, 255, 255, 26)); g.fill(new Ellipse2D.Double(-w * .25, -h * .2, w * .85, w * .85));
                g.setColor(new Color(255, 255, 255, 34)); g.setStroke(new BasicStroke(Math.max(18f, w / 22f)));
                double s = w * .62; g.draw(new Ellipse2D.Double(w * .55, h * .55, s, s));
                g.draw(new Arc2D.Double(w * .55 + s * .1, h * .55 + s * .12, s * .35, s * .8, 90, 180, Arc2D.OPEN));
            }
        };
        card.noShadow();
        card.setName("rule-hero"); card.setLayout(new BorderLayout(0, 20)); card.setBorder(new EmptyBorder(34, 36, 32, 36));
        card.setPreferredSize(new Dimension(560, 560)); card.setMinimumSize(new Dimension(420, 520));
        JPanel top = column();
        Theme.add(top, iconTile(rule.category(), 66, new Color(255, 255, 255, 58), WHITE)); gap(top, 22);
        Theme.add(top, label(rule.category().label() + "   ·   " + rule.topic(), 15, new Color(255, 255, 255, 215), true)); gap(top, 8);
        JTextArea title = wrapText(rule.title(), 38, WHITE, true, 360); Theme.add(top, title); gap(top, 22);
        RoundedPanel core = new RoundedPanel(new Color(255, 255, 255, 40), 20); core.setLayout(new BorderLayout(0, 8)); core.setBorder(new EmptyBorder(16, 20, 18, 20));
        core.add(label("한 줄 핵심", 13, new Color(255, 255, 255, 210), true), BorderLayout.NORTH);
        JTextArea summary = wrapText(rule.summary(), 19, WHITE, true, 330); core.add(summary, BorderLayout.CENTER);
        Theme.add(top, core);
        card.add(top, BorderLayout.NORTH);
        JPanel actions = row(10);
        List<Question> quizzes = questions();
        Theme.ActionButton quiz = pillButton("이 규칙 퀴즈 풀기   →", WHITE, true, this::startQuiz);
        quiz.setName("rule-quiz"); quiz.setEnabled(!quizzes.isEmpty()); if (quizzes.isEmpty()) quiz.setToolTipText("아직 연결된 퀴즈가 없는 규칙이에요.");
        actions.add(quiz);
        boolean saved = service.bookmarked(rule.id());
        Theme.ActionButton save = pillButton(saved ? "★  저장됨" : "☆  저장하기", WHITE, false, this::toggleSave);
        save.setName("rule-save"); actions.add(save);
        Theme.ActionButton memo = pillButton("메모하기", WHITE, false, () -> app.editNote(rule));
        memo.setName("rule-memo"); actions.add(memo);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private JComponent sideColumn() {
        // 글 상자가 한 줄 길이만큼 넓어지려는 것을 막고, 남는 폭은 GridBag 비율(6:4)로 나눈다.
        JPanel side = new JPanel() {
            @Override public Dimension getPreferredSize() { return new Dimension(360, super.getPreferredSize().height); }
            @Override public Dimension getMinimumSize() { return new Dimension(300, super.getMinimumSize().height); }
        };
        side.setOpaque(false); side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        String extra = rule.extraDetail();
        if (!extra.isEmpty()) {   // 요약과 같은 설명은 다시 보여 주지 않는다
            Theme.add(side, sectionTitle("자세히 알아보기", null)); gap(side, 10);
            JTextArea detail = wrapText(extra, 16, INK, false, 300); detail.setName("rule-detail"); Theme.add(side, detail); gap(side, 24);
        }
        Theme.add(side, sectionTitle("규칙 근거", null)); gap(side, 10);
        Theme.add(side, grayBox(wrapText(rule.source(), 14, MUTED, false, 260))); gap(side, 24);
        List<Question> quizzes = questions();
        Theme.add(side, sectionTitle("이 규칙으로 풀 수 있는 퀴즈  " + quizzes.size(), null)); gap(side, 10);
        JPanel list = column();
        if (quizzes.isEmpty()) Theme.add(list, wrapText("아직 연결된 퀴즈가 없어요. 읽어 두고 경기에서 만나면 떠올려 보세요.", 13, MUTED, false, 260));
        for (Question q : quizzes) { Theme.add(list, quizRow(q)); gap(list, 8); }
        Theme.add(side, grayBox(list)); gap(side, 24);
        List<RuleConcept> related = new RuleEncyclopedia(service.catalog()).related(rule.id(), 9);
        if (!related.isEmpty()) {
            Theme.add(side, sectionTitle("같이 보면 좋은 규칙", null)); gap(side, 12);
            JPanel pills = wrapRow(8, 8);
            for (RuleConcept r : related) {
                Theme.ActionButton b = pillButton(r.title(), BLUE, false, () -> app.openRule(r.id(), trail, app.ruleTrailLabel));
                b.setFont(font(13, true)); b.setBorder(new EmptyBorder(9, 16, 9, 16)); b.setName("related-" + r.id()); pills.add(b);
            }
            Theme.add(side, pills); gap(side, 16);
        }
        if (!rule.keywords().isEmpty()) Theme.add(side, wrapText("함께 쓰는 말:  " + String.join(", ", rule.keywords()), 13, MUTED, false, 300));
        return side;
    }

    private JComponent quizRow(Question q) {
        JPanel row = new JPanel(new BorderLayout(10, 0)); row.setOpaque(false);
        JPanel left = row(6);
        left.add(q.requiresClue() ? chip("상황 퀴즈", GREEN_SOFT, GREEN_OK, null, null) : chip("일반 퀴즈", BLUE_SOFT, BLUE, null, null));
        left.add(chip(q.level(), CHIP_GRAY, MUTED, null, null));
        row.add(left, BorderLayout.WEST);
        row.add(label(q.title(), 14, INK, true), BorderLayout.CENTER);   // 좁으면 제목만 …으로 줄어든다
        Optional<Attempt> latest = service.latest(q.id());
        JLabel state = latest.isEmpty() ? label("아직 안 풂", 13, LIGHT_TEXT, true)
                : latest.get().understood() ? label("●  맞힘", 13, GREEN_OK, true) : label("●  다시 보기", 13, RED, true);
        row.add(state, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private static JComponent grayBox(JComponent content) {
        RoundedPanel box = new RoundedPanel(new Color(0xF3F5F8), 16) {
            @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
        };
        box.setLayout(new BorderLayout()); box.setBorder(new EmptyBorder(14, 18, 14, 18)); box.add(content);
        return box;
    }

    private List<Question> questions() {
        return service.catalog().questions().stream().filter(q -> q.ruleId().equals(rule.id())).toList();
    }

    private void startQuiz() {
        List<Question> qs = questions(); if (qs.isEmpty()) return;
        try { app.showQuiz(service.start(x -> qs), "규칙 퀴즈 · " + rule.title()); } catch (Exception e) { app.error(e); }
    }

    private void toggleSave() {
        try { service.toggleBookmark(rule.id()); app.message(service.bookmarked(rule.id()) ? "규칙을 저장했어요." : "저장을 해제했어요."); app.showPage("rule"); }
        catch (Exception e) { app.error(e); }
    }

    private static void bind(JComponent c, int key, String name, Runnable run) {
        c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key, 0), name);
        c.getActionMap().put(name, new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { run.run(); } });
    }
}
