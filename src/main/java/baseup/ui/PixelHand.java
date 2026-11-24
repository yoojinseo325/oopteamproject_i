package baseup.ui;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * 3D 손 뼈대를 도트로 그리는 도구.
 *
 * 손 좌표계 (오른손, 손등이 나를 보고 손가락이 위):
 *   x: 새끼 쪽 +, 엄지 쪽 -   y: 손가락 방향 +   z: 손등 쪽 +, 손바닥 쪽 -
 *
 * 비율 (손 전체 길이 100):
 *   - 손목 ~ 중지 너클 50 = 너클 ~ 중지 끝 50
 *   - 너클은 아치 모양 (중지가 가장 높음), 손가락 마디는 끝으로 갈수록 약 0.7배
 *   - 약지 ≈ 검지, 새끼가 가장 짧다
 *   - 엄지는 손목 가까이에서 시작, 손허리뼈까지 3마디처럼 움직이고 더 굵다
 */
public class PixelHand {

    public static final class Pose {
        public double[] spread = {-0.10, 0.0, 0.08, 0.18};       // 검지~새끼 벌림
        public double[][] flex = new double[4][3];                // 각 손가락 [너클, 가운데 마디, 끝 마디] 굽힘
        public double thumbOpp = 0.1;                             // 0: 옆으로 벌림, 1: 손바닥 앞으로 모음
        public double thumbAbd = 0;
        public boolean forearm = true;                            // false면 손목까지만 (팔은 몸 쪽에서 그릴 때)                               // 1: 손가락과 직각으로 활짝 (엄지 척)
        public double[] thumbFlex = {0.1, 0.1, 0.1};

        public Pose curl(int finger, double mcpDeg, double pipDeg, double dipDeg) {
            flex[finger] = new double[]{Math.toRadians(mcpDeg), Math.toRadians(pipDeg), Math.toRadians(dipDeg)};
            return this;
        }

        public Pose abd(double v) { thumbAbd = v; return this; }

        public Pose thumb(double opp, double a, double b, double c) {
            thumbOpp = opp;
            thumbFlex = new double[]{Math.toRadians(a), Math.toRadians(b), Math.toRadians(c)};
            return this;
        }
    }

    /** 손 안에 쥔 공 (손 좌표계) */
    public static final class HeldBall {
        public double x, y, z, r, spin, tilt;

        public HeldBall(double x, double y, double z, double r, double spin, double tilt) {
            this.x = x; this.y = y; this.z = z; this.r = r; this.spin = spin; this.tilt = tilt;
        }
    }

    // 너클 위치 (아치), 마디 길이, 마디 굵기
    static final double[][] KNUCKLE = {{-15, 48, 1.5}, {-5, 50, 2.5}, {5, 48.5, 2}, {14, 44.5, 0.8}};
    static final double[][] LEN = {{22, 15, 10}, {24, 16, 11}, {23, 15, 10}, {17, 11, 9}};
    static final double[][] WID = {{10.5, 9.5, 9, 8}, {11, 10, 9.5, 8.5}, {10.5, 9.5, 9, 8}, {9, 8.5, 8, 7}};
    static final double[] THUMB_BASE = {-13, 9, -3};
    static final double[] THUMB_LEN = {20, 15, 12};
    static final double[] THUMB_WID = {15, 13, 12, 10.5};

    static final Color SKIN = new Color(0xf1c27d), SKIN_S = new Color(0xd49a5a), SKIN_D = new Color(0xa86b3c),
            SKIN_L = new Color(0xf8d9a8), LINE = new Color(0x7a4a28);

    private interface El { double depth(); void draw(Graphics2D g); }

    /**
     * 손 하나를 그린다.
     * yaw: 세로축 회전(π면 손바닥이 보임), pitch: 앞뒤로 기울임, roll: 화면에서 돌림
     */
    public static void draw(BufferedImage img, Graphics2D g, Pose p, double cx, double cy, double sc,
                            double yaw, double pitch, double roll, Color sleeve, HeldBall ball) {
        double[][] R = rot(yaw, pitch, roll);
        List<El> els = new ArrayList<>();

        // 손목·팔뚝
        double[] w0 = proj(R, 0, 4, -1, cx, cy, sc), w1 = proj(R, 0, -12, -1, cx, cy, sc), w2 = proj(R, 0, -40, -1, cx, cy, sc);
        els.add(el(-1e9, gg -> {                                   // 손목은 손바닥보다 좁고, 팔뚝으로 가며 굵어진다
            if (p.forearm)
                capsule(gg, w1, w2, 25 * sc, 29 * sc, sleeve != null ? sleeve : SKIN, sleeve != null ? sleeve.darker() : SKIN_S);
            capsule(gg, w0, w1, 21 * sc, p.forearm ? 23 * sc : 20 * sc, SKIN, SKIN_S);   // 손목은 손바닥·팔뚝보다 가늘다
        }));

        // 손바닥: 손등(z+3.5)과 손바닥면(z-5) 점들의 볼록 껍질 → 두께가 있는 판
        // 손목 쪽은 좁고 너클 쪽이 넓은 쐐기. 엄지 뿌리 근육(엄지두덩)이 손목 쪽 엄지편을 부풀린다
        double[][] palmLocal = {{-11, 0}, {11, 0}, {-19, 14}, {-20, 28}, {17, 18}, {18, 36},
                {-19, 45}, {-5, 51}, {5, 49.5}, {17, 44}};
        List<double[]> pts = new ArrayList<>();
        double pz = 0;
        for (double[] q : palmLocal)
            for (double z : new double[]{3.5, -5}) {
                double[] s = proj(R, q[0], q[1], z, cx, cy, sc);
                pts.add(s);
                pz += s[2];
            }
        final double palmDepth = pz / pts.size();
        final double[][] hull = hull(pts);
        final double backFacing = R[2][2];                        // 손등 법선이 화면 쪽을 얼마나 보나
        els.add(el(palmDepth, gg -> {
            fillPoly(gg, LINE, grow(hull, 1.0));
            fillPoly(gg, SKIN_S, hull);
            fillPoly(gg, SKIN, shiftPoly(shrink(hull, 0.84), -1, -1));
            if (backFacing > 0.25) {                              // 손등: 너클 뼈, 힘줄
                for (int i = 0; i < 3; i++) {
                    double[] a = proj(R, KNUCKLE[i][0], KNUCKLE[i][1] - 2, 4, cx, cy, sc);
                    oval(gg, SKIN_L, a[0] - 3 * sc, a[1] - 2 * sc, 6 * sc, 4 * sc);
                }
            } else if (backFacing < -0.25) {                      // 손바닥: 손금 두 줄
                double[] a = proj(R, -16, 34, -5, cx, cy, sc), b = proj(R, 0, 33, -5, cx, cy, sc), c2 = proj(R, 16, 38, -5, cx, cy, sc);
                curve(gg, SKIN_S, 1, a[0], a[1], b[0], b[1], c2[0], c2[1]);
                double[] d = proj(R, -14, 40, -5, cx, cy, sc), e = proj(R, -8, 20, -5, cx, cy, sc), f = proj(R, -10, 4, -5, cx, cy, sc);
                curve(gg, SKIN_S, 1, d[0], d[1], e[0], e[1], f[0], f[1]);
            }
        }));

        // 네 손가락: 너클에서 시작해 손바닥 쪽(-z)으로 굽는다
        for (int i = 0; i < 4; i++) {
            double s = p.spread[i];
            double[] d = {Math.sin(s), Math.cos(s), 0};
            double[] cur = {KNUCKLE[i][0], KNUCKLE[i][1], KNUCKLE[i][2]};
            double T = 0;
            for (int j = 0; j < 3; j++) {
                T += p.flex[i][j];
                double[] dir = {d[0] * Math.cos(T), d[1] * Math.cos(T), -Math.sin(T)};
                double[] nxt = {cur[0] + dir[0] * LEN[i][j], cur[1] + dir[1] * LEN[i][j], cur[2] + dir[2] * LEN[i][j]};
                addSeg(els, R, cur, nxt, WID[i][j], WID[i][j + 1], cx, cy, sc, j > 0);
                cur = nxt;
            }
        }

        // 엄지: 벌린 방향 ↔ 손바닥 앞으로 모은 방향 사이, 손바닥·검지 쪽으로 굽는다
        double o = p.thumbOpp;
        double[] dir0 = norm(lerp3(new double[]{-0.62, 0.74, -0.25}, new double[]{0.30, 0.62, -0.72}, o));
        dir0 = norm(lerp3(dir0, new double[]{-0.96, 0.25, 0.1}, p.thumbAbd));
        double[] bend = norm(lerp3(new double[]{0.75, 0.0, -0.66}, new double[]{0.85, -0.25, 0.45}, o));
        double dot = dir0[0] * bend[0] + dir0[1] * bend[1] + dir0[2] * bend[2];
        bend = norm(new double[]{bend[0] - dir0[0] * dot, bend[1] - dir0[1] * dot, bend[2] - dir0[2] * dot});
        double[] cur = THUMB_BASE.clone();
        double T = 0;
        for (int j = 0; j < 3; j++) {
            T += p.thumbFlex[j];
            double[] dir = {dir0[0] * Math.cos(T) + bend[0] * Math.sin(T), dir0[1] * Math.cos(T) + bend[1] * Math.sin(T),
                    dir0[2] * Math.cos(T) + bend[2] * Math.sin(T)};
            double[] nxt = {cur[0] + dir[0] * THUMB_LEN[j], cur[1] + dir[1] * THUMB_LEN[j], cur[2] + dir[2] * THUMB_LEN[j]};
            addSeg(els, R, cur, nxt, THUMB_WID[j], THUMB_WID[j + 1], cx, cy, sc, j == 2);
            cur = nxt;
        }

        // 쥔 공
        if (ball != null) {
            double[] b = proj(R, ball.x, ball.y, ball.z, cx, cy, sc);
            els.add(el(b[2], gg -> IntroAnimation.ball(img, b[0], b[1], ball.r * sc, ball.spin, ball.tilt)));
        }

        els.sort(Comparator.comparingDouble(El::depth));
        for (El e : els) e.draw(g);
    }

    private static void addSeg(List<El> els, double[][] R, double[] a, double[] b, double wa, double wb,
                               double cx, double cy, double sc, boolean crease) {
        double[] pa = proj(R, a[0], a[1], a[2], cx, cy, sc), pb = proj(R, b[0], b[1], b[2], cx, cy, sc);
        els.add(el((pa[2] + pb[2]) / 2 + 0.5, gg -> {
            capsule(gg, pa, pb, wa * sc, wb * sc, SKIN, SKIN_S);
            if (crease) {                                          // 마디 주름
                double dx = pb[0] - pa[0], dy = pb[1] - pa[1], l = Math.hypot(dx, dy);
                if (l > 2) {
                    double nx = -dy / l, ny = dx / l, hw = wa * sc * 0.28;
                    line(gg, SKIN_S, 1, pa[0] + dx / l * 1.5 - nx * hw, pa[1] + dy / l * 1.5 - ny * hw,
                            pa[0] + dx / l * 1.5 + nx * hw, pa[1] + dy / l * 1.5 + ny * hw);
                }
            }
        }));
    }

    /** 테두리 → 그늘 → 빛 받는 면 순서로 굵기가 변하는 원기둥 */
    public static void capsule(Graphics2D g, double[] a, double[] b, double wa, double wb, Color c, Color shade) {
        IntroAnimation.limb(g, a[0], a[1], b[0], b[1], wa + 2, wb + 2, LINE);
        IntroAnimation.limb(g, a[0], a[1], b[0], b[1], wa, wb, shade);
        double o = Math.min(wa, wb) * 0.14;
        IntroAnimation.limb(g, a[0] - o, a[1] - o, b[0] - o, b[1] - o, wa * 0.7, wb * 0.7, c);
    }

    // ---- 수학 도구 ----

    static double[][] rot(double yaw, double pitch, double roll) {
        double cy = Math.cos(yaw), sy = Math.sin(yaw), cp = Math.cos(pitch), sp = Math.sin(pitch), cr = Math.cos(roll), sr = Math.sin(roll);
        double[][] Ry = {{cy, 0, sy}, {0, 1, 0}, {-sy, 0, cy}};
        double[][] Rx = {{1, 0, 0}, {0, cp, -sp}, {0, sp, cp}};
        double[][] Rz = {{cr, -sr, 0}, {sr, cr, 0}, {0, 0, 1}};
        return mul(Rz, mul(Rx, Ry));
    }

    static double[][] mul(double[][] a, double[][] b) {
        double[][] r = new double[3][3];
        for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) for (int k = 0; k < 3; k++) r[i][j] += a[i][k] * b[k][j];
        return r;
    }

    /** 손 좌표 → 화면 (x, y, 깊이: 클수록 가까움) */
    static double[] proj(double[][] R, double x, double y, double z, double cx, double cy, double sc) {
        double X = R[0][0] * x + R[0][1] * y + R[0][2] * z, Y = R[1][0] * x + R[1][1] * y + R[1][2] * z, Z = R[2][0] * x + R[2][1] * y + R[2][2] * z;
        return new double[]{cx + X * sc, cy - Y * sc, Z};
    }

    static double[] lerp3(double[] a, double[] b, double f) {
        return new double[]{a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f, a[2] + (b[2] - a[2]) * f};
    }

    static double[] norm(double[] v) {
        double l = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return new double[]{v[0] / l, v[1] / l, v[2] / l};
    }

    static double[][] hull(List<double[]> pts) {
        List<double[]> p = new ArrayList<>(pts);
        p.sort((a, b) -> a[0] != b[0] ? Double.compare(a[0], b[0]) : Double.compare(a[1], b[1]));
        double[][] h = new double[p.size() * 2][];
        int k = 0;
        for (double[] q : p) {
            while (k >= 2 && cross(h[k - 2], h[k - 1], q) <= 0) k--;
            h[k++] = q;
        }
        for (int i = p.size() - 2, t = k + 1; i >= 0; i--) {
            double[] q = p.get(i);
            while (k >= t && cross(h[k - 2], h[k - 1], q) <= 0) k--;
            h[k++] = q;
        }
        return Arrays.copyOf(h, k - 1);
    }

    static double cross(double[] o, double[] a, double[] b) {
        return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0]);
    }

    static double[][] shrink(double[][] h, double f) {
        double mx = 0, my = 0;
        for (double[] q : h) { mx += q[0]; my += q[1]; }
        mx /= h.length; my /= h.length;
        double[][] r = new double[h.length][];
        for (int i = 0; i < h.length; i++) r[i] = new double[]{mx + (h[i][0] - mx) * f, my + (h[i][1] - my) * f};
        return r;
    }

    static double[][] grow(double[][] h, double px) {
        double mx = 0, my = 0;
        for (double[] q : h) { mx += q[0]; my += q[1]; }
        mx /= h.length; my /= h.length;
        double[][] r = new double[h.length][];
        for (int i = 0; i < h.length; i++) {
            double dx = h[i][0] - mx, dy = h[i][1] - my, l = Math.max(1e-6, Math.hypot(dx, dy));
            r[i] = new double[]{h[i][0] + dx / l * px, h[i][1] + dy / l * px};
        }
        return r;
    }

    static double[][] shiftPoly(double[][] h, double dx, double dy) {
        double[][] r = new double[h.length][];
        for (int i = 0; i < h.length; i++) r[i] = new double[]{h[i][0] + dx, h[i][1] + dy};
        return r;
    }

    static void fillPoly(Graphics2D g, Color c, double[][] h) {
        Path2D.Double s = new Path2D.Double();
        s.moveTo(h[0][0], h[0][1]);
        for (int i = 1; i < h.length; i++) s.lineTo(h[i][0], h[i][1]);
        s.closePath();
        g.setColor(c);
        g.fill(s);
    }

    static void line(Graphics2D g, Color c, float w, double x1, double y1, double x2, double y2) {
        IntroAnimation.line(g, c, w, x1, y1, x2, y2);
    }

    static void oval(Graphics2D g, Color c, double x, double y, double w, double h) {
        IntroAnimation.oval(g, c, x, y, w, h);
    }

    static void curve(Graphics2D g, Color c, float w, double x1, double y1, double qx, double qy, double x2, double y2) {
        IntroAnimation.curve(g, c, w, x1, y1, qx, qy, x2, y2);
    }

    private static El el(double depth, java.util.function.Consumer<Graphics2D> d) {
        return new El() {
            public double depth() { return depth; }
            public void draw(Graphics2D g) { d.accept(g); }
        };
    }
}
