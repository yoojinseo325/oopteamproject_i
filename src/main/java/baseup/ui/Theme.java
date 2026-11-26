package baseup.ui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/** 외부 UI 라이브러리 없이 공통 색, 글꼴, 컴포넌트를 정의한다. */
public final class Theme {
    // 메인 홈(야구장 전광판) 팔레트로 통일: 남색 전광판 + 빨간 행동 버튼 + 노란 램프.
    public static final Color BG = new Color(0xF3F5F8), WHITE = Color.WHITE, INK = new Color(0x111827),
            MUTED = new Color(0x5B6678), PALE = new Color(0xE8EEF6),
            LINE = new Color(0xDDE3EB), NAVY = new Color(0x0D1B2A), ORANGE = new Color(0xFFC94A),
            RED = new Color(0xB3261E);
    /** 강조 글자·배지·선택 표시 색(남색). 예전 이름 GREEN을 많은 화면이 쓰므로 이름은 유지한다. */
    public static final Color ACCENT = new Color(0x1E3A5F), GREEN = ACCENT;
    /** 주요 행동 버튼 색. 메인 홈의 "규칙 카드 보기" 버튼과 같은 빨강. */
    public static final Color ACTION = new Color(0xD7263D);
    /** 정답·완료 표시 색. 강조색(남색)과 오답(빨강)과 구분된다. */
    public static final Color SUCCESS = new Color(0x1F8A4C);
    private Theme() { }
    /** data/fonts 폴더에 KBO 다이아고딕(.ttf/.otf)이 있으면 그 글꼴을, 없으면 맑은 고딕을 쓴다. */
    private static final Font KBO_BOLD = loadFont("bold"), KBO_TEXT = loadFont("light") != null ? loadFont("light") : loadFont("medium");
    private static Font loadFont(String weight) {
        java.io.File[] files = new java.io.File("data/fonts").listFiles();
        if (files == null) return null;
        for (java.io.File f : files) {
            String name = f.getName().toLowerCase(java.util.Locale.ROOT);
            if (name.contains(weight) && (name.endsWith(".ttf") || name.endsWith(".otf"))) {
                try { return Font.createFont(Font.TRUETYPE_FONT, f); } catch (Exception ignored) { }
            }
        }
        return null;
    }
    public static Font font(int size, boolean bold) {
        Font kbo = bold ? KBO_BOLD : KBO_TEXT;
        if (kbo != null) return kbo.deriveFont((float) size);
        return new Font("Malgun Gothic", bold ? Font.BOLD : Font.PLAIN, size);
    }
    public static Color contrast(Color background) {
        double[] channels = {background.getRed()/255.0, background.getGreen()/255.0, background.getBlue()/255.0};
        for (int i=0;i<3;i++) channels[i]=channels[i]<=0.04045?channels[i]/12.92:Math.pow((channels[i]+0.055)/1.055,2.4);
        return 0.2126*channels[0]+0.7152*channels[1]+0.0722*channels[2]>0.179?Color.BLACK:Color.WHITE;
    }
    public static void install() {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignored) { }
        for (Object key : UIManager.getDefaults().keySet().toArray())
            if (key.toString().endsWith(".font")) UIManager.put(key, new javax.swing.plaf.FontUIResource(font(14, false)));
        UIManager.put("Panel.background", BG);
        UIManager.put("OptionPane.background", BG);
        UIManager.put("Button.focus", GREEN);
        UIManager.put("TextField.caretForeground", INK);
    }
    public static JLabel label(String text, int size, Color color, boolean bold) {
        JLabel label = new JLabel(text); label.setFont(font(size, bold)); label.setForeground(color);
        label.setBorder(new EmptyBorder(0, 0, 0, 3)); // 일부 글꼴에서 마지막 글자가 잘리지 않게 여유
        return label;
    }
    public static JTextArea text(String value, int size, Color color) {
        JTextArea text = new JTextArea(value) {
            @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
        }; text.setEditable(false); text.setFocusable(false);
        text.setLineWrap(true); text.setWrapStyleWord(true); text.setOpaque(false);
        text.setFont(font(size, false)); text.setForeground(color); text.setBorder(null); return text;
    }
    public static JPanel column() {
        JPanel p = new JPanel(); p.setOpaque(false); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); return p;
    }
    public static JPanel row(int gap) { JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, gap, 0)); p.setOpaque(false); return p; }
    public static void add(JPanel parent, Component child) {
        if (child instanceof JComponent jc) jc.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(child);
    }
    public static void gap(JPanel panel, int height) { panel.add(Box.createVerticalStrut(height)); }
    public static RoundedPanel card() { return new RoundedPanel(WHITE, 20); }
    public static JScrollPane scroll(Component content) {
        Component view = content instanceof JPanel panel ? new WidthPanel(panel) : content;
        JScrollPane scroll = new JScrollPane(view); scroll.setBorder(null); scroll.getViewport().setBackground(BG);
        scroll.getVerticalScrollBar().setUnitIncrement(22); scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        SwingUtilities.invokeLater(() -> scroll.getViewport().setViewPosition(new Point(0, 0)));
        return scroll;
    }
    /** 세로 스크롤 시에도 본문 너비를 창에 맞춰 가로 내용이 잘리지 않게 한다. */
    private static final class WidthPanel extends JPanel implements Scrollable {
        WidthPanel(JPanel content) { super(new BorderLayout()); setOpaque(false); add(content); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) { return 22; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) { return Math.max(22, r.height - 40); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
    public static ActionButton button(String title, boolean primary, Runnable action) {
        ActionButton button = new ActionButton(title, primary);
        button.addActionListener(e -> action.run()); return button;
    }
    public static JTextField input(String placeholder) {
        JTextField field = new JTextField(); field.setFont(font(14, false));
        field.setBorder(new CompoundBorder(new LineBorder(LINE, 1, true), new EmptyBorder(11, 13, 11, 13)));
        field.setToolTipText(placeholder); field.getAccessibleContext().setAccessibleName(placeholder); return field;
    }
    public static String html(String s) { return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>"); }
    public static class RoundedPanel extends JPanel {
        private final int radius;
        public RoundedPanel(Color background, int radius) {
            this.radius = radius; setBackground(background); setOpaque(false); setBorder(new EmptyBorder(22, 22, 22, 22));
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(getBackground()); g.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g.dispose(); super.paintComponent(graphics);
        }
    }
    public static final class ActionButton extends JButton {
        private final boolean primary;
        private boolean selected;
        private Color teamColor;
        public void teamColor(Color value) { teamColor=value; repaint(); }
        public ActionButton(String text, boolean primary) {
            super(text); this.primary = primary;
            setFont(font(14, true)); setBorder(new EmptyBorder(12, 17, 12, 17));
            setContentAreaFilled(false); setFocusPainted(false); setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { repaint(); }
                @Override public void mouseExited(MouseEvent e) { repaint(); }
            });
        }
        public void mark(boolean value) { selected = value; repaint(); }
        /** 메인 홈 버튼과 같은 알약 모양. 주요 버튼은 빨강(팀 색이 있으면 팀 색), 보조 버튼은 흰 바탕 + 테두리. */
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean fill = primary || selected;
            Color accent = teamColor != null ? teamColor : primary ? ACTION : ACCENT;
            Color bg = !isEnabled() ? LINE : fill ? accent : WHITE;
            boolean hover = getModel().isRollover() && isEnabled();
            if (hover) bg = fill ? mix(accent, Color.BLACK, .15) : PALE;
            int w = getWidth(), h = getHeight(), arc = Math.min(h, 40);
            if (fill && isEnabled()) { g.setColor(new Color(0, 0, 0, 40)); g.fillRoundRect(0, 3, w - 1, h - 4, arc, arc); }
            g.setColor(bg); g.fillRoundRect(0, 0, w - 1, h - (fill && isEnabled() ? 3 : 1), arc, arc);
            if (!fill || hasFocus()) {
                g.setColor(hasFocus() ? ORANGE : teamColor != null ? accent : LINE);
                g.setStroke(new BasicStroke(hasFocus() ? 3 : 1));
                g.drawRoundRect(1, 1, w - 3, h - (fill ? 5 : 3), arc, arc);
            }
            g.dispose();
            setForeground(!isEnabled() ? MUTED : fill ? contrast(bg) : INK);
            super.paintComponent(graphics);
        }
        private static Color mix(Color a, Color b, double t) {
            return new Color((int) (a.getRed() * (1 - t) + b.getRed() * t), (int) (a.getGreen() * (1 - t) + b.getGreen() * t),
                    (int) (a.getBlue() * (1 - t) + b.getBlue() * t));
        }
    }
}
