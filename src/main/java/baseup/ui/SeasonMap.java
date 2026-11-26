package baseup.ui;

import baseup.model.LearningStep;
import baseup.service.LearningService;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * 퀴즈 화면의 "시즌 지도". 단계(STEP)를 구불구불한 길 위의 베이스로 그린다.
 * 한 줄(4단계)이 한 이닝이고, 한 줄을 모두 끝내면 깃발에 "득점!"이 뜬다. 마지막 단계 뒤에는 우승 트로피.
 * 베이스는 진짜 버튼(season-step-번호)이라 키보드·테스트로도 누를 수 있다. 잠긴 단계는 누를 수 없다.
 */
final class SeasonMap extends JPanel {
    private static final int PER_ROW = 4, ROW_H = 190, TOP = 56, SIDE = 120, NODE = 46;
    private final List<LearningStep> steps;
    private final LearningService service;
    private final Color accent;
    private final List<JButton> nodes = new ArrayList<>();

    SeasonMap(List<LearningStep> steps, LearningService service, Color accent, IntConsumer start) {
        super(null);
        this.steps = List.copyOf(steps); this.service = service; this.accent = accent;
        setOpaque(false);
        for (LearningStep s : this.steps) {
            JButton b = new Base(s.number());
            b.setName("season-step-" + s.number());
            b.setToolTipText("STEP " + s.number() + (unlocked(s) ? " · " + s.goal() : " · 앞 단계를 마치면 열려요"));
            b.setEnabled(unlocked(s));
            b.addActionListener(e -> start.accept(s.number()));
            nodes.add(b); add(b);
        }
        setPreferredSize(new Dimension(900, TOP + rows() * ROW_H + 30));
    }

    private int rows() { return Math.max(1, (steps.size() + PER_ROW - 1) / PER_ROW); }
    private boolean done(LearningStep s) { return service.stepCompleted(s.number()); }
    private boolean unlocked(LearningStep s) { return service.stepUnlocked(s.number()); }
    private boolean rowDone(int r) {
        for (int i = r * PER_ROW; i < Math.min(steps.size(), (r + 1) * PER_ROW); i++) if (!done(steps.get(i))) return false;
        return true;
    }

    /** i번째 단계 베이스의 가운데. 짝수 줄은 왼→오, 홀수 줄은 오→왼. */
    private Point center(int i) {
        int r = i / PER_ROW, c = i % PER_ROW; double colW = (getWidth() - SIDE * 2) / (double) PER_ROW;
        int col = r % 2 == 0 ? c : PER_ROW - 1 - c;
        return new Point((int) (SIDE + colW * col + colW / 2), TOP + r * ROW_H + 26);
    }
    private int rowEdge(int r, boolean end) {   // 그 줄 길이 시작/끝(꺾이는 곳) x
        double colW = (getWidth() - SIDE * 2) / (double) PER_ROW;
        boolean right = (r % 2 == 0) == end;
        return (int) (right ? SIDE + colW * (PER_ROW - .5) : SIDE + colW * .5);
    }

    @Override public void doLayout() {
        for (int i = 0; i < nodes.size(); i++) { Point p = center(i); nodes.get(i).setBounds(p.x - NODE / 2, p.y - NODE / 2, NODE, NODE); }
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = Ballpark.smooth(g0); int w = getWidth();
        // 줄마다 옅은 띠
        for (int r = 0; r < rows(); r++) if (r % 2 == 1) { g.setColor(new Color(255, 255, 255, 120)); g.fill(new Rectangle2D.Double(0, TOP + r * ROW_H - 50, w, ROW_H)); }
        // 길: 줄마다 가로선, 줄 끝에서 반원으로 꺾인다
        Path2D road = new Path2D.Double(); Point first = center(0); road.moveTo(rowEdge(0, false) - 30, first.y);
        double radius = ROW_H / 2.0, k = radius * 4 / 3;
        for (int r = 0; r < rows(); r++) {
            int y = TOP + r * ROW_H + 26; boolean last = r == rows() - 1;
            int endX = last ? trophyPoint().x : rowEdge(r, true);
            road.lineTo(endX, y);
            if (!last) { boolean right = r % 2 == 0; double dir = right ? 1 : -1;
                road.curveTo(endX + dir * k, y, endX + dir * k, y + ROW_H, endX, y + ROW_H); }
        }
        g.setStroke(new BasicStroke(38, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.setColor(new Color(0xEAD5AE)); g.draw(road);
        g.setStroke(new BasicStroke(26, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.setColor(Ballpark.mix(accent, Color.WHITE, .12)); g.draw(road);
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{4, 9}, 0)); g.setColor(new Color(255, 255, 255, 230)); g.draw(road);
        // 이닝 깃발: 꺾이는 곳 바깥쪽
        for (int r = 0; r < rows() - 1; r++) {
            boolean right = r % 2 == 0; int x = rowEdge(r, true) + (right ? (int) radius + 34 : -(int) radius - 80);
            flag(g, x, TOP + r * ROW_H + ROW_H / 2 + 20, (r + 1) + "회", rowDone(r));
        }
        trophy(g, trophyPoint(), steps.stream().allMatch(this::done));
        // 단계 이름
        for (int i = 0; i < steps.size(); i++) {
            LearningStep s = steps.get(i); Point p = center(i); boolean d = done(s), u = unlocked(s);
            String state = d ? "완료" : u ? "도전!" : "잠김";
            g.setFont(Theme.font(12, true)); FontMetrics fm = g.getFontMetrics();
            String head = "STEP " + s.number() + "   " + state;
            g.setColor(d ? accent.darker() : u ? BookParts.ORANGE_TEXT : BookParts.LIGHT_TEXT);
            g.drawString(head, p.x - fm.stringWidth(head) / 2, p.y + 46);
            g.setFont(Theme.font(14, true)); fm = g.getFontMetrics();
            String goal = u ? s.goal() : "앞 단계를 마치면 열려요";
            int colW = (int) ((getWidth() - SIDE * 2) / (double) PER_ROW) - 14, y = p.y + 66;
            g.setColor(u ? new Color(0x1F2937) : new Color(0x9AA3B2));
            for (String line : BookParts.ClampText.wrap(goal, fm, colW, 2)) { g.drawString(line, p.x - fm.stringWidth(line) / 2, y); y += fm.getHeight(); }
        }
        g.dispose();
    }

    /** 트로피 자리: 마지막 줄에 자리가 남으면 그 다음 칸, 꽉 찼으면 꺾이는 곳 바깥. */
    private Point trophyPoint() {
        int lastRow = rows() - 1, inLast = steps.size() - lastRow * PER_ROW;
        if (inLast < PER_ROW) { Point p = center(Math.min(steps.size(), lastRow * PER_ROW + inLast)); return p; }
        boolean right = lastRow % 2 == 0; Point p = center(steps.size() - 1);
        return new Point(p.x + (right ? 1 : -1) * 90, p.y);
    }

    private static void flag(Graphics2D g, int x, int y, String text, boolean scored) {
        Color green = scored ? new Color(0x22A659) : new Color(0xB8C0CC);
        g.setColor(new Color(0, 0, 0, 30)); g.fill(new Ellipse2D.Double(x - 12, y + 30, 24, 7));
        g.setColor(new Color(0x9AA3AE)); g.setStroke(new BasicStroke(3)); g.draw(new Line2D.Double(x, y - 44, x, y + 33));
        Path2D cloth = new Path2D.Double(); cloth.moveTo(x, y - 44); cloth.lineTo(x + 46, y - 38); cloth.lineTo(x + 40, y - 26); cloth.lineTo(x + 46, y - 14); cloth.lineTo(x, y - 16); cloth.closePath();
        g.setColor(green); g.fill(cloth);
        g.setColor(Color.WHITE); g.setFont(Theme.font(12, true)); g.drawString(text, x + 8, y - 25);
        if (scored) { g.setColor(BookParts.ORANGE_TEXT); g.setFont(Theme.font(13, true)); g.drawString("득점!", x - 16, y + 54); }
    }

    private static void trophy(Graphics2D g, Point p, boolean won) {
        int x = p.x, y = p.y - 6;
        if (won) { g.setPaint(new RadialGradientPaint(new Point2D.Double(x, y), 56, new float[]{0f, 1f}, new Color[]{new Color(255, 214, 80, 150), new Color(255, 214, 80, 0)}));
            g.fill(new Ellipse2D.Double(x - 56, y - 56, 112, 112)); }
        Color gold = won ? new Color(0xF5B700) : new Color(0xC9CED6), dark = won ? new Color(0xC98A00) : new Color(0xA7AFBB);
        g.setColor(gold); g.setStroke(new BasicStroke(5));
        g.draw(new Arc2D.Double(x - 30, y - 24, 18, 22, 90, 180, Arc2D.OPEN)); g.draw(new Arc2D.Double(x + 12, y - 24, 18, 22, 270, 180, Arc2D.OPEN));
        Path2D cup = new Path2D.Double(); cup.moveTo(x - 20, y - 28); cup.lineTo(x + 20, y - 28); cup.curveTo(x + 20, y, x + 8, y + 8, x, y + 8); cup.curveTo(x - 8, y + 8, x - 20, y, x - 20, y - 28); cup.closePath();
        g.fill(cup); g.setColor(dark); g.fill(new Rectangle2D.Double(x - 4, y + 8, 8, 12)); g.fill(new RoundRectangle2D.Double(x - 16, y + 18, 32, 8, 4, 4));
        g.setFont(Theme.font(13, true)); FontMetrics fm = g.getFontMetrics(); String t = won ? "시즌 우승!" : "우승 트로피";
        g.setColor(won ? BookParts.ORANGE_TEXT : BookParts.LIGHT_TEXT); g.drawString(t, x - fm.stringWidth(t) / 2, y + 46);
    }

    /** 베이스 버튼: 완료=초록 다이아몬드+체크, 도전 가능=노란 다이아몬드(빛남), 잠김=회색. */
    private final class Base extends JButton {
        private final int number;
        Base(int number) {
            this.number = number; setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false); setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); setRolloverEnabled(true);
            getAccessibleContext().setAccessibleName("STEP " + number);
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = Ballpark.smooth(g0); int s = Math.min(getWidth(), getHeight());
            boolean d = service.stepCompleted(number), u = isEnabled();
            double c = s / 2.0, r = s / 2.0 - 3 - (getModel().isRollover() && u ? 0 : 2);
            Path2D dia = new Path2D.Double(); dia.moveTo(c, c - r); dia.lineTo(c + r, c); dia.lineTo(c, c + r); dia.lineTo(c - r, c); dia.closePath();
            Color fill = d ? new Color(0x2EAA55) : u ? BookParts.YELLOW : new Color(0xC6CDD7);
            g.setColor(new Color(0, 0, 0, 40)); g.fill(AffineTransform.getTranslateInstance(0, 3).createTransformedShape(dia));
            g.setColor(fill); g.fill(dia); g.setColor(Color.WHITE); g.setStroke(new BasicStroke(3f)); g.draw(dia);
            g.setStroke(new BasicStroke(3.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (d) { Path2D check = new Path2D.Double(); check.moveTo(c - 8, c); check.lineTo(c - 2, c + 6); check.lineTo(c + 9, c - 7); g.draw(check); }
            else if (u) { g.setColor(new Color(0x6B4A00)); Path2D play = new Path2D.Double(); play.moveTo(c - 4, c - 7); play.lineTo(c + 7, c); play.lineTo(c - 4, c + 7); play.closePath(); g.fill(play); }
            else { g.fill(new RoundRectangle2D.Double(c - 6, c - 2, 12, 9, 3, 3)); g.draw(new Arc2D.Double(c - 4, c - 8, 8, 10, 0, 180, Arc2D.OPEN)); }
            g.dispose();
        }
    }
}
