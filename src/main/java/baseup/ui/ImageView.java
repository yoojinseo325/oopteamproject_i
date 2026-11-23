package baseup.ui;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** 이미지 기반 문제의 PNG 파일을 비율에 맞추어 출력한다. */
public final class ImageView extends JPanel {
    private final BufferedImage image;
    private final String title;

    public ImageView(Path imagePath, String title) throws IOException {
        if (imagePath == null || !Files.isRegularFile(imagePath)) {
            throw new IOException("이미지 파일이 없습니다: " + imagePath);
        }
        BufferedImage loaded = ImageIO.read(imagePath.toFile());
        if (loaded == null) {
            throw new IOException("이미지 파일을 읽을 수 없습니다: " + imagePath);
        }
        this.image = loaded;
        this.title = title == null ? "" : title;
        setBackground(Theme.WHITE);
        setOpaque(true);
        setPreferredSize(new Dimension(730, 320));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            int availableWidth = Math.max(1, getWidth() - 20);
            int availableHeight = Math.max(1, getHeight() - 20);
            double scale = Math.min((double) availableWidth / image.getWidth(),
                    (double) availableHeight / image.getHeight());
            int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(image.getHeight() * scale));
            g.drawImage(image, (getWidth() - width) / 2,
                    (getHeight() - height) / 2, width, height, null);
        } finally {
            g.dispose();
        }
    }
}
