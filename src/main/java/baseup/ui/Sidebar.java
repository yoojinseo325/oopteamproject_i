package baseup.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.*;

/** 야구장 밤하늘 느낌의 왼쪽 메뉴 부품들: 배경, BASEUP 엠블럼, 베이스 모양 메뉴 버튼. */
final class Sidebar {
    private Sidebar() {}

    static final Color DEEP = new Color(0x0D1826);      // 밤 경기장 남색
    static final Color DEEP2 = new Color(0x13253A);
    static final Color STITCH = new Color(0xE0483C);    // 야구공 실밥 빨강
    static final Color CHALK = new Color(0xF4F1E8);     // 분필 라인 흰색
    static final Color SOFT = new Color(0xF1F3F6);

    /** 팀 색으로 꽉 채운 배경: 위는 선명한 팀 색, 아래로 갈수록 진해지고, 비스듬한 색 조각과 핀스트라이프가 들어간다. */
    static void paintTeam(Graphics2D g, int w, int h, Color main, Color accent) {
        Color top = main, bottom = Ballpark.mix(main, Color.BLACK, .62);
        g.setPaint(new GradientPaint(0, 0, top, w * .4f, h, bottom)); g.fillRect(0, 0, w, h);
        Path2D a = new Path2D.Double(); a.moveTo(w, h * .30); a.lineTo(w, h * .62); a.lineTo(0, h * .92); a.lineTo(0, h * .74); a.closePath();
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 150)); g.fill(a);
        Path2D b = new Path2D.Double(); b.moveTo(w, h * .64); b.lineTo(w, h * .70); b.lineTo(0, h * .98); b.lineTo(0, h * .94); b.closePath();
        g.setColor(new Color(255, 255, 255, 70)); g.fill(b);
        Path2D c = new Path2D.Double(); c.moveTo(w, 0); c.lineTo(w, h * .16); c.lineTo(w * .35, 0); c.closePath();
        g.setColor(new Color(255, 255, 255, 28)); g.fill(c);
        g.setColor(new Color(255, 255, 255, 13)); for (int x = 10; x < w; x += 14) g.fillRect(x, 0, 1, h); // 유니폼 핀스트라이프
    }
    static final class Background extends JPanel {
        private final Color team, accent;
        Background(Color team, Color accent) { super(new BorderLayout()); this.team = team; this.accent = accent; setOpaque(true); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            if (team != null) paintTeam(g, w, h, team, accent);
            else { g.setPaint(new GradientPaint(0, 0, DEEP2, w * .4f, h, DEEP)); g.fillRect(0, 0, w, h); }
            int s = (int) (w * 1.1), cx = w / 2, cy = h - s / 3; // 내야 다이아몬드 워터마크
            Path2D d = new Path2D.Double();
            d.moveTo(cx, cy - s / 2.0); d.lineTo(cx + s / 2.0, cy); d.lineTo(cx, cy + s / 2.0); d.lineTo(cx - s / 2.0, cy); d.closePath();
            g.setColor(new Color(255, 255, 255, 30)); g.setStroke(new BasicStroke(2.5f)); g.draw(d);
            g.dispose();
        }
    }

    /** 직접 그린 BASEUP 엠블럼: 실밥이 있는 야구공 + 글자. (실제 구단 로고는 상표라 쓰지 않음) */
    static final class Emblem extends JComponent {
        Emblem() { setPreferredSize(new Dimension(170, 58)); setMaximumSize(new Dimension(Integer.MAX_VALUE, 58)); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int r = 46, y = 6;
            g.setColor(CHALK); g.fillOval(0, y, r, r);
            g.setColor(STITCH); g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Arc2D.Double(-r * 0.42, y + 4, r * 0.7, r - 8, -55, 110, Arc2D.OPEN));
            g.draw(new Arc2D.Double(r * 0.72, y + 4, r * 0.7, r - 8, 125, 110, Arc2D.OPEN));
            g.setStroke(new BasicStroke(1.4f));
            for (int i = -2; i <= 2; i++) { // 실밥 바늘땀
                int yy = y + r / 2 + i * 7;
                g.drawLine(r / 2 - 13, yy - 2, r / 2 - 8, yy + 1); g.drawLine(r / 2 + 8, yy + 1, r / 2 + 13, yy - 2);
            }
            g.setColor(Color.WHITE); g.setFont(Theme.font(25, true)); g.drawString("BASEUP", r + 12, y + 26);
            g.setColor(new Color(255,255,255,210)); g.setFont(Theme.font(10, true)); g.drawString("RULE  ·  UMPIRE  ·  PLAY", r + 13, y + 43);
            g.dispose();
        }
    }

    /** 메뉴 버튼: 왼쪽에 베이스(◆) 모양, 선택되면 노란 베이스 + 밝은 배경 + 굵은 흰 글씨. */
    static final class NavButton extends JButton {
        private boolean active; private String icon = "";
        NavButton(String title, String icon) { this(title); this.icon = icon; }
        NavButton(String title) {
            super(title); setFont(Theme.font(15, true)); setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(new EmptyBorder(13, 42, 13, 8)); setContentAreaFilled(false); setFocusPainted(false); setOpaque(false);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 48)); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setRolloverEnabled(true);
        }
        void active(boolean value) { active = value; repaint(); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int h = getHeight();
            if (active || getModel().isRollover()) {
                g.setColor(new Color(255, 255, 255, active ? 60 : 28)); g.fillRoundRect(0, 0, getWidth(), h, 12, 12);
            }
            if (active) { g.setColor(Color.WHITE); g.fillRoundRect(0, 8, 4, h - 16, 4, 4); }
            if (!icon.isEmpty()) HomeParts.icon(g, icon, 14, h / 2.0 - 9, 18, active ? Color.WHITE : new Color(255, 255, 255, 190));
            else {
                int cx = 22, cy = h / 2, b = 6; // 베이스 모양 마름모
                Polygon base = new Polygon(new int[]{cx, cx + b, cx, cx - b}, new int[]{cy - b, cy, cy + b, cy}, 4);
                if (active) { g.setColor(Color.WHITE); g.fill(base); }
                else { g.setColor(new Color(255,255,255,150)); g.setStroke(new BasicStroke(1.5f)); g.draw(base); }
            }
            g.dispose();
            setForeground(active ? Color.WHITE : SOFT);
            super.paintComponent(graphics);
        }
    }
}
