package baseup.ui;

import baseup.model.Attempt;
import baseup.model.LearningStep;
import baseup.service.LearningService;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * LED 도트 그리기 도구.
 * 작은 그림(점 하나 = LED 하나)에 5x7 글자를 찍고(dots), 그 그림을 동그란 LED들로 크게 그린다(paintLeds).
 * 오른쪽 위 카운트판(GameBoard)과 게임 광고판(PitchGame)이 같이 쓴다.
 */
final class LedPainter {
    private LedPainter() { }

    static final Color RED = new Color(0xFF4D4D), AMBER = new Color(0xFFC94A), GREEN = new Color(0x4ADE80),
            WHITE = new Color(0xF2F2F2), BLUE = new Color(0xC9D3DE), DIM = new Color(0xA9B4C2);



    // ---------- 작은 그림(점 단위)에 글자 찍기 ----------
    /** 5x7 도트 글꼴. 한 글자 = 7줄, 각 줄 5칸('#'=켜짐). 실제 전광판처럼 숫자와 영문만 쓴다. */
    private static final java.util.Map<Character, String[]> GLYPHS = new java.util.HashMap<>();
    static {
        String[][] g = {
            {"0"," ### ","#   #","#  ##","# # #","##  #","#   #"," ### "},{"1","  #  "," ##  ","  #  ","  #  ","  #  ","  #  "," ### "},
            {"2"," ### ","#   #","    #","   # ","  #  "," #   ","#####"},{"3","#####","   # ","  #  ","   # ","    #","#   #"," ### "},
            {"4","   # ","  ## "," # # ","#  # ","#####","   # ","   # "},{"5","#####","#    ","#### ","    #","    #","#   #"," ### "},
            {"6","  ## "," #   ","#    ","#### ","#   #","#   #"," ### "},{"7","#####","    #","   # ","  #  "," #   "," #   "," #   "},
            {"8"," ### ","#   #","#   #"," ### ","#   #","#   #"," ### "},{"9"," ### ","#   #","#   #"," ####","    #","   # "," ##  "},
            {"A"," ### ","#   #","#   #","#####","#   #","#   #","#   #"},{"B","#### ","#   #","#   #","#### ","#   #","#   #","#### "},
            {"C"," ### ","#   #","#    ","#    ","#    ","#   #"," ### "},{"D","#### ","#   #","#   #","#   #","#   #","#   #","#### "},
            {"E","#####","#    ","#    ","#### ","#    ","#    ","#####"},{"F","#####","#    ","#    ","#### ","#    ","#    ","#    "},
            {"G"," ### ","#   #","#    ","# ###","#   #","#   #"," ####"},{"H","#   #","#   #","#   #","#####","#   #","#   #","#   #"},
            {"I"," ### ","  #  ","  #  ","  #  ","  #  ","  #  "," ### "},{"L","#    ","#    ","#    ","#    ","#    ","#    ","#####"},
            {"M","#   #","## ##","# # #","# # #","#   #","#   #","#   #"},{"N","#   #","##  #","# # #","#  ##","#   #","#   #","#   #"},
            {"O"," ### ","#   #","#   #","#   #","#   #","#   #"," ### "},{"P","#### ","#   #","#   #","#### ","#    ","#    ","#    "},
            {"R","#### ","#   #","#   #","#### ","# #  ","#  # ","#   #"},{"S"," ####","#    ","#    "," ### ","    #","    #","#### "},
            {"T","#####","  #  ","  #  ","  #  ","  #  ","  #  ","  #  "},{"U","#   #","#   #","#   #","#   #","#   #","#   #"," ### "},
            {"V","#   #","#   #","#   #","#   #","#   #"," # # ","  #  "},{"W","#   #","#   #","#   #","# # #","# # #","## ##","#   #"},
            {"X","#   #","#   #"," # # ","  #  "," # # ","#   #","#   #"},{"Y","#   #","#   #"," # # ","  #  ","  #  ","  #  ","  #  "},
            {":","     ","  #  ","  #  ","     ","  #  ","  #  ","     "},{"/","    #","    #","   # ","  #  "," #   ","#    ","#    "},
            {"-","     ","     ","     ","#####","     ","     ","     "},{"!","  #  ","  #  ","  #  ","  #  ","  #  ","     ","  #  "},
            {"K","#   #","#  # ","# #  ","##   ","# #  ","#  # ","#   #"},{"J","  ###","   # ","   # ","   # ","   # ","#  # "," ##  "},
            {"Q"," ### ","#   #","#   #","#   #","# # #","#  # "," ## #"},{"Z","#####","    #","   # ","  #  "," #   ","#    ","#####"},
            {" ","     ","     ","     ","     ","     ","     ","     "}};
        for (String[] row : g) GLYPHS.put(row[0].charAt(0), java.util.Arrays.copyOfRange(row, 1, 8));
    }
    static int width(String s, int scale) { return s.isEmpty() ? 0 : (s.length() * 6 - 1) * scale; }
    /** 도트 글자를 (cx 가운데, top 위쪽) 기준으로 찍는다. scale=2면 두 배 크기. */
    static void dots(Graphics2D g, String s, int cx, int top, int scale, Color c) {
        g.setColor(c);
        int x = cx - width(s, scale) / 2;
        for (char ch : s.toUpperCase().toCharArray()) {
            String[] glyph = GLYPHS.getOrDefault(ch, GLYPHS.get(' '));
            for (int r = 0; r < 7; r++) for (int k = 0; k < 5; k++)
                if (glyph[r].charAt(k) == '#') g.fillRect(x + k * scale, top + r * scale, scale, scale);
            x += 6 * scale;
        }
    }


    /** 작은 그림(canvas)의 점 하나하나를 동그란 LED로 크게 그린다. (GameBoard, PitchGame) */
    static void paintLeds(Graphics2D g, BufferedImage canvas, int compW, int compH) {
        int W = canvas.getWidth(), H = canvas.getHeight();
        g = (Graphics2D) g.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double pitch = Math.min((compW - 24) / (double) W, (compH - 24) / (double) H);
        double pw = pitch * W, ph = pitch * H, ox = (compW - pw) / 2, oy = (compH - ph) / 2;

        // 패널 틀: 금속 테두리 + 모서리 나사
        g.setPaint(new GradientPaint(0, (float) oy - 12, new Color(0x3A4250), 0, (float) (oy + ph + 12), new Color(0x161A21)));
        g.fill(new java.awt.geom.RoundRectangle2D.Double(ox - 12, oy - 12, pw + 24, ph + 24, 16, 16));
        g.setColor(new Color(0x050608)); g.fill(new java.awt.geom.Rectangle2D.Double(ox - 3, oy - 3, pw + 6, ph + 6));
        for (double[] p : new double[][]{{ox - 6, oy - 6}, {ox + pw + 6, oy - 6}, {ox - 6, oy + ph + 6}, {ox + pw + 6, oy + ph + 6}}) {
            g.setColor(new Color(0x8A93A3)); g.fill(new java.awt.geom.Ellipse2D.Double(p[0] - 3, p[1] - 3, 6, 6));
            g.setColor(new Color(0x2A2F38)); g.drawLine((int) p[0] - 2, (int) p[1], (int) p[0] + 2, (int) p[1]);
        }

        double d = pitch * 0.74, glow = pitch * 1.45;
        Color off = new Color(0x1E242D), offRim = new Color(0x0C0F14);
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            int rgb = canvas.getRGB(x, y) & 0xFFFFFF;
            int r = rgb >> 16, gr = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
            double cx = ox + x * pitch + pitch / 2, cy = oy + y * pitch + pitch / 2;
            if (r + gr + b > 60) {
                r = Math.min(255, r + 40); gr = Math.min(255, gr + 40); b = Math.min(255, b + 40);   // 더 밝게
                g.setColor(new Color(r, gr, b, 95));                                         // 빛 번짐
                g.fill(new java.awt.geom.Ellipse2D.Double(cx - glow / 2, cy - glow / 2, glow, glow));
                g.setColor(new Color(r, gr, b));                                             // LED 알
                g.fill(new java.awt.geom.Ellipse2D.Double(cx - d / 2, cy - d / 2, d, d));
                g.setColor(new Color(255, 255, 255, 150));                                   // 반사광
                g.fill(new java.awt.geom.Ellipse2D.Double(cx - d * 0.28, cy - d * 0.32, d * 0.3, d * 0.3));
            } else {
                g.setColor(offRim); g.fill(new java.awt.geom.Ellipse2D.Double(cx - d / 2, cy - d / 2 + 0.6, d, d));
                g.setColor(off); g.fill(new java.awt.geom.Ellipse2D.Double(cx - d / 2, cy - d / 2, d, d));
            }
        }
        // 패널 모듈 이음새 (32x16 점마다)
        g.setColor(new Color(0, 0, 0, 160));
        for (int x = 32; x < W; x += 32) g.fill(new java.awt.geom.Rectangle2D.Double(ox + x * pitch - 0.7, oy, 1.4, ph));
        for (int y = 16; y < H; y += 16) g.fill(new java.awt.geom.Rectangle2D.Double(ox, oy + y * pitch - 0.7, pw, 1.4));
        // 유리판 반사
        g.setPaint(new GradientPaint((float) ox, (float) oy, new Color(255, 255, 255, 18), (float) ox, (float) (oy + ph * 0.45), new Color(255, 255, 255, 0)));
        g.fill(new java.awt.geom.Rectangle2D.Double(ox, oy, pw, ph * 0.45));
        g.dispose();
    }
}
