package baseup.ui;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 사용자가 선택한 세 가지 아이콘을 Java 클래스에 내장하여 배포한다.
 * 실행 디렉터리 / assets 위치 / JAR 실행 방식과 관계없이 같은 아이콘을 표시한다.
 * 아이콘 출처의 재배포 라이선스는 팀에서 별도 확인한다.
 */
final class MyPageIcons {
    private static final Color TINT = DashboardStyle.ACCENT;
    private static final Map<String, String> ICON_DATA = Map.of(
            "path", "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",
            "growth", "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",
            "notes", "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"
    );
    private static final Map<String, BufferedImage> CACHE = new HashMap<>();
    private static final Map<String, BufferedImage> COLOR_CACHE = new HashMap<>();

    private MyPageIcons() { }

    // 카드용 기본 녹색 아이콘. 기존 사용부와 호환된다.
    static void paint(Graphics2D g, String type, int x, int y, int size) {
        paint(g, type, x, y, size, TINT);
    }

    // 사이드바에서 같은 원본 모양을 밝은 색으로 표시할 때 사용한다.
    static void paint(Graphics2D g, String type, int x, int y, int size, Color color) {
        BufferedImage picture = recoloredIcon(type, color);
        Object old = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(picture, x, y, size, size, null);
        if (old != null) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, old);
        }
    }

    private static synchronized BufferedImage recoloredIcon(String type, Color color) {
        if (TINT.equals(color)) return icon(type);
        String key = type + ":" + color.getRGB();
        BufferedImage cached = COLOR_CACHE.get(key);
        if (cached != null) return cached;
        BufferedImage original = icon(type);
        BufferedImage result = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        try {
            graphics.drawImage(original, 0, 0, null);
            graphics.setComposite(AlphaComposite.SrcIn);
            graphics.setColor(color);
            graphics.fillRect(0, 0, result.getWidth(), result.getHeight());
        } finally {
            graphics.dispose();
        }
        COLOR_CACHE.put(key, result);
        return result;
    }

    private static synchronized BufferedImage icon(String type) {
        BufferedImage existing = CACHE.get(type);
        if (existing != null) return existing;
        String data = ICON_DATA.get(type);
        if (data == null) throw new IllegalArgumentException("알 수 없는 마이페이지 아이콘: " + type);
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(bytes));
            if (original == null) throw new IOException("이미지 디코딩 실패");
            BufferedImage tinted = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D ig = tinted.createGraphics();
            try {
                ig.drawImage(original, 0, 0, null);
                ig.setComposite(AlphaComposite.SrcIn);
                ig.setColor(TINT);
                ig.fillRect(0, 0, tinted.getWidth(), tinted.getHeight());
            } finally {
                ig.dispose();
            }
            CACHE.put(type, tinted);
            return tinted;
        } catch (IOException | IllegalArgumentException ex) {
            throw new IllegalStateException("내장 마이페이지 아이콘을 읽지 못했습니다: " + type, ex);
        }
    }
}
