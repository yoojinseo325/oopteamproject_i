package baseup.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;

/** 메인페이지 부품: 아이콘, 둥근 화살표 버튼, 알약 버튼, 오늘의 규칙 슬라이드 그림, 다음 단계 카드. */
final class HomeParts {
    private HomeParts() {}

    /** 직접 그리는 단순 아이콘. kind: home, path, book, chart, note, ball, glove, diamond, star, mitt, camera, person */
    static void icon(Graphics2D g, String kind, double x, double y, double s, Color c) {
        g = (Graphics2D) g.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(c); g.setStroke(new BasicStroke((float) Math.max(1.6, s / 11), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (kind) {
            case "home" -> { Path2D p = new Path2D.Double(); p.moveTo(x, y + s * .45); p.lineTo(x + s / 2, y + s * .05); p.lineTo(x + s, y + s * .45);
                p.lineTo(x + s * .85, y + s * .45); p.lineTo(x + s * .85, y + s * .95); p.lineTo(x + s * .15, y + s * .95); p.lineTo(x + s * .15, y + s * .45); p.closePath(); g.fill(p); }
            case "path" -> { g.draw(new Line2D.Double(x + s * .2, y + s * .05, x + s * .2, y + s * .95));
                Path2D f = new Path2D.Double(); f.moveTo(x + s * .2, y + s * .08); f.lineTo(x + s * .9, y + s * .25); f.lineTo(x + s * .2, y + s * .45); f.closePath(); g.fill(f); }
            case "book" -> { g.draw(new RoundRectangle2D.Double(x + s * .1, y + s * .1, s * .8, s * .8, s * .15, s * .15));
                g.draw(new Line2D.Double(x + s * .5, y + s * .1, x + s * .5, y + s * .9)); }
            case "chart" -> { for (int i = 0; i < 3; i++) { double h = s * (.35 + i * .25); g.fill(new Rectangle2D.Double(x + s * (.1 + i * .3), y + s - h, s * .2, h)); } }
            case "note" -> { g.draw(new RoundRectangle2D.Double(x + s * .15, y + s * .05, s * .7, s * .9, s * .12, s * .12));
                for (int i = 0; i < 3; i++) g.draw(new Line2D.Double(x + s * .3, y + s * (.3 + i * .2), x + s * .7, y + s * (.3 + i * .2))); }
            case "ball" -> { g.setColor(Color.WHITE); g.fill(new Ellipse2D.Double(x, y, s, s)); g.setColor(new Color(0xB9C4BF)); g.draw(new Ellipse2D.Double(x, y, s, s));
                g.setColor(new Color(0xE0483C)); g.draw(new Arc2D.Double(x - s * .25, y + s * .1, s * .55, s * .8, -60, 120, Arc2D.OPEN));
                g.draw(new Arc2D.Double(x + s * .7, y + s * .1, s * .55, s * .8, 120, 120, Arc2D.OPEN)); }
            case "glove" -> { g.setColor(new Color(0xB8662E)); g.fill(new RoundRectangle2D.Double(x + s * .15, y + s * .25, s * .7, s * .7, s * .5, s * .5));
                for (int i = 0; i < 4; i++) g.fill(new RoundRectangle2D.Double(x + s * (.15 + i * .17), y + s * .02, s * .15, s * .5, s * .15, s * .15));
                g.setColor(new Color(0x7A3F17)); g.draw(new Arc2D.Double(x + s * .3, y + s * .45, s * .4, s * .35, 200, 140, Arc2D.OPEN)); }
            case "diamond" -> { Path2D d = new Path2D.Double(); d.moveTo(x + s / 2, y); d.lineTo(x + s, y + s / 2); d.lineTo(x + s / 2, y + s); d.lineTo(x, y + s / 2); d.closePath();
                g.setColor(new Color(0x3E8E4E)); g.fill(d); g.setColor(new Color(0xC98B55));
                g.fill(new Ellipse2D.Double(x + s * .38, y + s * .38, s * .24, s * .24)); g.setColor(Color.WHITE);
                for (double[] b : new double[][]{{.5, .06}, {.94, .5}, {.5, .94}, {.06, .5}}) g.fill(new Rectangle2D.Double(x + s * b[0] - s * .06, y + s * b[1] - s * .06, s * .12, s * .12)); }
            case "star" -> { Path2D p = new Path2D.Double();
                for (int i = 0; i < 10; i++) { double a = -Math.PI / 2 + i * Math.PI / 5, r = i % 2 == 0 ? s / 2 : s * .22;
                    double px = x + s / 2 + Math.cos(a) * r, py = y + s / 2 + Math.sin(a) * r; if (i == 0) p.moveTo(px, py); else p.lineTo(px, py); }
                p.closePath(); g.fill(p); }
            case "mitt" -> { // 사이드바용 글러브(선 아이콘)
                g.fill(new RoundRectangle2D.Double(x + s * .12, y + s * .32, s * .72, s * .62, s * .45, s * .45));
                for (int i = 0; i < 4; i++) g.fill(new RoundRectangle2D.Double(x + s * (.12 + i * .17), y + s * .02, s * .14, s * .5, s * .14, s * .14));
                g.fill(new RoundRectangle2D.Double(x + s * .78, y + s * .3, s * .2, s * .32, s * .16, s * .16)); }
            case "camera" -> { // 비디오 판독 카메라
                g.draw(new RoundRectangle2D.Double(x + s * .04, y + s * .25, s * .64, s * .52, s * .14, s * .14));
                Path2D l = new Path2D.Double(); l.moveTo(x + s * .7, y + s * .44); l.lineTo(x + s * .98, y + s * .28); l.lineTo(x + s * .98, y + s * .74); l.lineTo(x + s * .7, y + s * .58); l.closePath(); g.fill(l);
                g.fill(new Ellipse2D.Double(x + s * .14, y + s * .36, s * .14, s * .14)); }
            case "person" -> { // 마이페이지
                g.fill(new Ellipse2D.Double(x + s * .3, y + s * .02, s * .4, s * .4));
                g.fill(new Arc2D.Double(x + s * .08, y + s * .5, s * .84, s * .9, 0, 180, Arc2D.CHORD)); }
            case "rulebook" -> { // 규칙 백과사전: 펼친 책(채운 아이콘, 가운데 접힌 선만 비움)
                Path2D left = new Path2D.Double(); left.moveTo(x + s * .47, y + s * .2); left.curveTo(x + s * .35, y + s * .1, x + s * .15, y + s * .1, x + s * .02, y + s * .16);
                left.lineTo(x + s * .02, y + s * .9); left.curveTo(x + s * .15, y + s * .84, x + s * .35, y + s * .84, x + s * .47, y + s * .94); left.closePath(); g.fill(left);
                Path2D right = new Path2D.Double(); right.moveTo(x + s * .53, y + s * .2); right.curveTo(x + s * .65, y + s * .1, x + s * .85, y + s * .1, x + s * .98, y + s * .16);
                right.lineTo(x + s * .98, y + s * .9); right.curveTo(x + s * .85, y + s * .84, x + s * .65, y + s * .84, x + s * .53, y + s * .94); right.closePath(); g.fill(right); }
            default -> { }
        }
        g.dispose();
    }

    /** 아이콘 하나를 컴포넌트로. */
    static JComponent iconBox(String kind, int size, Color c) {
        JComponent box = new JComponent() {
            @Override protected void paintComponent(Graphics g) { icon((Graphics2D) g, kind, 2, 2, size - 4, c); }
        };
        Dimension d = new Dimension(size, size); box.setPreferredSize(d); box.setMinimumSize(d); box.setMaximumSize(d);
        return box;
    }

    /** 동그란 화살표 버튼 (‹ ›). */
    static JButton circle(boolean next, boolean dark, Runnable action) {
        JButton b = new JButton() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = Ballpark.smooth(g0); int s = Math.min(getWidth(), getHeight());
                g.setColor(new Color(0, 0, 0, 40)); g.fill(new Ellipse2D.Double(1, 3, s - 2, s - 2));
                g.setColor(dark ? new Color(0x111827) : Color.WHITE); g.fill(new Ellipse2D.Double(1, 1, s - 2, s - 2));
                g.setColor(dark ? Color.WHITE : new Color(0x111827)); g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                double cx = s / 2.0, cy = s / 2.0, a = s * .14;
                Path2D p = new Path2D.Double();
                if (next) { p.moveTo(cx - a / 2, cy - a); p.lineTo(cx + a / 2, cy); p.lineTo(cx - a / 2, cy + a); }
                else { p.moveTo(cx + a / 2, cy - a); p.lineTo(cx - a / 2, cy); p.lineTo(cx + a / 2, cy + a); }
                g.draw(p); g.dispose();
            }
        };
        b.setName(next ? "slide-next" : "slide-prev");
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false); b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> action.run());
        return b;
    }

    /** 색이 꽉 찬 알약 모양 버튼. */
    static JButton pill(String text, Color color, Runnable action) {
        JButton b = new JButton(text + "   ›") {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = Ballpark.smooth(g0); int w = getWidth(), h = getHeight();
                g.setColor(new Color(0, 0, 0, 45)); g.fill(new RoundRectangle2D.Double(0, 3, w, h - 3, h - 3, h - 3));
                g.setColor(getModel().isRollover() ? Ballpark.mix(color, Color.BLACK, .15) : color);
                g.fill(new RoundRectangle2D.Double(0, 0, w, h - 3, h - 3, h - 3)); g.dispose();
                super.paintComponent(g0);
            }
        };
        b.setFont(Theme.font(15, true)); b.setForeground(Color.WHITE); b.setBorder(new EmptyBorder(13, 26, 16, 26));
        b.setContentAreaFilled(false); b.setFocusPainted(false); b.setOpaque(false); b.setRolloverEnabled(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); b.addActionListener(e -> action.run());
        return b;
    }

    /** 오늘의 규칙 카드 왼쪽 그림: 모서리를 둥글게 자르고, 오른쪽 끝을 흰색으로 부드럽게 녹인다. */
    static final class SlidePicture extends JComponent {
        private BufferedImage image;
        SlidePicture(Path file) {
            try { if (file != null && Files.isReadable(file)) image = ImageIO.read(file.toFile()); } catch (Exception ignored) {}
            setPreferredSize(new Dimension(440, 260));
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = Ballpark.smooth(g0); int w = getWidth(), h = getHeight();
            g.clip(new RoundRectangle2D.Double(0, 0, w, h, 24, 24));          // 자기 칸 안에서만 그린다
            if (image != null) {
                double s = Math.max((double) w / image.getWidth(), (double) h / image.getHeight());
                int iw = (int) (image.getWidth() * s), ih = (int) (image.getHeight() * s);
                g.drawImage(image, (w - iw) / 2, (h - ih) / 2, iw, ih, null);
            } else { g.setColor(new Color(0xDCE5DE)); g.fillRect(0, 0, w, h); }
            g.setPaint(new GradientPaint(w * .82f, 0, new Color(255, 255, 255, 0), w, 0, new Color(255, 255, 255, 200)));
            g.fillRect((int) (w * .8), 0, w, h);
            g.dispose();
        }
    }

    /** 슬라이드 아래 점(●○○○) 표시. */
    static final class Dots extends JComponent {
        private int count, current; private final Color color;
        Dots(Color color) { this.color = color; setPreferredSize(new Dimension(200, 18)); }
        void set(int count, int current) { this.count = count; this.current = current; repaint(); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = Ballpark.smooth(g0); int n = Math.min(count, 12), gap = 16, x0 = (getWidth() - (n - 1) * gap) / 2;
            for (int i = 0; i < n; i++) {
                boolean on = i == current % Math.max(1, n);
                g.setColor(on ? color : new Color(255, 255, 255, 150));
                double r = on ? 10 : 8; g.fill(new Ellipse2D.Double(x0 + i * gap - r / 2, 9 - r / 2, r, r));
            }
            g.dispose();
        }
    }

    /** 흰 카드 배경 + 오른쪽 위에 희미한 야구공 무늬. */
    static final class LessonCard extends JPanel {
        LessonCard() { setOpaque(false); setBorder(new EmptyBorder(18, 20, 18, 20)); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = Ballpark.smooth(g0); int w = getWidth(), h = getHeight();
            g.setColor(new Color(0, 0, 0, 35)); g.fill(new RoundRectangle2D.Double(0, 4, w, h - 4, 22, 22));
            g.setColor(new Color(255, 255, 255, 245)); g.fill(new RoundRectangle2D.Double(0, 0, w, h - 4, 22, 22));
            g.setClip(new RoundRectangle2D.Double(0, 0, w, h - 4, 22, 22));
            double r = h * .9, cx = w - r * .25, cy = h * .2;
            g.setColor(new Color(0xEFE7E7)); g.setStroke(new BasicStroke(3f));
            g.draw(new Ellipse2D.Double(cx - r / 2, cy - r / 2, r, r));
            g.setColor(new Color(0xF2D6D6));
            g.draw(new Arc2D.Double(cx - r * .75, cy - r * .4, r * .55, r * .8, -60, 120, Arc2D.OPEN));
            g.dispose();
        }
    }

    /**
     * BASEUP 워드마크 배너: BASE(흰색) + UP(강조색), UP 위로 솟아오르는 야구공 궤적, 아래에 한 줄 소개.
     * 이름 그대로 "한 베이스씩 올라간다"는 뜻을 그림으로 보여 준다.
     */
    static final class Wordmark extends JComponent {
        private final Color accent; private final String tagline;
        Wordmark(Color accent, String tagline) { this.accent = accent; this.tagline = tagline; setPreferredSize(new Dimension(900, 190)); }
        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, 190); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = Ballpark.smooth(g0);
            int w = getWidth();
            Font big = Theme.font(104, true).deriveFont(Font.BOLD | Font.ITALIC, 104f);
            g.setFont(big); FontMetrics m = g.getFontMetrics();
            int baseW = m.stringWidth("BASE"), upW = m.stringWidth("UP"), total = baseW + upW + 6;
            int x = (w - total) / 2 - 30, y = 112;
            // 그림자
            g.setColor(new Color(0, 0, 0, 110)); g.drawString("BASE", x + 4, y + 5); g.drawString("UP", x + baseW + 10, y + 5);
            // BASE: 흰 글씨 + 아래 분필 라인
            g.setColor(Color.WHITE); g.drawString("BASE", x, y);
            // UP: 강조색 + 흰 테두리
            java.awt.font.GlyphVector gv = big.createGlyphVector(g.getFontRenderContext(), "UP");
            Shape up = gv.getOutline(x + baseW + 6, y);
            g.setColor(accent); g.fill(up);
            g.setColor(Color.WHITE); g.setStroke(new BasicStroke(3f)); g.draw(up);
            // 솟아오르는 공 궤적
            double sx = x + baseW + upW + 14, sy = y - 20;
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < 4; i++) {
                g.setColor(new Color(255, 255, 255, 60 + i * 40));
                g.draw(new QuadCurve2D.Double(sx - 70 + i * 8, sy + 40 - i * 6, sx - 10, sy + 10 - i * 4, sx + 30, sy - 50 - i * 3));
            }
            icon(g, "ball", sx + 18, sy - 92, 46, Color.WHITE);
            // 베이스 3개가 올라가는 계단
            for (int i = 0; i < 3; i++) {
                double bx = x - 70 + i * 22, by = y - 14 - i * 22;
                Path2D b = new Path2D.Double(); b.moveTo(bx, by - 9); b.lineTo(bx + 9, by); b.lineTo(bx, by + 9); b.lineTo(bx - 9, by); b.closePath();
                g.setColor(i == 2 ? Ballpark.LAMP : Color.WHITE); g.fill(b);
            }
            // 밑줄 + 소개
            g.setColor(accent); g.fill(new RoundRectangle2D.Double(x, y + 14, total, 6, 6, 6));
            g.setFont(Theme.font(18, true)); FontMetrics tm = g.getFontMetrics();
            g.setColor(new Color(0, 0, 0, 120)); g.drawString(tagline, (w - tm.stringWidth(tagline)) / 2 + 1, y + 54);
            g.setColor(Color.WHITE); g.drawString(tagline, (w - tm.stringWidth(tagline)) / 2, y + 53);
            g.dispose();
        }
    }
}
