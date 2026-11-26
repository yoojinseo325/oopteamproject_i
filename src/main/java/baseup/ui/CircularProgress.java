package baseup.ui;

import javax.swing.*;
import javax.accessibility.AccessibleContext;
import java.awt.*;
import static baseup.ui.Theme.*;

/** 완료 단계 수를 0~100% 원형 진행률로 표시하는 Swing 컴포넌트. */
public final class CircularProgress extends JComponent {
    private final int completed;
    private final int total;

    public CircularProgress(int completed, int total) {
        this.completed = Math.max(0, Math.min(completed, Math.max(total, 0)));
        this.total = Math.max(0, total);
        setOpaque(false);
        setPreferredSize(new Dimension(105, 105));
        setMinimumSize(new Dimension(96, 96));
        getAccessibleContext().setAccessibleName("학습 진행률 " + percentage() + "퍼센트, "
                + this.completed + " / " + this.total + " 단계");
    }

    @Override public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) accessibleContext = new AccessibleJComponent() {};
        return accessibleContext;
    }

    public int percentage() {
        return total == 0 ? 0 : (int) Math.round(100.0 * completed / total);
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int d = Math.min(getWidth(), getHeight()) - 16;
            int x = (getWidth() - d) / 2;
            int y = (getHeight() - d) / 2;
            g.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(0xDCF5E4));
            g.drawArc(x + 5, y + 5, d - 10, d - 10, 0, 360);
            g.setPaint(new GradientPaint(0, 0, new Color(0x4ADE80), 0, getHeight(), new Color(0x16A34A)));
            g.drawArc(x + 5, y + 5, d - 10, d - 10, 90, -(int) Math.round(360.0 * (total == 0 ? 0 : (double)completed / total)));
            // 가운데: 완료한 단계 수(크게) + "/ 전체 단계"(작게)
            String big = String.valueOf(completed), small = "/ " + total + " 단계";
            g.setColor(INK);
            g.setFont(font(26, true));
            FontMetrics metrics = g.getFontMetrics();
            int cy = getHeight() / 2;
            g.drawString(big, (getWidth() - metrics.stringWidth(big)) / 2, cy + 4);
            g.setFont(font(11, true)); g.setColor(MUTED);
            FontMetrics sm = g.getFontMetrics();
            g.drawString(small, (getWidth() - sm.stringWidth(small)) / 2, cy + 4 + sm.getHeight());
        } finally { g.dispose(); }
    }
}
