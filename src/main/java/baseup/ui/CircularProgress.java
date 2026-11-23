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
            g.setColor(LINE);
            g.drawArc(x + 5, y + 5, d - 10, d - 10, 0, 360);
            g.setColor(GREEN);
            g.drawArc(x + 5, y + 5, d - 10, d - 10, 90, -(int) Math.round(360.0 * (total == 0 ? 0 : (double)completed / total)));
            String txt = percentage() + "%";
            g.setColor(INK);
            g.setFont(font(25, true));
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(txt, (getWidth() - metrics.stringWidth(txt)) / 2,
                    (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
        } finally { g.dispose(); }
    }
}
