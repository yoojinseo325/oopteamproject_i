package baseup.ui;

import baseup.model.Scene;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Rectangle2D;
import java.util.function.Consumer;
import java.awt.geom.Point2D;
/**
 * 800x500 가상 캔버스에 야구 장면을 그린다.
 * 장면 표시와 클릭 입력만 담당하며 정답 여부나 채점 규칙은 알지 못한다.
 */
public final class FieldPanel extends JPanel {
    private static final int FIELD_WIDTH = 800;
    private static final int FIELD_HEIGHT = 500;

    private static final Color BACKGROUND = new Color(0xE9F0E8);
    private static final Color GRASS_LIGHT = new Color(0xB4D1AA);
    private static final Color GRASS_DARK = new Color(0xABCAA1);
    private static final Color DIRT = new Color(0xD6B992);
    private static final Color INFIELD = new Color(0x93B98A);
    private static final Color HIGHLIGHT = new Color(0xF1BA54);

    private Scene scene;
    private String selected = "";
    private Consumer<String> onSelect;

    public FieldPanel(Scene scene) {
        this.scene = scene;
        setOpaque(false);
        setPreferredSize(new Dimension(640, 400));
        setMinimumSize(new Dimension(320, 200));

        getAccessibleContext().setAccessibleName(
                "야구장 장면. 단서는 그림 속 대상을 클릭하거나 아래 선택 버튼으로 선택할 수 있습니다."
        );
        getAccessibleContext().setAccessibleDescription(
                "선수, 주자, 공과 상황 정보가 표시된 야구장 도식입니다."
        );

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                selectAt(event.getPoint());
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                setCursor(Cursor.getPredefinedCursor(
                        onSelect != null && hitTest(toVirtual(event.getPoint())) != null
                                ? Cursor.HAND_CURSOR
                                : Cursor.DEFAULT_CURSOR
                ));
            }
        });
    }

    public void scene(Scene value) {
        if (value == null) {
            throw new IllegalArgumentException("scene must not be null");
        }
        scene = value;
        selected = "";
        repaint();
    }

    public void selected(String id) {
        selected = id == null ? "" : id;
        repaint();
    }

    public void onSelect(Consumer<String> callback) {
        onSelect = callback;
        setCursor(Cursor.getPredefinedCursor(
                callback == null ? Cursor.DEFAULT_CURSOR : Cursor.HAND_CURSOR
        ));
    }

    private void selectAt(Point point) {
        if (onSelect == null || scene == null) {
            return;
        }

        Point2D.Double virtual = toVirtual(point);
        if (virtual == null) {
            return; // 비율 유지로 생긴 여백 클릭은 무시한다.
        }

        Scene.Token token = hitTest(virtual);
        if (token != null) {
            onSelect.accept(token.id());
        }
    }

    /** 화면 좌표를 800x500 가상 캔버스로 변환한다. 여백이면 null을 반환한다. */
    private Point2D.Double toVirtual(Point point) {
        if (getWidth() <= 0 || getHeight() <= 0) {
            return null;
        }

        double scale = Math.min(
                getWidth() / (double) FIELD_WIDTH,
                getHeight() / (double) FIELD_HEIGHT
        );

        double offsetX = (getWidth() - FIELD_WIDTH * scale) / 2.0;
        double offsetY = (getHeight() - FIELD_HEIGHT * scale) / 2.0;

        double x = (point.x - offsetX) / scale;
        double y = (point.y - offsetY) / scale;

        if (x < 0 || x > FIELD_WIDTH || y < 0 || y > FIELD_HEIGHT) {
            return null;
        }
        return new Point2D.Double(x, y);
    }

    /**
     * 화면에 실제 그려진 선수·토큰·라벨 영역을 기준으로 클릭 대상을 찾는다.
     * 가까이 겹친 토큰은 가장 가까운 중심을 우선한다.
     */
    private Scene.Token hitTest(Point2D point) {
        if (point == null || scene == null) {
            return null;
        }

        Scene.Token best = null;
        double bestDistance = Double.MAX_VALUE;

        for (Scene.Token token : scene.tokens()) {
            int x = token.x();
            int y = token.y();
            int labelX = labelX(token);

            boolean body = new Rectangle2D.Double(x - 25, y - 26, 50, 52).contains(point);
            int labelWidth = Math.max(60, getFontMetrics(Theme.font(19, true))
                    .stringWidth(token.label()) + 24);
            boolean caption = new Rectangle2D.Double(
                    labelX - labelWidth / 2.0, y + 22, labelWidth, 35
            ).contains(point);

            boolean name = false;
            if (token.player() != null) {
                int nameWidth = getFontMetrics(Theme.font(19, true))
                        .stringWidth(token.player().displayName()) + 22;
                name = new Rectangle2D.Double(
                        labelX - nameWidth / 2.0, y - 59, nameWidth, 33
                ).contains(point);
            }

            if (body || caption || name) {
                double dx = point.getX() - x;
                double dy = point.getY() - y;
                double distance = dx * dx + dy * dy;
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = token;
                }
            }
        }
        return best;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D outer = (Graphics2D) graphics.create();
        try {
            outer.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            outer.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            outer.setColor(BACKGROUND);
            outer.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);

            if (getWidth() <= 0 || getHeight() <= 0) {
                return;
            }

            double scale = Math.min(
                    getWidth() / (double) FIELD_WIDTH,
                    getHeight() / (double) FIELD_HEIGHT
            );

            outer.translate(
                    (getWidth() - FIELD_WIDTH * scale) / 2.0,
                    (getHeight() - FIELD_HEIGHT * scale) / 2.0
            );
            outer.scale(scale, scale);

            Graphics2D g = (Graphics2D) outer.create();
            try {
                paintField(g);
            } finally {
                g.dispose();
            }
        } finally {
            outer.dispose();
        }
    }

    private void paintField(Graphics2D g) {
        g.setColor(GRASS_LIGHT);
        g.fill(new Arc2D.Double(15, -25, 770, 900, 42, 96, Arc2D.PIE));

        Shape previousClip = g.getClip();
        g.clip(new Arc2D.Double(15, -25, 770, 900, 42, 96, Arc2D.PIE));
        for (int i = 0; i < 10; i++) {
            g.setColor(i % 2 == 0 ? GRASS_DARK : GRASS_LIGHT);
            g.fillRect(0, i * 52, FIELD_WIDTH, 52);
        }
        g.setClip(previousClip);

        Polygon diamond = new Polygon(
                new int[]{400, 610, 400, 190},
                new int[]{425, 265, 105, 265},
                4
        );
        g.setColor(DIRT);
        g.fill(diamond);

        Polygon inner = new Polygon(
                new int[]{400, 551, 400, 249},
                new int[]{378, 265, 152, 265},
                4
        );
        g.setColor(INFIELD);
        g.fill(inner);

        g.setColor(new Color(0xF9F9E9));
        g.setStroke(new BasicStroke(3));
        g.drawLine(400, 425, 65, 170);
        g.drawLine(400, 425, 735, 170);

        g.setColor(DIRT);
        g.fillOval(369, 235, 62, 48);
        g.setColor(Color.WHITE);
        g.fillRoundRect(387, 255, 26, 8, 3, 3);

        base(g, 600, 265, "1루", 27, 4);
        base(g, 400, 112, "2루", 24, -5);
        base(g, 200, 265, "3루", -54, 4);

        g.setColor(Color.WHITE);
        g.fillPolygon(
                new int[]{388, 412, 412, 400, 388},
                new int[]{414, 414, 427, 436, 427},
                5
        );

        g.setFont(Theme.font(15, true));
        g.setColor(new Color(0x4B6F55));
        g.drawString("HOME", 376, 464);

        g.setColor(new Color(0x72956A));
        g.setFont(Theme.font(12, true));
        g.drawString("BASEUP  ·  FIELD NOTES", 34, 468);

        if (scene != null) {
            for (Scene.Token token : scene.tokens()) {
                paintToken(g, token);
            }
        }
    }

    private void base(Graphics2D g, int x, int y, String text, int dx, int dy) {
        g.setColor(Color.WHITE);
        g.fillPolygon(
                new int[]{x, x + 10, x, x - 10},
                new int[]{y - 10, y, y + 10, y},
                4
        );
        g.setFont(Theme.font(16, true));
        g.setColor(new Color(0x54724D));
        g.drawString(text, x + dx, y + dy);
    }

    private void paintToken(Graphics2D g, Scene.Token token) {
        int x = token.x();
        int y = token.y();
        int labelX = labelX(token);
        boolean chosen = selected.equals(token.id());

        if (chosen) {
            g.setColor(HIGHLIGHT);
            g.setStroke(new BasicStroke(4));
            g.drawOval(x - 28, y - 28, 56, 56);
        }

        Color color = switch (token.kind()) {
            case "runner" -> new Color(0xE6A052);
            case "fielder" -> new Color(0x286859);
            case "ball" -> Color.WHITE;
            default -> new Color(0x3A5450);
        };

        if (token.player() != null) {
            color = new Color(token.player().team().color());
        }

        g.setColor(new Color(0, 0, 0, 25));
        g.fillOval(x - 20, y - 16, 40, 40);

        g.setColor(color);
        g.fillOval(x - 20, y - 21, 40, 40);

        if (token.player() != null) {
            g.setColor(new Color(token.player().team().accent()));
            g.setStroke(new BasicStroke(3));
            g.drawOval(x - 20, y - 21, 40, 40);
        }

        g.setColor(Theme.contrast(color));
        g.setFont(Theme.font(16, true));
        String symbol = switch (token.kind()) {
            case "runner" -> "R";
            case "fielder" -> "D";
            case "ball" -> "●";
            default -> "!";
        };
        g.drawString(symbol, x - g.getFontMetrics().stringWidth(symbol) / 2, y + 5);

        if (token.player() != null) {
            g.setFont(Theme.font(19, true));
            String name = token.player().displayName();
            int nameWidth = g.getFontMetrics().stringWidth(name) + 18;
            g.setColor(color);

            if (labelX != x) {
                g.setStroke(new BasicStroke(2));
                g.drawLine(labelX, y - 28, x, y - 21);
            }

            g.fillRoundRect(labelX - nameWidth / 2, y - 57, nameWidth, 29, 8, 8);
            g.setColor(Theme.contrast(color));
            g.drawString(name, labelX - nameWidth / 2 + 9, y - 36);
        }

        g.setFont(Theme.font(19, true));
        int labelWidth = g.getFontMetrics().stringWidth(token.label()) + 20;
        g.setColor(chosen ? new Color(0xFFF1CC) : new Color(255, 255, 255, 240));
        g.fillRoundRect(labelX - labelWidth / 2, y + 24, labelWidth, 31, 9, 9);
        g.setColor(Theme.INK);
        g.drawString(token.label(), labelX - labelWidth / 2 + 10, y + 46);
    }

    /** 가까이 있는 선수는 토큰 위치를 유지하고 이름표만 좌우로 분리한다. */
    private int labelX(Scene.Token token) {
        if (token.player() == null) {
            return token.x();
        }

        for (Scene.Token other : scene.tokens()) {
            if (other.player() != null
                    && !other.id().equals(token.id())
                    && Math.abs(other.x() - token.x()) < 130
                    && Math.abs(other.y() - token.y()) < 100) {
                return token.x() + (token.x() < other.x() ? -100 : 100);
            }
        }
        return token.x();
    }
}
