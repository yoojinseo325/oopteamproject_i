package baseup.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import static baseup.ui.Theme.*;

/** 좌측 야구장 사진 / 브랜드 메시지. 사진이 없으면 프로그램 내장 도식이 표시된다. */
public final class BrandPanel extends JPanel {
    private final BufferedImage stadium;

    public BrandPanel(Path projectRoot) {
        setLayout(new BorderLayout());
        setBorder(javax.swing.BorderFactory.createEmptyBorder(36, 38, 46, 36));
        setOpaque(false);
        stadium = loadPhoto(projectRoot);
        JPanel top = new JPanel(new BorderLayout()); top.setOpaque(false);
        top.add(label("BASEUP", 30, WHITE, true), BorderLayout.WEST);
        add(top, BorderLayout.NORTH);
        JPanel bottom = new JPanel(); bottom.setOpaque(false); bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        JLabel kicker = label("LEARN THE GAME", 12, new Color(0xB8DFD0), true);
        JLabel title1 = label("야구 규칙을 더 쉽게,", 27, WHITE, true);
        JLabel title2 = label("한 단계씩.", 27, WHITE, true);
        bottom.add(kicker);bottom.add(Box.createVerticalStrut(13));
        bottom.add(title1);bottom.add(Box.createVerticalStrut(4));bottom.add(title2);
        bottom.add(Box.createVerticalStrut(16));
        JLabel subtitle1 = label("다양한 야구 상황을 통해 판정 원리를 배우고,", 13, new Color(0xE1EAE6), false);
        JLabel subtitle2 = label("나만의 학습 기록을 쌓아보세요.", 13, new Color(0xE1EAE6), false);
        bottom.add(subtitle1);bottom.add(Box.createVerticalStrut(5));bottom.add(subtitle2);
        add(bottom, BorderLayout.SOUTH);
    }

    private BufferedImage loadPhoto(Path root) {
        for (String filename : new String[]{"login-stadium.jpg", "login-stadium.png"}) {
            Path path = root.resolve("assets").resolve(filename);
            if (Files.isRegularFile(path)) try { return ImageIO.read(path.toFile()); }
            catch (Exception ignored) { }
        }
        return null;
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w=getWidth(), h=getHeight();
        if (stadium != null) {
            double scale=Math.max((double)w/stadium.getWidth(), (double)h/stadium.getHeight());
            int imgW=(int)Math.ceil(stadium.getWidth()*scale), imgH=(int)Math.ceil(stadium.getHeight()*scale);
            g.drawImage(stadium, (w-imgW)/2, (h-imgH)/2, imgW, imgH, null);
        } else {
            g.setPaint(new GradientPaint(0,0,new Color(0x233D42),0,h,new Color(0x166347)));
            g.fillRect(0,0,w,h);
            // 기본 도식은 사용자가 Figma 배경 사진을 내보내지 않았을 때만 표시한다.
            int mid=w/2, cy=(int)(h*.64), r=Math.min(w,h)/2;
            Path2D field=new Path2D.Double();
            field.moveTo(mid,cy+r*.60);field.lineTo(mid-r*.81,cy-r*.16);
            field.lineTo(mid,cy-r*.89);field.lineTo(mid+r*.81,cy-r*.16);field.closePath();
            g.setColor(new Color(0x2E8859));g.fill(field);
            g.setStroke(new BasicStroke(2f));g.setColor(new Color(255,255,255,90));g.draw(field);
            g.setColor(new Color(0xA78359));
            int d=(int)(r*.30);g.fillOval(mid-d/2,cy-d/2,d,d);
        }
        g.setPaint(new GradientPaint(0,0,new Color(0,23,22,205),0,h,new Color(0,20,18,194)));
        g.fillRect(0,0,w,h);
        g.dispose();
        super.paintComponent(graphics);
    }
}
