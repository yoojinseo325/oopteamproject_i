package baseup.ui;

import baseup.model.*;
import baseup.service.RuleEncyclopedia;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Line2D;
import java.util.*;
import java.util.List;
import static baseup.ui.Theme.*;
import static baseup.ui.BookParts.*;

/**
 * 규칙 학습 = 규칙 사전(교수님 피드백 5·6). 문제를 풀지 않아도 모든 규칙을 카드로 찾아 읽는다.
 * 위: 소개 카드와 "퀴즈로 확인한 규칙" 링. 가운데: 검색칸과 분류 칩. 아래: 규칙 카드 3열. 카드를 누르면 상세(RuleDetailScreen)로 간다.
 */
final class RuleBookScreen extends Screen {
    private final RuleEncyclopedia book;
    private final Set<String> confirmed;
    private final JPanel grid = new JPanel(new GridLayout(0, 3, 18, 18));
    private final JLabel count = label("", 15, INK, true);
    private final JPanel chips = wrapRow(8, 0);
    private JTextField search;

    RuleBookScreen(MainFrame app) {
        super(app);
        book = new RuleEncyclopedia(service.catalog());
        confirmed = confirmedRules(service);
    }

    /** 퀴즈에서 한 번이라도 이해(판정·단서 정답)한 규칙. */
    static Set<String> confirmedRules(baseup.service.LearningService service) {
        Set<String> ids = new HashSet<>();
        for (Attempt a : service.attempts())
            if (a.understood()) service.catalog().question(a.questionId()).ifPresent(q -> ids.add(q.ruleId()));
        return ids;
    }

    @Override JComponent build() {
        JPanel p = column(); p.setBorder(new EmptyBorder(24, 30, 30, 30));
        Theme.add(p, hero()); gap(p, 22);
        search = searchField("규칙 이름이나 내용으로 찾아보세요  (예: 보크, 인필드 플라이, 포스)");
        search.setName("rule-search"); search.setText(app.bookQuery);
        search.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        listen(search, () -> { app.bookQuery = search.getText(); refresh(); });
        Theme.add(p, search); gap(p, 16);
        Theme.add(p, chips); gap(p, 18);
        Theme.add(p, count); gap(p, 14);
        grid.setOpaque(false); Theme.add(p, grid);
        refresh();
        JScrollPane page = scroll(p); page.getViewport().setBackground(WHITE);   // PPT처럼 흰 바탕
        return page;
    }

    private JComponent hero() {
        GradientCard card = new GradientCard(new Color(0xF2F6FF), new Color(0xFFF2D6), new Color(0xE7E3D6), 26) {
            @Override void decorate(Graphics2D g, int w, int h) {   // 옅은 내야 다이아몬드 선
                g.setColor(new Color(0xC9B98F)); g.setStroke(new BasicStroke(1f));
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .35f));
                g.draw(new Line2D.Double(w * .48, h, w * .70, h * .1)); g.draw(new Line2D.Double(w * .70, h * .1, w * .92, h));
            }
        };
        card.setLayout(new BorderLayout(24, 0)); card.setBorder(new EmptyBorder(28, 32, 30, 32));
        JPanel text = column();
        Theme.add(text, label("RULE BOOK  ·  규칙 사전", 13, ORANGE_TEXT, true)); gap(text, 10);
        Theme.add(text, label("읽고, 이해하고, 판정까지.", 31, INK, true)); gap(text, 10);
        Theme.add(text, label("궁금한 규칙을 사전처럼 찾아 읽어 보세요. 이해했다면 퀴즈에서 심판이 되어 직접 판정해요.", 14, MUTED, false)); gap(text, 16);
        long saved = service.catalog().rules().stream().filter(r -> service.bookmarked(r.id())).count();
        JPanel stats = row(8);
        stats.add(chip("규칙 " + book.size() + "개", WHITE, INK, null, BORDER));
        stats.add(chip("★ 저장 " + saved, WHITE, ORANGE_TEXT, null, new Color(0xF3DDB5)));
        stats.add(chip("퀴즈로 확인 " + confirmed.size(), WHITE, GREEN_OK, GREEN_OK, new Color(0xBFE6CC)));
        Theme.add(text, stats); gap(text, 18);
        JPanel actions = row(12);
        Theme.ActionButton quiz = pillButton("퀴즈 풀러 가기   →", YELLOW, true, () -> app.showPage("quiz"));
        quiz.setName("book-go-quiz"); actions.add(quiz);
        Theme.ActionButton pick = pillButton("오늘 읽을 규칙 뽑기", new Color(0x5B6678), false, this::pickRandom);
        pick.setName("book-random"); actions.add(pick);
        Theme.add(text, actions);
        card.add(text, BorderLayout.CENTER);
        JPanel ring = new JPanel(new GridBagLayout()); ring.setOpaque(false);
        ring.add(new Ring(confirmed.size(), book.size(), "확인", new Color(0x22C55E), 176));
        card.add(ring, BorderLayout.EAST);
        return card;
    }

    private List<RuleConcept> visible() {
        String q = app.bookQuery == null ? "" : app.bookQuery.strip();
        if (!q.isEmpty()) return book.search(q, app.bookCategory);
        return app.bookCategory == null ? service.catalog().rules() : book.rulesIn(app.bookCategory);
    }

    private void refresh() {
        // 분류 칩: 선택한 칩만 진한 남색, 나머지는 분류 색을 옅게.
        chips.removeAll();
        chips.add(categoryChip(null, "전체", book.size()));
        for (RuleCategory c : book.categories()) chips.add(categoryChip(c, c.label(), book.rulesIn(c).size()));
        List<RuleConcept> rules = visible();
        String where = app.bookCategory == null ? "" : app.bookCategory.label() + " · ";
        count.setText(rules.isEmpty() ? "찾는 규칙이 없어요. 다른 말로 검색해 보세요." : where + rules.size() + "개의 규칙");
        grid.removeAll();
        for (RuleConcept r : rules) grid.add(ruleCard(r, rules));
        chips.revalidate(); grid.revalidate(); grid.repaint(); chips.repaint();
    }

    private JComponent categoryChip(RuleCategory c, String name, int n) {
        boolean on = Objects.equals(app.bookCategory, c);
        Color tone = c == null ? new Color(0x1B2433) : color(c);
        JLabel chip = on ? chip(name + "   " + n, new Color(0x1B2433), WHITE, c == null ? null : tone, null)
                : chip(name + "   " + n, tint(tone, .08), INK, c == null ? null : tone, tint(tone, .28));
        chip.setName("category-" + (c == null ? "all" : c.code()));
        clickable(chip, () -> { app.bookCategory = c; refresh(); });
        return chip;
    }

    private JComponent ruleCard(RuleConcept r, List<RuleConcept> trail) {
        Color tone = color(r.category());
        GradientCard card = new GradientCard(WHITE, tint(tone, .06), tint(tone, .22), 18);
        card.setName("rule-card-" + r.id());
        card.setLayout(new BorderLayout(0, 12)); card.setBorder(new EmptyBorder(18, 20, 16, 20));
        JPanel head = new JPanel(new BorderLayout(14, 0)); head.setOpaque(false);
        head.add(iconTile(r.category(), 46, tone, WHITE), BorderLayout.WEST);
        JPanel names = column();
        Theme.add(names, label(r.category().label(), 12, tone, true)); gap(names, 3);
        JLabel title = label(r.title(), 19, INK, true); Theme.add(names, title);
        head.add(names, BorderLayout.CENTER);
        card.add(head, BorderLayout.NORTH);
        card.add(new ClampText(r.summary(), 14, new Color(0x3F4A5C), 2), BorderLayout.CENTER);
        JPanel foot = new JPanel(new BorderLayout()); foot.setOpaque(false);
        JPanel left = row(6);
        boolean ok = confirmed.contains(r.id());
        left.add(ok ? chip("퀴즈로 확인", GREEN_SOFT, GREEN_OK, GREEN_OK, null) : chip("아직 확인 전", CHIP_GRAY, MUTED, new Color(0xB4BCC8), null));
        long quizzes = service.questionCount(r.id());
        left.add(chip(quizzes == 0 ? "읽기 전용" : "퀴즈 " + quizzes, CHIP_GRAY, MUTED, null, null));
        foot.add(left, BorderLayout.WEST);
        foot.add(label("자세히 보기  →", 13, tone, true), BorderLayout.EAST);
        card.add(foot, BorderLayout.SOUTH);
        clickable(card, () -> open(r, trail));
        return card;
    }

    private void open(RuleConcept r, List<RuleConcept> trail) {
        String label = app.bookCategory == null ? (app.bookQuery.isBlank() ? "전체 규칙" : "검색 결과") : app.bookCategory.label();
        app.openRule(r.id(), trail.stream().map(RuleConcept::id).toList(), label);
    }

    private void pickRandom() {
        List<RuleConcept> rules = visible().isEmpty() ? service.catalog().rules() : visible();
        open(rules.get(new Random().nextInt(rules.size())), rules);
    }
}
