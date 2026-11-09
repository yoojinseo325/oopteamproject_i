package baseup.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

/** 화면에서 공통으로 사용하는 색상과 Swing 컴포넌트 스타일. */
public final class Theme {
    private Theme() {}
    public static final Color BG = new Color(14, 23, 37);
    public static final Color SURFACE = new Color(24, 37, 55);
    public static final Color SURFACE_2 = new Color(37, 53, 73);
    public static final Color BORDER = new Color(56, 75, 96);
    public static final Color WHITE = new Color(239, 246, 251);
    public static final Color MUTED = new Color(179, 196, 211);
    public static final Color ORANGE = new Color(252, 168, 70);
    public static final Color GREEN = new Color(77, 211, 168);
    public static final Color RED = new Color(254, 127, 127);
    public static final String FONT = "Dialog";

    public static void install() {
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("Panel.background", BG);
        UIManager.put("OptionPane.messageForeground", WHITE);
        UIManager.put("OptionPane.messageFont", font(Font.PLAIN, 14));
        UIManager.put("Button.font", font(Font.BOLD, 13));
    }
    public static Font font(int style, int size) { return new Font(FONT, style, size); }
    public static JPanel panel() {
        JPanel p = new JPanel();
        p.setOpaque(true);
        p.setBackground(BG);
        return p;
    }
    public static JPanel card() {
        JPanel p = new JPanel();
        p.setOpaque(true);
        p.setBackground(SURFACE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(20, 22, 20, 22)));
        return p;
    }
    public static JLabel label(String value, int size, Color color, boolean bold) {
        JLabel l = new JLabel(value);
        l.setFont(font(bold ? Font.BOLD : Font.PLAIN, size));
        l.setForeground(color);
        return l;
    }
    public static JTextArea wrap(String value, int size, Color color) {
        JTextArea a = new JTextArea(value);
        a.setFont(font(Font.PLAIN, size));
        a.setForeground(color);
        a.setOpaque(false);
        a.setEditable(false);
        a.setFocusable(false);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setBorder(null);
        return a;
    }
    public static JButton button(String value, boolean primary) {
        JButton b = new JButton(value);
        b.setFont(font(Font.BOLD, 14));
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(12, 18, 12, 18));
        b.setBackground(primary ? ORANGE : SURFACE_2);
        b.setForeground(primary ? BG : WHITE);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
    public static JScrollPane scroll(JComponent c) {
        JScrollPane sc = new JScrollPane(c);
        sc.setBorder(null);
        sc.getViewport().setBackground(BG);
        sc.getVerticalScrollBar().setUnitIncrement(18);
        return sc;
    }
    public static JPanel padded(JComponent component, int top, int left, int bottom, int right) {
        JPanel p = panel();
        p.setLayout(new BorderLayout());
        p.setBorder(new EmptyBorder(top, left, bottom, right));
        p.add(component, BorderLayout.CENTER);
        return p;
    }
    public static JPanel spacer(int height) {
        JPanel p = panel();
        p.setPreferredSize(new Dimension(5, height));
        p.setOpaque(false);
        return p;
    }
    public static void styleTextField(JComponent c) {
        c.setBackground(SURFACE_2);
        c.setForeground(WHITE);
        c.setFont(font(Font.PLAIN, 14));
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(8, 10, 8, 10)));
    }
}
