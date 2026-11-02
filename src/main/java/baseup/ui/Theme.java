package baseup.ui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/** 외부 UI 라이브러리 없이 공통 색, 글꼴, 컴포넌트를 정의한다. */
public final class Theme {
    public static final Color BG = new Color(0xF4F6F3), WHITE = Color.WHITE, INK = new Color(0x162D2A),
            MUTED = new Color(0x697975), GREEN = new Color(0x176B50), PALE = new Color(0xE8F3EC),
            LINE = new Color(0xDCE5DE), NAVY = new Color(0x132E2B), ORANGE = new Color(0xE8A44F),
            RED = new Color(0xB44E3C);
    private Theme() { }
    public static Font font(int size, boolean bold) { return new Font("Malgun Gothic", bold ? Font.BOLD : Font.PLAIN, size); }
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
        JLabel label = new JLabel(text); label.setFont(font(size, bold)); label.setForeground(color); return label;
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
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean fill = primary || selected;
            Color accent = teamColor == null ? GREEN : teamColor;
            Color bg = !isEnabled() ? LINE : fill ? accent : WHITE;
            if (getModel().isRollover() && isEnabled()) bg = fill ? accent : PALE;
            g.setColor(bg); g.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
            g.setColor(hasFocus() ? ORANGE : fill || teamColor != null ? accent : LINE);
            g.setStroke(new BasicStroke(hasFocus() ? 3 : 1));
            g.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 12, 12); g.dispose();
            setForeground(!isEnabled() ? MUTED : fill ? contrast(bg) : INK);
            super.paintComponent(graphics);
        }
    }
}
