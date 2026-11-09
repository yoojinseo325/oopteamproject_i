package baseup.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

/** images 폴더 이미지를 보여 준다. 미첨부 시 정답 노출 없는 야구장 안내화면. */
public final class ImageView extends JPanel {
    private String fileName = "";
    private BufferedImage image;
    public ImageView() {
        setPreferredSize(new Dimension(730, 300));
        setBackground(new Color(22, 43, 59));
    }
    public void setImageFile(String file) {
        fileName = file;
        image = null;
        Path path = Path.of("images", file);
        if (Files.isRegularFile(path)) {
            try { image = ImageIO.read(path.toFile()); }
            catch (IOException ignored) { image = null; }
        }
        repaint();
    }
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D)graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        int w = getWidth(), h = getHeight();
        if (image != null) {
            double scale = Math.min((double)(w - 20)/image.getWidth(), (double)(h - 20)/image.getHeight());
            int iw = (int)(image.getWidth() * scale), ih = (int)(image.getHeight() * scale);
            g.drawImage(image, (w-iw)/2, (h-ih)/2, iw, ih, null);
        } else {
            g.setColor(new Color(23, 71, 77));
            g.fillRect(0, 0, w, h);
            int size = Math.max(45, Math.min(w / 5, (h - 105) / 2));
            int cx = w / 2, cy = Math.max(size + 10, (h - 105) / 2);
            Polygon diamond = new Polygon(new int[]{cx, cx+size, cx, cx-size},
                    new int[]{cy-size, cy, cy+size, cy}, 4);
            g.setColor(new Color(43, 117, 108));
            g.fillPolygon(diamond);
            g.setColor(new Color(135, 211, 180));
            g.setStroke(new BasicStroke(3f));
            g.drawPolygon(diamond);
            for (int[] pt : new int[][]{{cx,cy-size},{cx+size,cy},{cx,cy+size},{cx-size,cy}}) {
                g.setColor(new Color(237, 229, 210));
                g.fillRect(pt[0]-5, pt[1]-5, 10, 10);
            }
            g.setFont(Theme.font(Font.BOLD, 15));
            g.setColor(Theme.WHITE);
            center(g, "야구 이미지가 아직 등록되지 않았습니다", w/2, h-55);
            g.setColor(Theme.MUTED);
            g.setFont(Theme.font(Font.PLAIN, 13));
            center(g, "images/" + fileName + " 파일을 넣으면 이곳에 표시됩니다", w/2, h-31);
        }
        g.dispose();
    }
    private static void center(Graphics2D g, String value, int centerX, int y) {
        g.drawString(value, centerX - g.getFontMetrics().stringWidth(value)/2, y);
    }
}
