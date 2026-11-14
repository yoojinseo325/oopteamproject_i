package baseup.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/** V2 삽화를 원본 비율로 표시한다. 화면 확대는 원본 파일을 변경하지 않는다. */
public final class ImageView extends JPanel {
    private final BufferedImage image;
    public ImageView(Path file, String description) throws IOException {
        image = ImageIO.read(file.toFile());
        if (image == null) throw new IOException("그림을 읽을 수 없습니다: " + file);
        setBackground(Theme.WHITE);
        setPreferredSize(new Dimension(600,400));
        setMinimumSize(new Dimension(200,140));
        getAccessibleContext().setAccessibleName(description);
    }
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        double scale = Math.min(getWidth() / (double) image.getWidth(), getHeight() / (double) image.getHeight());
        int w = (int) (image.getWidth() * scale), h = (int) (image.getHeight() * scale);
        g.drawImage(image, (getWidth()-w)/2, (getHeight()-h)/2, w, h, null);
        g.dispose();
    }
}
