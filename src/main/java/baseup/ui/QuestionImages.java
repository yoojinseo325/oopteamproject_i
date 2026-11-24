package baseup.ui;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/** 현재 문항의 이미지 하나만 보관한다. 단계 전환/확대 시 파일을 다시 읽지 않는다. */
final class QuestionImages {
    private Path path;
    private BufferedImage image;

    BufferedImage load(Path file) throws IOException {
        Path requested = file.toAbsolutePath().normalize();
        if (!requested.equals(path)) {
            BufferedImage loaded = ImageIO.read(requested.toFile());
            if (loaded == null) throw new IOException("그림을 읽을 수 없습니다: " + file);
            image = loaded;
            path = requested;
        }
        return image;
    }
}
