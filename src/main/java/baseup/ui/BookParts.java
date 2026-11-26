package baseup.ui;

import baseup.model.RuleCategory;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 규칙 학습(규칙 사전)·퀴즈 화면이 함께 쓰는 부품. 발표 PPT 시안의 밝은 카드 디자인을 따른다.
 * 분류 색·아이콘, 알약 칩, 그라데이션 카드, 진행 링, 줄 수를 제한한 글, 안내 문구가 있는 입력칸, 줄바꿈 배치.
 */
final class BookParts {
    private BookParts() {}

    // PPT 시안 색
    static final Color GREEN_OK = new Color(0x1FA45A), GREEN_SOFT = new Color(0xE6F6EC), YELLOW = new Color(0xFFC83D),
            ORANGE_TEXT = new Color(0xD9822B), BLUE = new Color(0x3B6FE0), BLUE_SOFT = new Color(0xEAF0FD),
            CHIP_GRAY = new Color(0xEEF1F5), BORDER = new Color(0xE3E8EF), LIGHT_TEXT = new Color(0x8A94A6);

    /** 분류마다 고정된 색. 카드 아이콘·칩 점·상세 카드 배경에 쓴다. */
    static Color color(RuleCategory c) {
        return switch (c) {
            case BASIC -> new Color(0x3B82F6);
            case BATTING -> new Color(0xE5484D);
            case RUNNING -> new Color(0xF07F1E);
            case PITCHING -> new Color(0x14A8C4);
            case UMPIRE -> new Color(0x22A659);
            case RECORD -> new Color(0x8B5CF6);
        };
    }
    static Color tint(Color c, double amount) { return Ballpark.mix(Color.WHITE, c, amount); }

    /** 분류 아이콘(흰 선). 기본=공, 타격=방망이, 주루=베이스, 투수=투구 궤적, 심판=방패, 기록=막대그래프. */
    static void glyph(Graphics2D g0, RuleCategory c, double x, double y, double s, Color ink) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(ink); g.setStroke(new BasicStroke((float) Math.max(1.8, s / 12), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (c) {
            case BASIC -> { g.draw(new Ellipse2D.Double(x + s * .08, y + s * .08, s * .84, s * .84));
                g.draw(new Arc2D.Double(x - s * .12, y + s * .2, s * .55, s * .6, -55, 110, Arc2D.OPEN));
                g.draw(new Arc2D.Double(x + s * .57, y + s * .2, s * .55, s * .6, 125, 110, Arc2D.OPEN)); }
            case BATTING -> { g.setStroke(new BasicStroke((float) (s / 7), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Double(x + s * .22, y + s * .8, x + s * .8, y + s * .22)); }
            case RUNNING -> { Path2D d = new Path2D.Double(); d.moveTo(x + s / 2, y + s * .1); d.lineTo(x + s * .9, y + s / 2);
                d.lineTo(x + s / 2, y + s * .9); d.lineTo(x + s * .1, y + s / 2); d.closePath(); g.draw(d); }
            case PITCHING -> { g.draw(new Arc2D.Double(x + s * .12, y + s * .2, s * .76, s * .9, 20, 140, Arc2D.OPEN));
                g.fill(new Ellipse2D.Double(x + s * .72, y + s * .52, s * .16, s * .16)); }
            case UMPIRE -> { Path2D p = new Path2D.Double(); p.moveTo(x + s * .5, y + s * .08); p.lineTo(x + s * .86, y + s * .22);
                p.lineTo(x + s * .82, y + s * .55); p.curveTo(x + s * .78, y + s * .75, x + s * .62, y + s * .86, x + s * .5, y + s * .92);
                p.curveTo(x + s * .38, y + s * .86, x + s * .22, y + s * .75, x + s * .18, y + s * .55); p.lineTo(x + s * .14, y + s * .22); p.closePath(); g.draw(p); }
            case RECORD -> { for (int i = 0; i < 3; i++) { double h = s * (.3 + i * .2);
                    g.draw(new RoundRectangle2D.Double(x + s * (.14 + i * .26), y + s * .86 - h, s * .16, h, s * .1, s * .1)); } }
        }
        g.dispose();
    }

    /** 색 사각형 위에 분류 아이콘. */
    static JComponent iconTile(RuleCategory c, int size, Color fill, Color ink) {
        JComponent tile = new JComponent() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = Ballpark.smooth(g0); int s = Math.min(getWidth(), getHeight());
                g.setColor(fill); g.fill(new RoundRectangle2D.Double(0, 0, s, s, s * .3, s * .3));
                glyph(g, c, s * .24, s * .24, s * .52, ink); g.dispose();
            }
        };
        Dimension d = new Dimension(size, size); tile.setPreferredSize(d); tile.setMinimumSize(d); tile.setMaximumSize(d);
        return tile;
    }

    /** 알약 모양 칩. dot이 있으면 앞에 작은 점을 찍는다. */
    static JLabel chip(String text, Color bg, Color fg, Color dot, Color border) {
        JLabel l = new JLabel(text) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = Ballpark.smooth(g0); int w = getWidth(), h = getHeight();
                if (bg != null) { g.setColor(bg); g.fill(new RoundRectangle2D.Double(0, 0, w - 1, h - 1, h, h)); }
                if (border != null) { g.setColor(border); g.draw(new RoundRectangle2D.Double(.5, .5, w - 2, h - 2, h, h)); }
                if (dot != null) { g.setColor(dot); g.fill(new Ellipse2D.Double(12, h / 2.0 - 4, 8, 8)); }
                g.dispose(); super.paintComponent(g0);
            }
        };
        l.setFont(Theme.font(13, true)); l.setForeground(fg);
        l.setBorder(new EmptyBorder(6, dot == null ? 12 : 26, 6, 12));
        return l;
    }

    /** 위에서 아래(또는 대각선)로 색이 바뀌는 둥근 카드. 테두리와 옅은 그림자가 있다. */
    static class GradientCard extends JPanel {
        private Color from, to, border; private final int radius; private boolean shadow = true;
        GradientCard(Color from, Color to, Color border, int radius) {
            this.from = from; this.to = to; this.border = border; this.radius = radius; setOpaque(false);
            setBorder(new EmptyBorder(22, 24, 22, 24));
        }
        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
        void colors(Color from, Color to, Color border) { this.from = from; this.to = to; this.border = border; repaint(); }
        GradientCard noShadow() { shadow = false; return this; }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = Ballpark.smooth(g0); int w = getWidth(), h = getHeight();
            if (shadow) { g.setColor(new Color(15, 30, 60, 14)); g.fill(new RoundRectangle2D.Double(1, 4, w - 2, h - 4, radius, radius)); }
            g.setPaint(new GradientPaint(0, 0, from, w * .6f, h, to));
            g.fill(new RoundRectangle2D.Double(0, 0, w - 1, h - (shadow ? 4 : 1), radius, radius));
            if (border != null) { g.setColor(border); g.draw(new RoundRectangle2D.Double(.5, .5, w - 2, h - (shadow ? 5 : 2), radius, radius)); }
            decorate(g, w, h); g.dispose();
        }
        /** 하위 클래스가 배경 장식을 그릴 자리. */
        void decorate(Graphics2D g, int w, int h) { }
    }

    /** 두꺼운 초록 링 + 가운데 큰 숫자 + 작은 설명(예: "/ 40 확인"). */
    static final class Ring extends JComponent {
        private final int value, total; private final String unit; private final Color color;
        @Override public javax.accessibility.AccessibleContext getAccessibleContext() {
            if (accessibleContext == null) accessibleContext = new AccessibleJComponent() {};
            return accessibleContext;
        }
        Ring(int value, int total, String unit, Color color, int size) {
            this.value = value; this.total = Math.max(0, total); this.unit = unit; this.color = color;
            setPreferredSize(new Dimension(size, size)); setMinimumSize(getPreferredSize()); setOpaque(false);
            getAccessibleContext().setAccessibleName(value + " / " + total + " " + unit);
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = Ballpark.smooth(g0); int s = Math.min(getWidth(), getHeight()); float stroke = s * .1f;
            double x = (getWidth() - s) / 2.0 + stroke, y = (getHeight() - s) / 2.0 + stroke, d = s - stroke * 2;
            g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(tint(color, .18)); g.draw(new Ellipse2D.Double(x, y, d, d));
            double ratio = total == 0 ? 0 : Math.min(1, value / (double) total);
            g.setPaint(new GradientPaint(0, 0, Ballpark.mix(color, Color.WHITE, .25), 0, s, color));
            if (ratio > 0) g.draw(new Arc2D.Double(x, y, d, d, 90, -360 * ratio, Arc2D.OPEN));
            g.setColor(Color.WHITE); g.fill(new Ellipse2D.Double(x + stroke / 2, y + stroke / 2, d - stroke, d - stroke));
            String big = String.valueOf(value), small = "/ " + total + "  " + unit;
            g.setColor(new Color(0x111827)); g.setFont(Theme.font(Math.max(18, s / 4), true));
            FontMetrics fm = g.getFontMetrics(); int cy = getHeight() / 2;
            g.drawString(big, (getWidth() - fm.stringWidth(big)) / 2, cy + fm.getAscent() / 3);
            g.setColor(new Color(0x5B6678)); g.setFont(Theme.font(Math.max(11, s / 13), true));
            FontMetrics sm = g.getFontMetrics();
            g.drawString(small, (getWidth() - sm.stringWidth(small)) / 2, cy + fm.getAscent() / 3 + sm.getHeight() + 2);
            g.dispose();
        }
    }

    /** 정해진 줄 수까지만 그리고 넘치면 …으로 줄이는 글. 카드 높이를 고르게 맞출 때 쓴다. */
    static final class ClampText extends JComponent {
        private final String text; private final int lines; private final Color color;
        @Override public javax.accessibility.AccessibleContext getAccessibleContext() {
            if (accessibleContext == null) accessibleContext = new AccessibleJComponent() {};
            return accessibleContext;
        }
        ClampText(String text, int size, Color color, int lines) {
            this.text = text == null ? "" : text; this.lines = lines; this.color = color; setFont(Theme.font(size, false)); setOpaque(false);
            FontMetrics fm = getFontMetrics(getFont());
            setPreferredSize(new Dimension(200, fm.getHeight() * lines + 2)); setMinimumSize(new Dimension(40, fm.getHeight() * lines + 2));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, fm.getHeight() * lines + 2));
            getAccessibleContext().setAccessibleName(this.text);
        }
        String text() { return text; }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = Ballpark.smooth(g0); g.setFont(getFont()); g.setColor(color);
            FontMetrics fm = g.getFontMetrics(); int y = fm.getAscent();
            for (String line : wrap(text, fm, getWidth(), lines)) { g.drawString(line, 0, y); y += fm.getHeight(); }
            g.dispose();
        }
        /** 띄어쓰기 단위로 줄을 나누고(한 단어가 너무 길면 글자 단위), 마지막 줄이 넘치면 말줄임표를 붙인다. */
        static List<String> wrap(String text, FontMetrics fm, int width, int maxLines) {
            List<String> out = new ArrayList<>(); if (width <= 0) return out;
            StringBuilder line = new StringBuilder(); boolean cut = false;
            List<String> words = new ArrayList<>(List.of(text.strip().split("\s+")));
            for (int i = 0; i < words.size(); i++) {
                String word = words.get(i);
                String tryLine = line.length() == 0 ? word : line + " " + word;
                if (fm.stringWidth(tryLine) <= width) { line.setLength(0); line.append(tryLine); continue; }
                if (line.length() > 0) {   // 이 단어는 다음 줄로
                    out.add(line.toString()); line.setLength(0);
                    if (out.size() == maxLines) { cut = true; break; }
                    i--; continue;
                }
                int fit = 1;                // 한 단어가 한 줄보다 길면 글자 단위로 자른다
                while (fit < word.length() && fm.stringWidth(word.substring(0, fit + 1)) <= width) fit++;
                out.add(word.substring(0, fit)); words.set(i, word.substring(fit)); i--;
                if (out.size() == maxLines) { cut = true; break; }
            }
            if (!cut && line.length() > 0) { if (out.size() < maxLines) out.add(line.toString()); else cut = true; }
            if (cut && !out.isEmpty()) {
                String last = out.get(out.size() - 1);
                while (!last.isEmpty() && fm.stringWidth(last + "…") > width) last = last.substring(0, last.length() - 1);
                out.set(out.size() - 1, last.stripTrailing() + "…");
            }
            return out;
        }
    }

    /** 비어 있을 때 회색 안내 문구와 돋보기를 보여 주는 검색칸. */
    static JTextField searchField(String hint) {
        JTextField f = new JTextField() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = Ballpark.smooth(g0); int w = getWidth(), h = getHeight();
                g.setColor(new Color(0xF4F6FA)); g.fill(new RoundRectangle2D.Double(0, 0, w - 1, h - 1, 16, 16));
                g.setColor(hasFocus() ? new Color(0xB8C6DE) : BORDER); g.draw(new RoundRectangle2D.Double(.5, .5, w - 2, h - 2, 16, 16));
                g.setColor(new Color(0x5B6678)); g.setStroke(new BasicStroke(2f));
                g.draw(new Ellipse2D.Double(18, h / 2.0 - 9, 14, 14)); g.draw(new Line2D.Double(30, h / 2.0 + 3, 36, h / 2.0 + 9));
                g.dispose(); super.paintComponent(g0);
                if (getText().isEmpty()) {
                    Graphics2D t = Ballpark.smooth(g0); t.setFont(getFont()); t.setColor(LIGHT_TEXT);
                    FontMetrics fm = t.getFontMetrics(); t.drawString(hint, getInsets().left, (h - fm.getHeight()) / 2 + fm.getAscent()); t.dispose();
                }
            }
        };
        f.setOpaque(false); f.setFont(Theme.font(15, false)); f.setBorder(new EmptyBorder(14, 50, 14, 16));
        f.getAccessibleContext().setAccessibleName(hint); f.setToolTipText(hint);
        return f;
    }

    /** 왼쪽에 파란 세로 막대가 있는 소제목(PPT의 "| 자세히 알아보기"). */
    static JPanel sectionTitle(String title, String note) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = Ballpark.smooth(g0); g.setColor(BLUE);
                g.fill(new RoundRectangle2D.Double(0, 4, 4, getHeight() - 8, 4, 4)); g.dispose();
            }
        };
        p.setOpaque(false); p.setBorder(new EmptyBorder(0, 14, 0, 0));
        p.add(Theme.label(title, 19, new Color(0x111827), true));
        if (note != null) { JLabel n = Theme.label("   " + note, 13, new Color(0x5B6678), false); p.add(n); }
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height));
        return p;
    }

    /** 카드 전체를 버튼처럼: 클릭하면 실행하고, 위에 올리면 손가락 커서. */
    static void clickable(JComponent c, Runnable run) {
        c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        c.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { run.run(); } });
    }

    /** 색을 바꾼 알약 버튼. primary면 색을 꽉 채우고, 아니면 흰 바탕에 색 테두리. */
    static Theme.ActionButton pillButton(String text, Color color, boolean primary, Runnable run) {
        Theme.ActionButton b = Theme.button(text, primary, run); b.teamColor(color); b.setFont(Theme.font(14, true));
        b.setBorder(new EmptyBorder(12, 20, 12, 20));
        return b;
    }

    /** FlowLayout처럼 왼쪽부터 놓되, 폭이 모자라면 다음 줄로 넘기고 그만큼 높이를 늘린다. */
    static final class WrapLayout extends FlowLayout {
        WrapLayout(int hgap, int vgap) { super(FlowLayout.LEFT, hgap, vgap); }
        @Override public Dimension preferredLayoutSize(Container target) { return size(target, true); }
        @Override public Dimension minimumLayoutSize(Container target) { Dimension d = size(target, false); d.width -= getHgap() + 1; return d; }
        private Dimension size(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int width = target.getSize().width; Container p = target;
                while (width == 0 && p.getParent() != null) { p = p.getParent(); width = p.getSize().width; }
                if (width == 0) width = Integer.MAX_VALUE;
                Insets in = target.getInsets(); int max = width - in.left - in.right - getHgap() * 2;
                Dimension dim = new Dimension(0, 0); int rowW = 0, rowH = 0;
                for (Component c : target.getComponents()) {
                    if (!c.isVisible()) continue;
                    Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
                    if (rowW + d.width > max && rowW > 0) { dim.width = Math.max(dim.width, rowW); dim.height += rowH + getVgap(); rowW = 0; rowH = 0; }
                    if (rowW != 0) rowW += getHgap();
                    rowW += d.width; rowH = Math.max(rowH, d.height);
                }
                dim.width = Math.max(dim.width, rowW); dim.height += rowH;
                dim.width += in.left + in.right + getHgap() * 2; dim.height += in.top + in.bottom + getVgap() * 2;
                return dim;
            }
        }
    }
    /** 줄바꿈 글 상자. width 폭에서 필요한 높이를 미리 계산해 두어, 처음 배치할 때도 한 줄로 잘리지 않는다. */
    static JTextArea wrapText(String value, int size, Color color, boolean bold, int width) {
        JTextArea a = new JTextArea(value) {
            private int laidOutWidth = width;
            @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
            // 실제 폭이 정해지거나 바뀌면 필요한 줄 수가 달라지므로 한 번 더 배치한다(작은 창에서 글 끝이 잘리지 않게).
            @Override public void setBounds(int x, int y, int w, int h) {
                super.setBounds(x, y, w, h);
                if (w > 0 && w != laidOutWidth) { laidOutWidth = w; SwingUtilities.invokeLater(this::revalidate); }
            }
        };
        a.setEditable(false); a.setFocusable(false); a.setLineWrap(true); a.setWrapStyleWord(true); a.setOpaque(false);
        a.setFont(Theme.font(size, bold)); a.setForeground(color); a.setBorder(null);
        a.setSize(new Dimension(width, Short.MAX_VALUE));   // 처음 배치 전에는 이 폭으로 줄 수를 어림한다(좁게 잡아 잘리지 않게)
        return a;
    }

    /** 화면이 다 배치된 뒤 target이 보이도록 스크롤한다(시즌 코스로 바로 내려갈 때). */
    static void scrollToTop(JComponent target) {
        SwingUtilities.invokeLater(() -> SwingUtilities.invokeLater(() -> {
            Window w = SwingUtilities.getWindowAncestor(target); if (w != null) w.validate();
            JViewport vp = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, target);
            if (vp == null || vp.getView() == null) return;
            Point at = SwingUtilities.convertPoint(target, 0, 0, vp.getView());
            int max = Math.max(0, vp.getView().getHeight() - vp.getExtentSize().height);
            vp.setViewPosition(new Point(0, Math.max(0, Math.min(at.y - 12, max))));
        }));
    }

    /** 세로로 쌓는 패널에서 늘어나지 않는 줄 패널(최대 높이 = 원하는 높이). */
    static JPanel fixedHeight(LayoutManager layout) {
        JPanel p = new JPanel(layout) {
            @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
        };
        p.setOpaque(false); return p;
    }

    static JPanel wrapRow(int hgap, int vgap) {
        JPanel p = new JPanel(new WrapLayout(hgap, vgap)) {
            @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
        };
        p.setOpaque(false); return p;
    }
}
