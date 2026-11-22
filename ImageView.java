package baseup.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/**
 * 삽화를 원본 비율로 표시한다.
 * 패널 크기가 바뀌어도 이미지를 자르거나 원본 파일을 변경하지 않는다.
 */
public final class ImageView extends JPanel {
    private final BufferedImage image;

    public ImageView(Path file, String description) throws IOException {
        if (file == null) {
            throw new IOException("그림 파일 경로가 없습니다.");
        }

        image = ImageIO.read(file.toFile());
        if (image == null) {
            throw new IOException("그림을 읽을 수 없습니다: " + file);
        }

        setOpaque(true);
        setBackground(Theme.WHITE);
        setPreferredSize(new Dimension(600, 400));
        setMinimumSize(new Dimension(200, 140));

        String accessibleDescription = description == null || description.isBlank()
                ? "학습 문제 삽화"
                : description;
        getAccessibleContext().setAccessibleName(accessibleDescription);
        getAccessibleContext().setAccessibleDescription(
                "원본 이미지 비율을 유지하여 표시하는 학습 삽화입니다."
        );
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        if (image == null || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }

        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

            double scale = Math.min(
                    getWidth() / (double) image.getWidth(),
                    getHeight() / (double) image.getHeight()
            );

            int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(image.getHeight() * scale));
            int x = (getWidth() - width) / 2;
            int y = (getHeight() - height) / 2;

            g.drawImage(image, x, y, width, height, this);
        } finally {
            g.dispose();
        }
    }
}
