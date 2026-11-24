package baseup.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;

/** 전광판 홈 화면 부품: 위쪽 탭 바, 야구장 배경, 9이닝 전광판, 반투명 카드. */
final class Ballpark {
    private Ballpark() {}

    static final Color BOARD = new Color(0x0D1B2A);              // 전광판 남색
    static final Color LAMP = new Color(0xFFC94A);               // 켜진 램프(노랑)
    static final Color CHALK = new Color(0xF4F1E8);

    static Graphics2D smooth(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        return g;
    }

    /** 홈 전체 배경: 야구장 그림을 꽉 채우고, 위는 진하게 아래로 갈수록 살짝 어둡게 덮는다. */
    static final class Stadium extends JPanel {
        private final BufferedImage image;
        Stadium(Path imagePath) {
            super(new BorderLayout());
            BufferedImage loaded = null;
            try { if (Files.isReadable(imagePath)) loaded = ImageIO.read(imagePath.toFile()); } catch (Exception ignored) {}
            image = loaded; setOpaque(true); setBackground(new Color(0x1E5B3A));
        }
        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = smooth(g0);
            int w = getWidth(), h = getHeight();
            if (image != null) {
                double s = Math.max((double) w / image.getWidth(), (double) h / image.getHeight());
                int iw = (int) (image.getWidth() * s), ih = (int) (image.getHeight() * s);
                g.drawImage(image, (w - iw) / 2, (h - ih) / 2, iw, ih, null);
            }
            g.setPaint(new LinearGradientPaint(0, 0, 0, h, new float[]{0f, .45f, 1f},
                    new Color[]{new Color(13, 27, 42, 120), new Color(13, 27, 42, 40), new Color(13, 27, 42, 110)}));
            g.fillRect(0, 0, w, h);
            g.dispose();
        }
    }

    /** 반투명 흰 카드: 배경 야구장이 살짝 비친다. */
    static final class Glass extends JPanel {
        private final Color fill;
        Glass(Color fill) { this.fill = fill; setOpaque(false); setBorder(new EmptyBorder(22, 24, 22, 24)); }
        Glass() { this(new Color(255, 255, 255, 238)); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = smooth(g0); g.setColor(fill); g.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 22, 22)); g.dispose();
        }
    }

    /** 색 섞기: t=0이면 a, t=1이면 b. */
    static Color mix(Color a, Color b, double t) {
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t), (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    /**
     * XP 주루 게이지: 홈 → 1루(루키 심판) → 2루(규칙 탐험가) → 홈인(한국시리즈 주심: 최고 무대의 심판).
     * 칭호 구간마다 같은 길이로 나눠서, 처음 구간도 짧게 보이지 않게 한다.
     */
    static final class XpRunway extends JComponent {
        private final int xp, max;
        private final int[] marks;
        private final String[] names;
        XpRunway(int xp, int max) {
            this.xp = xp; this.max = Math.max(max, 301);
            marks = new int[]{0, 120, 300, this.max};
            names = new String[]{"신입 심판", "루키 심판", "규칙 탐험가", "한국시리즈 주심"};
            setPreferredSize(new Dimension(600, 74)); setMinimumSize(new Dimension(300, 74));
        }
        private double pos(int value) {                       // XP를 0~1 위치로 바꾸기 (구간별 같은 길이)
            for (int i = 0; i < marks.length - 1; i++)
                if (value <= marks[i + 1]) return (i + (value - marks[i]) / (double) (marks[i + 1] - marks[i])) / (marks.length - 1);
            return 1;
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = smooth(g0);
            int w = getWidth(), pad = 26, track = 12, y = 34;
            double len = w - pad * 2, now = pos(xp);
            g.setColor(new Color(0x5A3B26)); g.fill(new RoundRectangle2D.Double(pad, y - track / 2.0, len, track, track, track));
            g.setPaint(new GradientPaint(pad, 0, LAMP, (float) (pad + len), 0, new Color(0xFF7A3D)));
            g.fill(new RoundRectangle2D.Double(pad, y - track / 2.0, Math.max(track, len * now), track, track, track));
            g.setColor(new Color(255, 255, 255, 70));                      // 베이스 사이 분필 라인
            for (int x = pad; x < pad + len; x += 10) g.fillRect(x, y - 1, 5, 2);
            g.setFont(Theme.font(12, true));
            for (int i = 0; i < marks.length; i++) {
                double x = pad + len * i / (marks.length - 1.0);
                boolean reached = xp >= marks[i];
                Path2D base = new Path2D.Double();
                if (i == 0 || i == marks.length - 1) {                    // 홈플레이트 모양
                    base.moveTo(x - 9, y - 9); base.lineTo(x + 9, y - 9); base.lineTo(x + 9, y + 2); base.lineTo(x, y + 10); base.lineTo(x - 9, y + 2);
                } else { base.moveTo(x, y - 11); base.lineTo(x + 11, y); base.lineTo(x, y + 11); base.lineTo(x - 11, y); }
                base.closePath();
                g.setColor(reached ? LAMP : CHALK); g.fill(base);
                g.setColor(new Color(0x0D1B2A)); g.setStroke(new BasicStroke(2f)); g.draw(base);
                String label = names[i] + (i > 0 && i < marks.length - 1 ? " " + marks[i] : "");
                int tw = g.getFontMetrics().stringWidth(label);
                g.setColor(reached ? LAMP : new Color(0x9FB0C6));
                g.drawString(label, (float) Math.max(0, Math.min(w - tw, x - tw / 2.0)), y + 30);
            }
            double rx = pad + len * now;                                  // 주자(야구공)
            g.setColor(new Color(0, 0, 0, 80)); g.fill(new Ellipse2D.Double(rx - 12, y - 9, 24, 24));
            g.setColor(CHALK); g.fill(new Ellipse2D.Double(rx - 12, y - 12, 24, 24));
            g.setColor(new Color(0xE0483C)); g.setStroke(new BasicStroke(1.8f));
            g.draw(new Arc2D.Double(rx - 18, y - 10, 12, 20, -60, 120, Arc2D.OPEN));
            g.draw(new Arc2D.Double(rx + 6, y - 10, 12, 20, 120, 120, Arc2D.OPEN));
            String tag = xp + " XP";
            g.setFont(Theme.font(12, true)); int tw = g.getFontMetrics().stringWidth(tag);
            double tx = Math.max(0, Math.min(w - tw - 12, rx - tw / 2.0 - 6));
            g.setColor(LAMP); g.fill(new RoundRectangle2D.Double(tx, 0, tw + 12, 18, 9, 9));
            g.setColor(BOARD); g.drawString(tag, (float) tx + 6, 13);
            g.dispose();
        }
    }

    /**
     * 그림 자리: data/폴더/팀ID.png 파일이 있으면 그 그림을, 없으면 점선 상자로 "어떤 파일을 넣을 자리"인지 보여 준다.
     * 구단 로고·마스코트 파일은 팀에서 사용 여부를 확인한 뒤 직접 넣는다.
     */
    static final class ImageSlot extends JComponent {
        private final String caption, file; private BufferedImage image;
        ImageSlot(String folder, String teamId, String caption, int w, int h) {
            this.caption = caption; this.file = "data/" + folder + "/" + teamId + ".png";
            try { Path f = Path.of(file); if (Files.isReadable(f)) image = ImageIO.read(f.toFile()); } catch (Exception ignored) {}
            Dimension d = new Dimension(w, h); setPreferredSize(d); setMinimumSize(d); setMaximumSize(d);
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = smooth(g0); int w = getWidth(), h = getHeight();
            if (image != null) {
                double s = Math.min((double) w / image.getWidth(), (double) h / image.getHeight());
                int iw = (int) (image.getWidth() * s), ih = (int) (image.getHeight() * s);
                g.drawImage(image, (w - iw) / 2, h - ih, iw, ih, null); g.dispose(); return;
            }
            g.setColor(new Color(255, 255, 255, 30)); g.fill(new RoundRectangle2D.Double(1, 1, w - 2, h - 2, 16, 16));
            g.setColor(new Color(255, 255, 255, 170));
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{6, 5}, 0));
            g.draw(new RoundRectangle2D.Double(1, 1, w - 2, h - 2, 16, 16));
            g.setFont(Theme.font(13, true)); FontMetrics m = g.getFontMetrics();
            g.drawString(caption, (w - m.stringWidth(caption)) / 2, h / 2 - 2);
            g.setFont(Theme.font(10, false)); m = g.getFontMetrics();
            String f = file.substring(file.lastIndexOf('/') + 1);
            g.drawString(f, (w - m.stringWidth(f)) / 2, h / 2 + 15);
            g.dispose();
        }
    }

}
