package baseup.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * 오른쪽 위 작은 LED 카운트판. PitchGame의 볼·스트라이크·아웃과 점수를 보여 준다.
 * LedPainter로: 작은 그림에 점을 찍고 → LED로 크게 그린다.
 */
final class GameBoard extends JComponent {
    private static final int W = 72, H = 44;
    private final PitchGame game;
    private final Timer timer = new Timer(200, e -> repaint());

    GameBoard(PitchGame game) {
        this.game = game;
        setPreferredSize(new Dimension(330, 330 * H / W + 24));
        setToolTipText("<html>B = 볼 (4개면 볼넷)<br>S = 스트라이크 (3개면 삼진)<br>O = 아웃 (3개면 공수 교대)<br>R = 점수 · H = 안타 · IN = 이닝</html>");
        game.addListener(this::repaint);
    }

    // 화면에 붙어 있을 때만 타이머를 돌린다 (페이지를 옮길 때마다 타이머가 쌓이지 않게)
    @Override public void addNotify() { super.addNotify(); timer.start(); }
    @Override public void removeNotify() { timer.stop(); super.removeNotify(); }

    @Override public Dimension getMaximumSize() { return getPreferredSize(); }

    private BufferedImage draw() {
        BufferedImage c = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = c.createGraphics();
        lamps(g, "B", 3, 3, game.state().balls(), LedPainter.GREEN);
        lamps(g, "S", 13, 2, game.state().strikes(), LedPainter.AMBER);
        lamps(g, "O", 23, 2, game.state().outs(), LedPainter.RED);
        // 오른쪽: 점수, 안타, 이닝
        LedPainter.dots(g, "R", 46, 3, 1, LedPainter.DIM);  LedPainter.dots(g, String.valueOf(Math.min(99, game.state().runs())), 60, 3, 1, LedPainter.WHITE);
        LedPainter.dots(g, "H", 46, 13, 1, LedPainter.DIM); LedPainter.dots(g, String.valueOf(Math.min(99, game.state().hits())), 60, 13, 1, LedPainter.WHITE);
        LedPainter.dots(g, "IN", 48, 23, 1, LedPainter.DIM); LedPainter.dots(g, String.valueOf(Math.min(9, game.state().inning())), 62, 23, 1, LedPainter.AMBER);
        // 아래: 주자 다이아몬드 (불 켜진 루 = 주자 있음)
        int cx = 36, cy = 37, r = 5;
        g.setColor(new Color(0x6B7686));
        for (int i = 0; i <= r; i++) {
            g.fillRect(cx + i, cy + r - i, 1, 1); g.fillRect(cx + r - i, cy - i, 1, 1);
            g.fillRect(cx - i, cy - r + i, 1, 1); g.fillRect(cx - r + i, cy + i, 1, 1);
        }
        int[][] bases = {{cx + r, cy}, {cx, cy - r}, {cx - r, cy}};
        for (int i = 0; i < 3; i++) { g.setColor(game.state().onBase(i) ? LedPainter.AMBER : new Color(0x30363D)); g.fillRect(bases[i][0] - 1, bases[i][1] - 1, 3, 3); }
        g.setColor(LedPainter.WHITE); g.fillRect(cx - 1, cy + r - 1, 3, 2);
        g.dispose();
        return c;
    }

    /** 글자 하나 + 동그란 램프 n개 (켜진 개수만큼 색). */
    private static void lamps(Graphics2D g, String label, int y, int n, int on, Color color) {
        LedPainter.dots(g, label, 5, y, 1, LedPainter.WHITE);
        for (int i = 0; i < n; i++) {
            g.setColor(i < on ? color : new Color(0x30363D));
            g.fillRect(12 + i * 8, y + 1, 5, 5);
        }
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        LedPainter.paintLeds(g, draw(), getWidth(), getHeight());
        g.dispose();
    }
}
