package baseup.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * 프로그램을 켰을 때 나오는 도트 인트로.
 * 320x180 작은 그림에 그린 뒤 화면 크기에 맞춰 픽셀 그대로 키운다.
 *
 * 장면 순서: 공 클로즈업 → 공을 쥔 손 → 투수 얼굴 → 투구(옆모습, 슬로모션)
 *          → 날아오는 공 → 타격 → 폴대 쪽으로 날아가는 공 → 판정하려는 심판 → 타이틀
 *
 * 사용법: new IntroAnimation(() -> 끝나면 할 일).start();
 * 클릭이나 아무 키를 누르면 바로 건너뛴다.
 */
public class IntroAnimation extends JComponent {

    public static final int W = 320, H = 180;

    // 장면 시작 시간(초)
    static final double S_GRIP = 1.3, S_FACE = 2.5, S_PITCH = 3.7, S_IN = 6.6,
            S_HIT = 7.7, S_FLY = 9.5, S_UMP = 12.4, S_TITLE = 14.0;
    public static final double LENGTH = 16.8;
    static final double HIT_REAL = 0.76;   // 타격 장면에서 공이 맞는 실제 시간

    private final Runnable onFinish;
    private final javax.swing.Timer timer;
    private long startNanos;
    private boolean finished;
    private BufferedImage frame;
    private long fadeStart = -1;                 // 끝날 때 인트로가 투명해지며 메인 화면이 드러난다
    static final double FADE = 0.5;

    public IntroAnimation(Runnable onFinish) {
        this.onFinish = onFinish;
        setOpaque(false);
        setFocusable(true);
        timer = new javax.swing.Timer(33, e -> tick());
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { finish(); }
        });
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { finish(); }
        });
    }

    public void start() {
        startNanos = System.nanoTime();
        finished = false;
        frame = render(0);
        timer.start();
        requestFocusInWindow();
    }

    private void tick() {
        if (fadeStart >= 0) {                                    // 사라지는 중
            if ((System.nanoTime() - fadeStart) / 1e9 >= FADE) {
                timer.stop();
                if (onFinish != null) onFinish.run();
            }
            repaint();
            return;
        }
        double t = (System.nanoTime() - startNanos) / 1e9;
        if (t >= LENGTH) { finish(); return; }
        frame = render(t);
        repaint();
    }

    /** 끝나거나 건너뛰면 바로 닫지 않고 FADE 초 동안 투명해지며 메인 화면을 보여 준다 */
    private void finish() {
        if (finished) return;
        finished = true;
        fadeStart = System.nanoTime();
        if (!timer.isRunning()) timer.start();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        if (fadeStart >= 0) {
            float a = (float) Math.max(0, 1 - (System.nanoTime() - fadeStart) / 1e9 / FADE);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
        }
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        if (frame == null) return;
        double s = Math.min(getWidth() / (double) W, getHeight() / (double) H);
        int w = (int) (W * s), h = (int) (H * s);
        int x = (getWidth() - w) / 2, y = (getHeight() - h) / 2;
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(frame, x, y, w, h, null);

        // 건너뛰기 안내는 글자가 깨지지 않게 큰 화면 기준으로 따로 쓴다
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(kfont(Font.PLAIN, Math.max(12, (int) (s * 3.2))));
        g.setColor(new Color(255, 255, 255, 130));
        String hint = "클릭하면 건너뛰기 ▶";
        int tw = g.getFontMetrics().stringWidth(hint);
        g.drawString(hint, x + w - tw - (int) (s * 6), y + h - (int) (s * 3));
    }

    // ------------------------------------------------------------------
    // 한 프레임 그리기
    // ------------------------------------------------------------------

    public static BufferedImage render(double t) {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = pg(img);

        if (t < S_GRIP) sceneBall(img, g, t);
        else if (t < S_FACE) sceneGrip(img, g, t - S_GRIP);
        else if (t < S_PITCH) sceneFace(img, g, t - S_FACE);
        else if (t < S_IN) sceneThrow(img, g, t - S_PITCH);
        else if (t < S_HIT) sceneIncoming(img, g, t - S_IN);
        else if (t < S_FLY) sceneHit(img, g, t - S_HIT);
        else if (t < S_UMP) sceneFly(img, g, t - S_FLY);
        else if (t < S_TITLE) sceneUmpire(img, g, t - S_UMP);
        else sceneTitle(img, g, t - S_TITLE);

        // 화면 흔들림
        double shake = 0;
        double hit = S_HIT + HIT_REAL;
        if (t >= hit && t < hit + 0.9) shake = 3 * (1 - seg(t, hit, hit + 0.9));
        else if (t >= S_IN + 0.85 && t < S_HIT) shake = 1.2;
        if (shake > 0) {
            BufferedImage copy = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
            Graphics cg = copy.getGraphics();
            cg.drawImage(img, 0, 0, null);
            cg.dispose();
            int f = (int) (t * 30);
            int dx = (int) Math.round(((f * 7) % 3 - 1) * shake);
            int dy = (int) Math.round(((f * 5) % 3 - 1) * shake);
            g.drawImage(copy, dx, dy, null);
        }

        // 영화처럼 위아래 검은 띠
        if (t < S_TITLE) {
            rect(g, Color.BLACK, 0, 0, W, 10);
            rect(g, Color.BLACK, 0, H - 10, W, 10);
        }

        // 페이드와 섬광
        if (t < 0.5) cover(g, 0x000000, 1 - t / 0.5);
        if (t >= hit + 0.05 && t < hit + 0.2) cover(g, 0xffffff, 0.55 * (1 - seg(t, hit + 0.05, hit + 0.2)));
        if (t >= S_UMP + 1.3 && t < S_TITLE) cover(g, 0xffffff, seg(t, S_UMP + 1.3, S_TITLE));
        if (t >= S_TITLE && t < S_TITLE + 0.35) cover(g, 0xffffff, 1 - seg(t, S_TITLE, S_TITLE + 0.35));

        g.dispose();
        return img;
    }

    // ------------------------------------------------------------------
    // 1. 공 클로즈업
    // ------------------------------------------------------------------

    static void sceneBall(BufferedImage img, Graphics2D g, double u) {
        vgrad(img, 0, H, 0x05070f, 0x0c1230, 0x161f48);
        bokeh(g, 11, u, 9);
        double r = 44 + 7 * easeOut(seg(u, 0, S_GRIP));
        ball(img, 160, 90, r, 0.6 + u * 1.2, 0.35);
        if (u > 0.6 && u < 0.95) sparkle(img, 138, 64, (int) (7 * Math.sin(seg(u, 0.6, 0.95) * Math.PI)));
    }

    // ------------------------------------------------------------------
    // 2. 공을 쥔 손 (포심 그립) - 손가락을 마디 단위로 그린다
    // ------------------------------------------------------------------

    static void sceneGrip(BufferedImage img, Graphics2D g, double u) {
        vgrad(img, 0, H, 0x0a0f26, 0x121a3f, 0x1d2a5c);
        bokeh(g, 23, u, 10);

        // 3D 손 뼈대(PixelHand)로 그리는 그립. 엄지 쪽에서 본다 (공은 왼쪽, 손목은 오른쪽 아래)
        //  0.0~0.55  펴져 있던 손가락이 공을 감싸며 쥔다 (검지·중지가 공 위로, 엄지가 아래로)
        //  0.55~0.95 꽉 쥐며 살짝 힘이 들어간다
        //  0.95~1.2  던지려고 손을 뒤로 뺀다
        double close = easeOut(seg(u, 0.0, 0.55));
        double squeeze = Math.sin(seg(u, 0.55, 0.95) * Math.PI) * 0.6;
        PixelHand.Pose open = new PixelHand.Pose()
                .curl(0, 8, 15, 8).curl(1, 8, 18, 8).curl(2, 15, 25, 12).curl(3, 22, 30, 15).thumb(0.45, 5, 5, 5);
        PixelHand.Pose grip = new PixelHand.Pose()
                .curl(0, 30, 65, 30).curl(1, 28, 65, 30).curl(2, 55, 80, 40).curl(3, 70, 90, 45).thumb(0.8, 30, 20, 10);
        open.spread = new double[]{-0.16, 0.02, 0.14, 0.26};
        grip.spread = new double[]{-0.12, 0.04, 0.12, 0.2};
        PixelHand.Pose pose = blend(open, grip, close);
        for (int i = 0; i < 4; i++) pose.flex[i][2] += Math.toRadians(4 * squeeze);
        pose.thumbFlex[0] += Math.toRadians(3 * squeeze);

        double pull = easeIn(seg(u, 0.95, 1.2));
        double sc = 2.0 - 0.08 * easeInOut(seg(u, 0, 0.95)) - 0.5 * pull;
        double cx = 208 + 110 * pull, cy = 172 + 10 * pull;
        double roll = 0.35 + 0.25 * pull;

        BufferedImage L = layer();
        Graphics2D h = pg(L);
        PixelHand.HeldBall ball = new PixelHand.HeldBall(-3, 44, -22, 19, -0.4 + 0.4 * close, 0.0);
        PixelHand.draw(L, h, pose, cx, cy, sc, 1.35, 0, roll, RED, ball);
        h.dispose();
        shadePass(L, -1, -1, 0xffe6b8, 0.45, 0.0);
        g.drawImage(L, 0, 0, null);
    }

    static PixelHand.Pose blend(PixelHand.Pose a, PixelHand.Pose b, double f) {
        PixelHand.Pose r = new PixelHand.Pose();
        for (int i = 0; i < 4; i++) {
            r.spread[i] = lerp(a.spread[i], b.spread[i], f);
            for (int j = 0; j < 3; j++) r.flex[i][j] = lerp(a.flex[i][j], b.flex[i][j], f);
        }
        r.thumbOpp = lerp(a.thumbOpp, b.thumbOpp, f);
        r.thumbAbd = lerp(a.thumbAbd, b.thumbAbd, f);
        for (int j = 0; j < 3; j++) r.thumbFlex[j] = lerp(a.thumbFlex[j], b.thumbFlex[j], f);
        return r;
    }

    /** 공 중심에서 각도(도)·반지름으로 점들을 만든다 */
    static double[] arcPts(double cx, double cy, double[] deg, double[] r) {
        double[] p = new double[deg.length * 2];
        for (int i = 0; i < deg.length; i++) {
            double a = Math.toRadians(deg[i]);
            p[i * 2] = cx + Math.cos(a) * r[i];
            p[i * 2 + 1] = cy + Math.sin(a) * r[i];
        }
        return p;
    }

    /** 손등 관절에서 시작해 공 둘레(반지름 R)를 따라 손가락 마디 점을 만든다 */
    static double[] arcFinger(double kx, double ky, double bx, double by, double R, double[] angs) {
        double[] p = new double[2 + angs.length * 2];
        p[0] = kx;
        p[1] = ky;
        for (int i = 0; i < angs.length; i++) {
            p[2 + i * 2] = bx + R * Math.cos(angs[i]);
            p[3 + i * 2] = by + R * Math.sin(angs[i]);
        }
        return p;
    }

    /** 점들을 잇는 굵기가 변하는 관절 사슬 */
    static void chain(Graphics2D g, Color c, double[] p, double[] w) {
        for (int i = 0; i + 3 < p.length; i += 2)
            limb(g, p[i], p[i + 1], p[i + 2], p[i + 3], w[i / 2], w[Math.min(w.length - 1, i / 2 + 1)], c);
    }

    static double[] shift(double[] p, double dx, double dy) {
        double[] r = p.clone();
        for (int i = 0; i < r.length; i += 2) { r[i] += dx; r[i + 1] += dy; }
        return r;
    }

    /** 손가락 마디 주름 */
    static void crease(Graphics2D g, double[] p, int j) {
        double x = p[j * 2], y = p[j * 2 + 1];
        double dx = p[j * 2 + 2] - p[j * 2 - 2], dy = p[j * 2 + 3] - p[j * 2 - 1], l = Math.hypot(dx, dy);
        double nx = -dy / l, ny = dx / l;
        line(g, SKIN_D, 1, x + nx * 1, y + ny * 1, x + nx * 4, y + ny * 4);
    }

    static void nail(Graphics2D g, double x, double y, double ang) {
        double ox = Math.cos(ang - 1.2) * 3, oy = Math.sin(ang - 1.2) * 3;
        oval(g, c(0xf7d6c0), x + ox - 3, y + oy - 2.5, 6, 5);
        rect(g, WHITE, x + ox - 1, y + oy - 1.5, 2, 1);
    }
    // ------------------------------------------------------------------
    // 3. 투수 얼굴
    // ------------------------------------------------------------------

    static void sceneFace(BufferedImage img, Graphics2D g, double u) {
        vgrad(img, 0, H, 0x070b22, 0x101a45, 0x1e2b62);
        bokeh(g, 37, u, 12);

        BufferedImage L = layer();
        Graphics2D h = pg(L);
        double sc = 1.0 + 0.10 * easeOut(seg(u, 0, 1.2));
        AffineTransform at = new AffineTransform();
        at.translate(160, 100);
        at.scale(sc, sc);
        at.translate(-160, -100);
        h.setTransform(at);

        // 유니폼 (흰 홈 유니폼, 남색 파이핑, 빨간 언더셔츠)
        poly(h, WHITE, 50, 190, 72, 150, 128, 136, 192, 136, 248, 150, 270, 190);
        poly(h, WHITE_S, 204, 140, 248, 150, 270, 190, 218, 190);
        poly(h, RED, 142, 136, 178, 136, 160, 162);
        line(h, NAVY, 2, 140, 137, 160, 165);
        line(h, NAVY, 2, 180, 137, 160, 165);
        line(h, NAVY, 2, 72, 150, 128, 137);
        line(h, NAVY, 2, 192, 137, 248, 150);
        glyphs(h, "BASEUP", 125, 167, 2, NAVY);
        // 목, 귀
        rect(h, SKIN, 146, 116, 28, 24);
        rect(h, SKIN_S, 146, 116, 28, 8);
        oval(h, SKIN, 113, 88, 12, 20);
        oval(h, SKIN_S, 116, 93, 5, 10);
        oval(h, SKIN, 195, 88, 12, 20);
        oval(h, SKIN_S, 199, 93, 5, 10);
        // 얼굴
        Area face = new Area(new Ellipse2D.Double(120, 48, 80, 80));
        face.add(new Area(polyShape(124, 96, 196, 96, 190, 124, 172, 136, 148, 136, 130, 124)));
        h.setColor(SKIN);
        h.fill(face);
        h.clip(face);
        poly(h, SKIN_S, 184, 90, 206, 90, 206, 140, 168, 140);
        rect(h, SKIN_D, 110, 72, 100, 18);          // 모자 챙 그림자
        rect(h, SKIN_S, 110, 90, 100, 3);
        h.setClip(null);
        // 눈썹, 눈 (그림자 속에서 빛나는 눈)
        line(h, c(0x2a1a10), 3, 130, 79, 147, 83);
        line(h, c(0x2a1a10), 3, 190, 79, 173, 83);
        rect(h, c(0xe8e0d0), 133, 84, 13, 5);
        rect(h, c(0x1a1010), 138, 84, 6, 5);
        rect(h, WHITE, 139, 84, 2, 2);
        rect(h, c(0xe8e0d0), 174, 84, 13, 5);
        rect(h, c(0x1a1010), 176, 84, 6, 5);
        rect(h, WHITE, 177, 84, 2, 2);
        // 코, 입
        line(h, SKIN_S, 2, 161, 93, 158, 106);
        rect(h, SKIN_D, 153, 106, 13, 3);
        rect(h, c(0x7a3b2a), 149, 117, 22, 2);
        rect(h, SKIN_S, 152, 121, 16, 2);
        // 모자 (남색, 흰 B 로고)
        h.setColor(NAVY);
        h.fill(new Arc2D.Double(112, 22, 96, 78, 0, 180, Arc2D.CHORD));
        oval(h, NAVY_L, 128, 28, 22, 9);
        line(h, NAVY_D, 1, 160, 23, 132, 60);
        line(h, NAVY_D, 1, 160, 23, 188, 60);
        oval(h, NAVY_L, 155, 19, 10, 6);
        glyphs(h, "B", 154, 37, 3, RED);
        glyphs(h, "B", 153, 36, 3, WHITE);
        poly(h, NAVY, 104, 60, 216, 60, 224, 67, 204, 76, 116, 76, 96, 67);
        poly(h, NAVY_D, 108, 72, 212, 72, 204, 76, 116, 76);
        rect(h, NAVY_L, 110, 61, 100, 1);
        // 땀방울
        oval(h, c(0xbfe3ff), 203, 76, 4, 6);
        h.dispose();
        shadePass(L, 1, -1, 0xbcd4ff, 0.5, 0.0);
        g.drawImage(L, 0, 0, null);

        if (u > 0.85) {
            Point2D e = at.transform(new Point2D.Double(140, 85), null);
            sparkle(img, (int) e.getX(), (int) e.getY(), (int) (8 * Math.sin(seg(u, 0.85, 1.2) * Math.PI)));
        }
    }

    // ------------------------------------------------------------------
    // 4. 투구 (옆모습, 역광) - 관절마다 스프링-댐퍼 물리로 움직인다
    //    어깨가 먼저 돌고 팔꿈치·손목이 늦게 따라오다가 채찍처럼 튀어 나간다
    // ------------------------------------------------------------------

    // 몸 길이 (픽셀)
    static final double TORSO = 30, UA = 17, FA = 16, THIGH = 24, SHIN = 24;
    static final double P_X0 = 108, P_GROUND = 156, P_REL = 1.64;

    // 키프레임 시간 (동작 시간)
    static final double[] PT = {0, 0.5, 0.95, 1.35, 1.5, 1.62, 1.9, 2.4};
    // 목표 각도 (화면 기준 라디안, 0=오른쪽, -π/2=위): 몸통, 던지는 팔 위·아래, 글러브 팔 위·아래
    static final double[][] PANG = {
            {-1.57, 1.3, -1.28, 1.4, -1.6},
            {-1.57, 1.3, -1.28, 1.4, -1.6},
            {-1.72, 1.45, -1.0, 1.5, -1.6},     // 다리 들기
            {-1.62, 2.6, 2.8, -0.1, 0.0},       // 스트라이드: 팔을 뒤로, 글러브는 포수 쪽
            {-1.45, 3.3, 4.3, -0.05, 0.0},      // 코킹: 팔뚝이 뒤로 젖혀짐
            {-0.95, 5.75, 5.2, 1.2, -2.0},      // 릴리스
            {-0.6, 7.2, 7.6, 1.5, -1.8},        // 팔로스루
            {-0.65, 7.4, 7.7, 1.5, -1.8}};
    // 골반 위치, 앞발, 축발 (기준점에서의 거리)
    static final double[][] PPOS = {
            {0, 0, 8, 0, -4, 0},
            {0, 0, 8, 0, -4, 0},
            {2, -2, 2, -30, -4, 0},
            {26, 8, 60, 0, -2, 0},
            {32, 10, 60, 0, 2, -2},
            {38, 10, 60, 0, 10, -6},
            {46, 8, 60, 0, 40, -16},
            {48, 6, 60, 0, 46, -8}};
    // 관절 강성(k)과 감쇠비(z): 끝쪽 관절일수록 부드러워서 늦게 따라온다
    static final double[] PK = {300, 700, 380, 500, 500}, PZ = {0.7, 0.55, 0.42, 0.6, 0.6};

    /** 실제 시간 → 동작 시간 (릴리스 앞뒤만 1/4 속도) */
    static double pitchS(double u) {
        double slowEnd = 1.45 + 0.27 / 0.25;
        if (u <= 1.45) return u;
        if (u <= slowEnd) return 1.45 + (u - 1.45) * 0.25;
        return 1.72 + (u - slowEnd);
    }

    static final class Pose {
        double[] ang;
        double px, py, sx, sy, ex, ey, hx, hy;   // 골반, 어깨, 팔꿈치, 손
        double[] trail = new double[24];         // 최근 손 위치 12개
        int trailN;
        boolean released;
        double relX, relY, relVX, relVY;
    }

    static void pitchJoints(Pose p, double s, double[] a) {
        double[] pp = kfa(s, PT, PPOS);
        p.px = P_X0 + pp[0];
        p.py = P_GROUND - 45 + pp[1];
        double[] sh = shoulderOf(p.px, p.py, a[0], 1);
        p.sx = sh[0];
        p.sy = sh[1];
        p.ex = p.sx + Math.cos(a[1]) * UA;
        p.ey = p.sy + Math.sin(a[1]) * UA;
        p.hx = p.ex + Math.cos(a[2]) * FA;
        p.hy = p.ey + Math.sin(a[2]) * FA;
    }

    /** 0초부터 s까지 스프링 물리를 적분한다 (매 프레임 다시 계산해도 결과가 같다) */
    static Pose simPitch(double s) {
        Pose p = new Pose();
        double[] a = kfa(0, PT, PANG).clone(), v = new double[a.length];
        double dt = 1 / 600.0, prevHx = 0, prevHy = 0;
        int step = 0;
        for (double t = 0; t < s; t += dt, step++) {
            double[] tg = kfa(t, PT, PANG);
            for (int i = 0; i < a.length; i++) {
                double acc = PK[i] * (tg[i] - a[i]) - 2 * PZ[i] * Math.sqrt(PK[i]) * v[i];
                v[i] += acc * dt;
                a[i] += v[i] * dt;
            }
            pitchJoints(p, t, a);
            if (!p.released && t >= P_REL) {
                p.released = true;
                p.relX = p.hx;
                p.relY = p.hy;
                p.relVX = (p.hx - prevHx) / dt;
                p.relVY = (p.hy - prevHy) / dt;
            }
            prevHx = p.hx;
            prevHy = p.hy;
            if (step % 5 == 0) {
                System.arraycopy(p.trail, 0, p.trail, 2, p.trail.length - 2);
                p.trail[0] = p.hx;
                p.trail[1] = p.hy;
                p.trailN = Math.min(12, p.trailN + 1);
            }
        }
        pitchJoints(p, s, a);
        p.ang = a;
        return p;
    }

    static void sceneThrow(BufferedImage img, Graphics2D g, double u) {
        double s = pitchS(u);
        Pose p = simPitch(s);

        // 배경: 투수 뒤 위쪽에 거대한 조명 (역광)
        vgrad(img, 0, 122, 0x03050f, 0x0a1033, 0x1a2660, 0x2c3c80);
        stars(img, 60, u, 71);
        for (int i = 8; i >= 1; i--) {
            double r = 12 + i * 16;
            oval(g, ca(0xfff2c0, 14), 64 - r, 34 - r * 0.8, r * 2, r * 1.6);
        }
        line(g, ca(0xfff2c0, 60), 1, 0, 34, W, 34);
        bigTower(g, 64, 34, u);
        crowd(img, g, 74, 118, 73, u, 0x0d1128);
        rect(g, NAVY_D, 0, 72, W, 3);
        rect(g, WALL_D, 0, 118, W, 10);
        rect(g, c(0x8a7a30), 0, 118, W, 1);
        for (int y = 128; y < H; y++) {
            double d = Math.pow((y - 128) / 52.0, 0.8) * 5;
            rect(g, ((int) d) % 2 == 0 ? GRASS_D : GRASS, 0, y, W, 1);
        }
        // 마운드
        oval(g, DIRT, -20, P_GROUND - 6, 300, 70);
        oval(g, DIRT_L, 10, P_GROUND - 4, 240, 30);
        AffineTransform cam = new AffineTransform();   // 투수를 크게 잡는 카메라
        cam.translate(P_X0 + 30, P_GROUND + 4);
        cam.scale(1.32, 1.32);
        cam.translate(-(P_X0 + 30), -(P_GROUND + 4));
        g.setTransform(cam);
        rect(g, WHITE, P_X0 - 8, P_GROUND - 1, 14, 2);

        // 앞발이 땅에 닿을 때 흙먼지
        if (s > 1.35) {
            double d = s - 1.35;
            for (int i = 0; i < 14; i++) {
                int hsh = hash(i, 5, 9);
                double vx = ((hsh & 255) / 255.0 - 0.3) * 70, vy = -20 - ((hsh >>> 8) & 255) / 255.0 * 40;
                double x = P_X0 + 60 + vx * d, y = P_GROUND + vy * d + 90 * d * d;
                if (y > P_GROUND + 1 || d > 0.6) continue;
                oval(g, ca(0xc98d55, (int) (200 * (1 - d / 0.6))), x - 2, y - 2, 4, 4);
            }
        }

        g.setTransform(new AffineTransform());
        BufferedImage L = layer();
        Graphics2D h = pg(L);
        h.setTransform(cam);
        drawPitcherSide(h, p, s);
        h.dispose();
        shadePass(L, -1, -1, 0xfff0c8, 0.6, 0.12);
        g.drawImage(L, 0, 0, null);
        g.setTransform(cam);

        // 팔 궤적 잔상 (빠를 때만)
        if (s > 1.45 && s < 1.85) {
            for (int i = 1; i < p.trailN; i++) {
                double x1 = p.trail[(i - 1) * 2], y1 = p.trail[(i - 1) * 2 + 1], x2 = p.trail[i * 2], y2 = p.trail[i * 2 + 1];
                if (Math.hypot(x2 - x1, y2 - y1) < 2) continue;
                line(g, ca(0xffffff, 110 - i * 9), (float) Math.max(1, 5 - i * 0.4), x1, y1, x2, y2);
            }
        }

        // 손을 떠난 공
        if (p.released) {
            double d = s - P_REL;
            double sp = Math.hypot(p.relVX, p.relVY);
            double vx = p.relVX / sp * 900, vy = p.relVY / sp * 900 + 120;   // 공은 포수 쪽으로
            for (int k = 10; k >= 1; k--) {
                double dk = Math.max(0, d - k * 0.006);
                oval(g, ca(0xfff6d0, 210 - k * 18), p.relX + vx * dk - 2, p.relY + vy * dk * 0.3 - 2, 4, 4);
            }
            Point2D bp = cam.transform(new Point2D.Double(p.relX + vx * d, p.relY + vy * d * 0.3), null);
            ball(img, bp.getX(), bp.getY(), 4.2, d * 60, 0.3);
            // 릴리스 순간 공기 링
            if (d < 0.08) {
                double e = d / 0.08;
                g.setColor(ca(0xffffff, (int) (200 * (1 - e))));
                g.setStroke(new BasicStroke(1));
                g.draw(new Ellipse2D.Double(p.relX - 4 - 20 * e, p.relY - 6 - 26 * e, 8 + 40 * e, 12 + 52 * e));
            }
        }
        g.setTransform(new AffineTransform());
    }

    /** 옆에서 본 투수 (오른쪽을 보고 던진다, 흰 홈 유니폼) */
    static void drawPitcherSide(Graphics2D h, Pose p, double s) {
        double[] a = p.ang;
        double[] pp = kfa(s, PT, PPOS);
        Athlete A = new Athlete();
        A.f = 1;
        A.px = p.px;
        A.py = p.py;
        A.spine = a[0];
        A.farFoot = new double[]{P_X0 + pp[2], P_GROUND + pp[3]};
        A.nearFoot = new double[]{P_X0 + pp[4], P_GROUND + pp[5]};
        double[] sh = shoulderOf(p.px, p.py, a[0], 1);
        double gsx = sh[0] + Math.cos(a[0]) * 0.5 - (-Math.sin(a[0])) * 1.5, gsy = sh[1] + Math.sin(a[0]) * 0.5 - Math.cos(a[0]) * 1.5;
        double gex = gsx + Math.cos(a[3]) * UA, gey = gsy + Math.sin(a[3]) * UA;
        A.farElbow = new double[]{gex, gey};
        A.farHand = new double[]{gex + Math.cos(a[4]) * FA, gey + Math.sin(a[4]) * FA};
        A.nearElbow = new double[]{p.ex, p.ey};
        A.nearHand = new double[]{p.hx, p.hy};
        A.jersey = WHITE; A.jerseyS = WHITE_S; A.pants = WHITE; A.pantsS = WHITE_S;
        A.sleeve = RED; A.sleeveS = RED_D; A.sock = NAVY; A.sockS = NAVY_D;
        A.cap = NAVY; A.capS = NAVY_D; A.trim = NAVY;
        A.farItem = () -> {           // 글러브
            double gx = A.farHand[0], gy = A.farHand[1];
            oval(h, LEATHER_D, gx - 6.5, gy - 6, 13, 12);
            oval(h, LEATHER, gx - 6, gy - 6, 11.5, 10.5);
            oval(h, LEATHER_D, gx - 2, gy - 2, 5, 4);
            line(h, c(0xc9a070), 1, gx - 4, gy - 4, gx + 3, gy - 5);
        };
        A.nearItem = () -> {          // 손 안의 공
            if (!p.released)
                oval(h, WHITE, p.hx + Math.cos(a[2]) * 2.5 - 2.2, p.hy + Math.sin(a[2]) * 2.5 - 2.2, 4.4, 4.4);
        };
        drawAthlete(h, A);
    }

    /** 두 마디 IK: 엉덩이(어깨)와 발(손) 위치로 무릎(팔꿈치) 위치를 구한다 */
    static double[] ik(double hx, double hy, double fx, double fy, double l1, double l2, int bend) {
        double dx = fx - hx, dy = fy - hy;
        double d = Math.max(0.01, Math.min(Math.hypot(dx, dy), l1 + l2 - 0.01));
        double base = Math.atan2(dy, dx);
        double cosA = (l1 * l1 + d * d - l2 * l2) / (2 * l1 * d);
        double A = Math.acos(Math.max(-1, Math.min(1, cosA)));
        double ang = base - bend * A;
        return new double[]{hx + Math.cos(ang) * l1, hy + Math.sin(ang) * l1};
    }
    // ------------------------------------------------------------------
    // 5. 타자 눈앞으로 날아오는 공
    // ------------------------------------------------------------------

    static void sceneIncoming(BufferedImage img, Graphics2D g, double u) {
        vgrad(img, 0, 36, 0x070a1f, 0x111b4a, 0x1f2d66);
        stars(img, 30, u, 9);
        lightTower(g, 30, 12, 0.8, u);
        lightTower(g, 290, 12, 0.8, u);
        crowd(img, g, 30, 64, 13, u, 0x161b36);
        rect(g, WALL, 0, 64, W, 12);
        rect(g, YELLOW, 0, 63, W, 1);
        rect(g, WALL_D, 0, 74, W, 2);
        for (int y = 76; y < H; y++) {
            double d = Math.pow((y - 76) / 104.0, 0.7) * 9;
            rect(g, ((int) d) % 2 == 0 ? GRASS : GRASS_L, 0, y, W, 1);
        }
        oval(g, DIRT_L, 118, 96, 84, 22);
        oval(g, DIRT, 126, 100, 68, 14);
        rect(g, WHITE, 154, 103, 12, 2);

        BufferedImage L = layer();
        Graphics2D h = pg(L);
        drawPitcherFrontSmall(h, 160, 105);
        h.dispose();
        shadePass(L, -1, -1, 0xfff0c8, 0.4, 0.1);
        g.drawImage(L, 0, 0, null);

        double k = easeIn(seg(u, 0, 1.15));
        speedLines(g, 164, 98, k, u);
        // 어두워지는 가장자리
        for (int i = 0; i < 5; i++) {
            g.setColor(ca(0x000000, (int) (30 * k)));
            g.setStroke(new BasicStroke(10));
            g.draw(new Ellipse2D.Double(-40 + i * 8, -40 + i * 6, W + 80 - i * 16, H + 80 - i * 12));
        }
        double r = 1.5 * Math.pow(42, clamp(u / 1.15));
        ball(img, 164 + 4 * k, 98 + 2 * k, r, u * 28, 0.3);
    }

    // ------------------------------------------------------------------
    // 6. 타격 (옆모습) - 손이 배트를 끌고 오고, 배트는 스프링으로 늦게 따라오다 휘어 나간다
    //    배트 방향은 3D로 계산해서 화면 쪽을 향할수록 짧고 끝이 커 보이게 한다
    // ------------------------------------------------------------------

    static final double B_X0 = 204, B_GROUND = 158, B_CONTACT = 0.40;
    // 스윙 단계: 준비 → 로드(체중 뒤로, 앞발 들기) → 스트라이드(손은 뒤에 남김) → 골반 회전(뒤 팔꿈치 붙이고 손이 몸 가까이)
    //          → 컨택(공 궤적과 나란히) → 익스텐션(팔을 바깥으로 뻗음) → 팔로스루(앞 어깨 위로)
    static final double[] HT = {0, 0.22, 0.30, 0.35, 0.40, 0.48, 0.62, 0.9};
    // 골반 x·y, 앞발 x·y, 뒷발 x·y, 손(어깨 기준) x·y, 몸통 각도, 배트 들린 각도
    static final double[][] HPOS = {
            {0, 6, -22, 0, 16, 0, 8, -12, -1.44, 0.85},      // 준비: 무릎 굽힌 넓은 자세, 배트는 뒤로 45도, 손은 귀 높이
            {3, 5, -20, -6, 16, 0, 12, -10, -1.42, 0.9},     // 로드: 체중 뒤로, 앞발 살짝 듦
            {-6, 8, -40, 0, 16, 0, 11, -9, -1.45, 0.35},     // 스트라이드: 앞발 넓게, 손은 어깨에 남고 배트가 머리 뒤로 눕는다
            {-10, 9, -40, 0, 15, -1, 4, 2, -1.45, 0.2},      // 골반 회전: 뒤 팔꿈치 붙이고 손이 몸 가까이
            {-12, 9, -40, 0, 11, -3, -12, 14, -1.47, -0.05}, // 컨택: 앞다리 펴서 버팀, 뒷무릎 굽혀 밀고 뒷발은 발끝
            {-13, 9, -40, 0, 8, -4, -22, 10, -1.52, 0.1},    // 익스텐션: 팔을 바깥으로
            {-14, 11, -40, 0, 2, -6, -12, -12, -1.62, 0.7},  // 팔로스루: 배트가 앞 어깨를 감싸고 뒷무릎이 내려감
            {-14, 12, -40, 0, 0, -7, -6, -16, -1.66, 0.3}};  // 마무리
    static final double[] HPSI = {0.1, 0.0, 0.0, 0.5, 2.3, 3.0, 4.2, 5.6};
    static final double BAT = 68;

    /** 실제 시간 → 동작 시간 (맞는 순간 앞뒤로 1/10 속도) */
    static double hitS(double u) {
        double slowEnd = 0.36 + 0.10 / 0.1;
        if (u <= 0.36) return u;
        if (u <= slowEnd) return 0.36 + (u - 0.36) * 0.1;
        return 0.46 + (u - slowEnd);
    }

    /** 배트 회전각 ψ 를 스프링으로 적분 */
    static double simPsi(double s) {
        double a = 0, v = 0, dt = 1 / 600.0, k = 900, z = 0.45;
        for (double t = 0; t < s; t += dt) {
            double tg = kf1(t, HT, HPSI);
            double acc = k * (tg - a) - 2 * z * Math.sqrt(k) * v;
            v += acc * dt;
            a += v * dt;
        }
        return a;
    }

    /** 배트 끝(barrel) 위치: [손x, 손y, 끝x, 끝y, 깊이] */
    static double[] batPose(double s) {
        double[] q = kfa(s, HT, HPOS);
        double px = B_X0 + q[0], py = B_GROUND - 45 + q[1];
        double[] sh = shoulderOf(px, py, q[8], -1);
        double hx = sh[0] + q[6], hy = sh[1] + q[7];
        double psi = simPsi(s), beta = q[9];
        double dx = Math.cos(beta) * Math.cos(psi), dy = -Math.sin(beta), dz = Math.cos(beta) * Math.sin(psi);
        return new double[]{hx, hy, hx + dx * BAT, hy + dy * BAT, dz};
    }

    static void sceneHit(BufferedImage img, Graphics2D g, double u) {
        double s = hitS(u);
        double ds = s - B_CONTACT;
        double[] cp = batPose(B_CONTACT);
        double cx = lerp(cp[0], cp[2], 0.82), cy = lerp(cp[1], cp[3], 0.82);

        // 배경: 어두운 경기장, 흐린 조명
        vgrad(img, 0, H, 0x050816, 0x0c1234, 0x18204a, 0x221c38);
        bokeh(g, 91, u, 14);
        crowd(img, g, 60, 120, 93, u, 0x0d1128);
        rect(g, WALL_D, 0, 120, W, 8);
        rect(g, DIRT_D, 0, 128, W, H - 128);
        oval(g, DIRT, -40, 150, 400, 60);
        poly(g, WHITE, 150, 160, 176, 160, 180, 163, 163, 166, 146, 163);

        // 맞은 뒤: 공이 날아가는 방향으로 속도선
        if (ds > 0) {
            double e = clamp(ds / 0.06);
            Random r = new Random(55 + (int) (u * 20));
            for (int i = 0; i < 26; i++) {
                double y = r.nextDouble() * H, x = r.nextDouble() * W, len = 30 + 80 * r.nextDouble();
                line(g, ca(0xffffff, (int) (40 + 60 * e)), 1, x, y, x + len * 0.85, y + len * 0.53);
            }
        }
        // 맞는 순간 빛
        if (ds > -0.01 && ds < 0.12) {
            double e = clamp((ds + 0.01) / 0.13);
            for (int i = 6; i >= 1; i--) {
                double r = (10 + i * 12) * (0.6 + e);
                oval(g, ca(0xffe9a8, (int) (34 * (1 - e))), cx - r, cy - r, r * 2, r * 2);
            }
        }

        BufferedImage L = layer();
        Graphics2D h = pg(L);
        drawBatterSide(h, s);
        h.dispose();
        shadePass(L, -1, -1, 0xffe9a8, ds > 0 && ds < 0.1 ? 0.8 : 0.45, 0.2);
        g.drawImage(L, 0, 0, null);

        // 공
        if (ds < 0) {
            double f = clamp((s - 0.24) / (B_CONTACT - 0.24));
            double bx = lerp(-20, cx, f), by = lerp(cy - 14, cy, f);
            for (int k = 8; k >= 1; k--) {
                double fk = Math.max(0, f - k * 0.03);
                oval(g, ca(0xffffff, 120 - k * 13), lerp(-20, cx, fk) - 2, lerp(cy - 14, cy, fk) - 2, 4, 4);
            }
            if (f > 0) ball(img, bx, by, 5, s * 40, 0.3);
        } else if (ds < 0.012) {
            oval(g, OUT, cx - 8, cy - 4, 15, 9);           // 맞는 순간 납작해진 공
            oval(g, WHITE, cx - 7, cy - 3, 13, 7);
            rect(g, RED, cx - 4, cy - 1, 8, 1);
        } else {
            double vx = -620, vy = -380;
            double d = ds - 0.012;
            for (int k = 14; k >= 1; k--) {
                double dk = Math.max(0, d - k * 0.004);
                oval(g, ca(0xfff6d0, 220 - k * 14), cx + vx * dk - 2.5, cy + vy * dk - 2.5, 5, 5);
            }
            ball(img, cx + vx * d, cy + vy * d, 5, d * 60, 0.3);
        }

        // 충격파 링 + 불꽃
        if (ds >= 0) {
            for (int ring = 0; ring < 3; ring++) {
                double d = ds - ring * 0.012;
                if (d < 0 || d > 0.09) continue;
                double e = d / 0.09, R = 6 + 120 * easeOut(e);
                g.setColor(ca(ring == 1 ? 0xffe14d : 0xffffff, (int) (230 * (1 - e))));
                g.setStroke(new BasicStroke(ring == 0 ? 2 : 1));
                g.draw(new Ellipse2D.Double(cx - R, cy - R * 0.6, R * 2, R * 1.2));
            }
            for (int i = 0; i < 30; i++) {
                int hs = hash(i, 17, 4);
                double ang = -2.6 + ((hs & 1023) / 1023.0 - 0.5) * 2.4;
                double sp = 200 + ((hs >>> 10) & 1023) / 1023.0 * 500;
                double d = ds;
                if (d > 0.35) continue;
                double x = cx + Math.cos(ang) * sp * d, y = cy + Math.sin(ang) * sp * d + 300 * d * d;
                double x2 = x - Math.cos(ang) * Math.min(10, sp * 0.02), y2 = y - Math.sin(ang) * Math.min(10, sp * 0.02);
                line(g, ca(i % 3 == 0 ? 0xffffff : 0xffd23f, (int) (255 * (1 - d / 0.35))), 1, x, y, x2, y2);
            }
        }

        // 임팩트 프레임: 맞는 순간 한두 프레임 흑백 반전
        if (ds >= 0 && ds < 0.005) impactFrame(img);
    }

    static void impactFrame(BufferedImage img) {
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++) {
                int c = img.getRGB(x, y);
                int lum = (((c >> 16) & 255) * 3 + ((c >> 8) & 255) * 6 + (c & 255)) / 10;
                img.setRGB(x, y, lum > 70 ? 0x0a0a14 : 0xf4f1e8);
            }
    }

    /** 옆에서 본 타자 (왼쪽 투수를 보고 친다, 회색 원정 유니폼) */
    static void drawBatterSide(Graphics2D h, double s) {
        double[] q = kfa(s, HT, HPOS);
        double[] bp = batPose(s);
        double hx = bp[0], hy = bp[1];
        double ds = s - B_CONTACT;
        double shake = ds > 0 && ds < 0.05 ? Math.sin(ds * 900) * 2 * (1 - ds / 0.05) : 0;
        Athlete A = new Athlete();
        A.f = -1;
        A.px = B_X0 + q[0];
        A.py = B_GROUND - 45 + q[1];
        A.spine = q[8];
        A.farFoot = new double[]{B_X0 + q[4], B_GROUND + q[5]};
        A.nearFoot = new double[]{B_X0 + q[2], B_GROUND + q[3]};
        double[] sh = shoulderOf(A.px, A.py, A.spine, -1);
        double fsx = sh[0] + 1.5, fsy = sh[1] - 0.5;     // 먼 어깨
        A.farElbow = elbowDown(fsx, fsy, hx + 1.5, hy - 1);
        A.farHand = new double[]{hx + 1.5, hy - 1};
        A.nearElbow = elbowDown(sh[0], sh[1], hx - 1, hy + 1.5);
        A.nearHand = new double[]{hx - 1, hy + 1.5};
        A.jersey = GRAY; A.jerseyS = GRAY_S; A.pants = GRAY; A.pantsS = GRAY_S;
        A.sleeve = RED; A.sleeveS = RED_D; A.sock = RED; A.sockS = RED_D;
        A.cap = RED; A.capS = RED_D; A.trim = RED_D; A.helmet = true;
        A.midItem = () -> {
            double ex = bp[2], ey = bp[3] + shake, dz = bp[4];
            double knx = hx - (ex - hx) * 0.12, kny = hy - (ey - hy) * 0.12;
            double endW = 7 + 5 * Math.max(0, dz);
            if (ds < 0.06 && ds > -0.1) {                  // 배트 끝이 지나간 궤적 (얇은 호)
                double px = ex, py = ey;
                for (int i = 1; i <= 8; i++) {
                    double[] pb = batPose(Math.max(0, s - i * 0.006));
                    line(h, ca(0xffffff, 150 - i * 17), (float) Math.max(1, 3 - i * 0.3), px, py, pb[2], pb[3]);
                    px = pb[2];
                    py = pb[3];
                }
            }
            limb(h, knx, kny, hx, hy, 3.5, 3.5, WOOD_S);
            limb(h, hx, hy, lerp(hx, ex, 0.45), lerp(hy, ey, 0.45), 4, 5.5, WOOD);
            limb(h, lerp(hx, ex, 0.45), lerp(hy, ey, 0.45), ex, ey, 5.5, endW, WOOD);
            line(h, WOOD_L, 1, lerp(hx, ex, 0.5), lerp(hy, ey, 0.5) - 1.5, ex, ey - endW / 2 + 1.5);
            oval(h, WOOD_S, ex - endW / 2, ey - endW / 2, endW, endW);
            oval(h, WOOD, ex - endW / 2 + 1, ey - endW / 2 + 1, endW - 2, endW - 2);
            oval(h, WOOD_S, knx - 2.5, kny - 2.5, 5, 5);
        };
        A.nearItem = () -> {          // 배팅 장갑 (두 손이 배트를 위아래로 쥠)
            double ex = bp[2], ey = bp[3];
            double l = Math.hypot(ex - hx, ey - hy), dx = (ex - hx) / Math.max(1, l), dy = (ey - hy) / Math.max(1, l);
            oval(h, BLACK, hx - 3.6 - dx * 2, hy - 3.6 - dy * 2, 7.2, 7.2);
            oval(h, BLACK_L, hx - 3.4 + dx * 4, hy - 3.4 + dy * 4, 6.8, 6.8);
            rect(h, WHITE, hx - 1.5 + dx * 4, hy - 0.5 + dy * 4, 3, 1);
        };
        drawAthlete(h, A);
    }

    static double[] elbowDown(double sx, double sy, double hx, double hy) {
        double[] e1 = ik(sx, sy, hx, hy, UA - 1, FA - 1, 1), e2 = ik(sx, sy, hx, hy, UA - 1, FA - 1, -1);
        return e1[1] > e2[1] ? e1 : e2;
    }

    // ------------------------------------------------------------------
    // 7. 날아가는 공 - 포물선(중력) 궤적을 카메라가 따라간다 → 폴대 바로 옆에서 정지
    // ------------------------------------------------------------------

    static void sceneFly(BufferedImage img, Graphics2D g, double u) {
        if (u < 1.6) flySky(img, g, u);
        else flyPole(img, g, u - 1.6);
    }

    /** 공의 월드 좌표 (x 오른쪽, y 위쪽, 땅=0) */
    static double[] flyWorld(double t) {
        return new double[]{300 * t, 20 + 300 * t - 0.5 * 160 * t * t};
    }

    static void flySky(BufferedImage img, Graphics2D g, double u) {
        double[] b = flyWorld(u);
        double camX = b[0] * 0.78, camY = Math.max(0, b[1] - 70) * 0.9;
        vgrad(img, 0, H, 0x020410, 0x070c28, 0x111a48, 0x1f2c68);
        // 별 (아주 느리게 움직임)
        Random r = new Random(19);
        for (int i = 0; i < 70; i++) {
            int x = (int) Math.floorMod((long) (r.nextInt(W * 2) - camX * 0.05), (long) W);
            int y = (int) Math.floorMod((long) (r.nextInt(H * 2) + camY * 0.05), (long) H);
            px(img, x, y, ((int) (u * 6) + i) % 9 == 0 ? 0xffffff : 0x7d86b8);
        }
        // 조명탑 (깊이감: 천천히 지나감)
        for (int i = 0; i < 3; i++) {
            double wx = 140 + i * 300;
            double sx = 60 + (wx - camX * 0.85), sy = 150 - (170 - camY * 0.85);
            if (sx > -80 && sx < W + 80) bigTower(g, sx, sy, u);
        }
        // 관중석 (빠르게 아래로 빠짐)
        double standTop = 150 - (60 - camY * 0.95);
        if (standTop < H) {
            rect(g, NAVY_D, 0, standTop - 4, W, 5);
            crowd(img, g, (int) standTop, H, 31, u * 2.5, 0x10142c);
            // 일어나서 팔을 드는 관중 실루엣
            for (int i = 0; i < 22; i++) {
                int hs = hash(i, 3, 8);
                double x = (hs >>> 2) % W, y = standTop + 22 + (hs >>> 12) % 18;
                double up = Math.max(0, Math.sin(u * 7 + i)) * 4;
                oval(g, c(0x05070f), x - 4, y - 4, 8, 8);
                rect(g, c(0x05070f), x - 6, y + 3, 12, 20);
                line(g, c(0x05070f), 2, x - 5, y + 4, x - 8, y - 4 - up);
                line(g, c(0x05070f), 2, x + 5, y + 4, x + 8, y - 4 - up);
            }
        }

        // 공과 꼬리
        for (int k = 30; k >= 1; k--) {
            double tk = u - k * 0.018;
            if (tk < 0) continue;
            double[] w = flyWorld(tk);
            double x = 60 + (w[0] - camX), y = 150 - (w[1] - camY);
            double rr = 3.6 - k * 0.09;
            oval(g, ca(k < 10 ? 0xffffff : 0xffd98a, (int) (230 * (1 - k / 31.0))), x - rr, y - rr, rr * 2, rr * 2);
        }
        double bx = 60 + (b[0] - camX), by = 150 - (b[1] - camY);
        for (int i = 4; i >= 1; i--) oval(g, ca(0xfff2c0, 22), bx - i * 4, by - i * 4, i * 8, i * 8);
        ball(img, bx, by, 6, u * 20, 0.3);
        if (((int) (u * 10)) % 2 == 0) sparkle(img, (int) bx + 7, (int) by - 6, 2);
    }
    static void flyPole(BufferedImage img, Graphics2D g, double v) {
        BufferedImage S = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D s = pg(S);
        vgrad(S, 0, 40, 0x050819, 0x0c1440, 0x1a2766);
        stars(S, 34, v, 41);
        lightTower(s, 70, 10, 0.9, v);
        crowd(S, s, 36, 84, 43, v * 2, 0x141933);
        // 외야 펜스
        rect(s, WALL, 0, 84, W, 14);
        for (int x = 4; x < W; x += 16) rect(s, WALL_D, x, 85, 1, 11);
        rect(s, YELLOW, 0, 84, W, 1);
        rect(s, WALL_D, 0, 96, W, 2);
        // 워닝 트랙, 잔디
        for (int y = 98; y < H; y++) {
            double d = Math.pow((y - 98) / 82.0, 0.7) * 7;
            rect(s, ((int) d) % 2 == 0 ? GRASS : GRASS_L, 0, y, W, 1);
        }
        rect(s, DIRT, 0, 98, W, 5);
        // 파울 라인 → 폴대
        poly(s, WHITE, 249, 98, 252, 98, 118, 180, 104, 180);
        // 파울 폴 + 그물
        for (int y = 8; y < 84; y += 3) rect(s, ca(0xffd23f, 110), 242, y, 6, 1);
        rect(s, YELLOW, 242, 8, 1, 76);
        rect(s, YELLOW, 248, 2, 4, 96);
        rect(s, c(0xc9a020), 251, 2, 1, 96);

        // 공: 왼쪽 위에서 폴대 바로 옆까지
        double e = easeOut(seg(v, 0, 0.9));
        double bx = lerp(-10, 243, e), by = lerp(-6, 44, e) - 18 * Math.sin(Math.PI * e * 0.8);
        for (int k = 20; k >= 1; k--) {
            double fk = e - k * 0.02;
            if (fk < 0) continue;
            double tx = lerp(-10, 243, fk), ty = lerp(-6, 44, fk) - 18 * Math.sin(Math.PI * fk * 0.8);
            oval(s, ca(0xfff6d0, (int) (200 * (1 - k / 21.0))), tx - 2, ty - 2, 4, 4);
        }
        boolean frozen = v >= 0.9;
        ball(S, bx, by, 4.5, frozen ? 1.2 : v * 20, 0.3);
        s.dispose();

        // 정지 화면 + 확대
        double zf = easeInOut(seg(v, 0.9, 1.3));
        double z = 1 + 1.1 * zf;
        double fx = lerp(243, 190, zf), fy = lerp(44, 82, zf);
        AffineTransform at = new AffineTransform();
        at.translate(fx, fy);
        at.scale(z, z);
        at.translate(-243, -44);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(S, at, null);
        if (frozen) {
            for (int i = 0; i < 6; i++) {
                g.setColor(ca(0x000000, (int) (40 * zf)));
                g.setStroke(new BasicStroke(12));
                g.draw(new Ellipse2D.Double(-30 + i * 10, -30 + i * 8, W + 60 - i * 20, H + 60 - i * 16));
            }
            double bounce = 4 * Math.abs(Math.sin(v * 9));
            glyphs(g, "?!", fx + 24, fy - 18 - bounce, 3, OUT);
            glyphs(g, "?!", fx + 22, fy - 20 - bounce, 3, YELLOW);
        }
    }

    // ------------------------------------------------------------------
    // 8. 판정하려는 심판
    // ------------------------------------------------------------------

    static void sceneUmpire(BufferedImage img, Graphics2D g, double u) {
        vgrad(img, 0, H, 0x080c20, 0x121a3c, 0x1c2550);
        crowd(img, g, 0, 112, 57, u * 2, 0x141933);
        rect(g, NAVY, 0, 112, W, 16);
        rect(g, NAVY_D, 0, 126, W, 2);
        for (int i = 0; i < 4; i++) glyphs(g, "BASEUP", i * 84 + 24, 116, 1, i % 2 == 0 ? WHITE : YELLOW);
        rect(g, DIRT_D, 0, 128, W, H - 128);

        BufferedImage L = layer();
        Graphics2D h = pg(L);
        drawUmpireBig(L, h, u);
        h.dispose();
        shadePass(L, 1, -1, 0xbcd4ff, 0.45, 0.05);
        g.drawImage(L, 0, 0, null);

        // 말풍선 "#$@!?"
        if (u > 0.3) {
            double sc = easeOutBack(seg(u, 0.3, 0.45));
            double cx = 240, cy = 44, bw = 116 * sc, bh = 46 * sc;
            poly(g, OUT, 178, 70, 196, cy + 6, 206, cy + 14);
            g.setColor(OUT);
            g.fill(new RoundRectangle2D.Double(cx - bw / 2 - 2, cy - bh / 2 - 2, bw + 4, bh + 4, 14, 14));
            poly(g, WHITE, 181, 68, 198, cy + 7, 205, cy + 12);
            g.setColor(WHITE);
            g.fill(new RoundRectangle2D.Double(cx - bw / 2, cy - bh / 2, bw, bh, 12, 12));
            if (sc > 0.9) {
                String sym = "#$@!?%&*";
                StringBuilder sb = new StringBuilder();
                int f = (int) (u * 12);
                for (int i = 0; i < 5; i++) sb.append(sym.charAt(Math.abs(hash(i, f, 3)) % sym.length()));
                Font font = kfont(Font.BOLD, 22);
                g.setFont(font);
                double tw = g.getFontMetrics().stringWidth(sb.toString());
                textOutlined(g, sb.toString(), cx - tw / 2, cy + 8, font, RED, OUT, 1);
            }
        }
    }

    // ------------------------------------------------------------------
    // 9. 타이틀: 규칙 알아보기 / BASEUP
    // ------------------------------------------------------------------

    static void sceneTitle(BufferedImage img, Graphics2D g, double u) {
        vgrad(img, 0, H, 0x050818, 0x0b1438, 0x15225a, 0x1d2f70);
        stars(img, 120, u, 61);
        // 아래쪽 야구장
        poly(g, c(0x123d22), 0, 180, 0, 152, 160, 132, 320, 152, 320, 180);
        poly(g, c(0x5a3c22), 160, 136, 236, 160, 160, 184, 84, 160);
        poly(g, c(0x164a29), 160, 145, 214, 161, 160, 177, 106, 161);
        rect(g, WHITE, 158, 135, 4, 2);
        rect(g, WHITE, 234, 159, 3, 2);
        rect(g, WHITE, 84, 159, 3, 2);
        lightTower(g, 26, 118, 0.7, u);
        lightTower(g, 294, 118, 0.7, u);

        // 규칙 알아보기 (한 글자씩)
        String sub = "규칙 알아보기";
        int n = (int) Math.round(sub.length() * seg(u, 0.15, 0.7));
        Font sf = kfont(Font.BOLD, 14);
        g.setFont(sf);
        double sw = g.getFontMetrics().stringWidth(sub);
        textOutlined(g, sub.substring(0, n), 160 - sw / 2, 52, sf, YELLOW, OUT, 1);

        // BASEUP 글자가 하나씩 떨어진다
        String word = "BASEUP";
        int sc = 6;
        double x0 = (W - (word.length() * 6 * sc - sc)) / 2.0;
        for (int i = 0; i < word.length(); i++) {
            double st = 0.55 + 0.08 * i;
            if (u < st) continue;
            double y = lerp(-50, 64, bounce(seg(u, st, st + 0.4)));
            String ch = String.valueOf(word.charAt(i));
            double x = x0 + i * 6 * sc;
            glyphs(g, ch, x + 4, y + 4, sc, NAVY_D);
            glyphs(g, ch, x + 2, y + 2, sc, RED);
            glyphs(g, ch, x, y, sc, WHITE);
        }
        // 빨간 밑줄
        double ue = easeOut(seg(u, 1.2, 1.5));
        if (ue > 0) rect(g, RED, x0, 116, (word.length() * 6 * sc - sc) * ue, 3);
        // 날아와서 마침표가 되는 공
        if (u > 1.05) {
            double e = easeOut(seg(u, 1.05, 1.45));
            double bx = lerp(340, x0 + word.length() * 6 * sc + 6, e), by = lerp(10, 103, e) - 30 * Math.sin(Math.PI * e);
            ball(img, bx, by, 6, u < 1.45 ? u * 25 : 1.0, 0.3);
            if (u > 1.45 && u < 1.8) sparkle(img, (int) bx, (int) by - 9, (int) (5 * Math.sin(seg(u, 1.45, 1.8) * Math.PI)));
        }
        // 시작 안내
        if (u > 1.6 && ((int) (u * 2.5)) % 2 == 0) {
            Font hf = kfont(Font.BOLD, 11);
            g.setFont(hf);
            String hint = "클릭해서 시작";
            double hw = g.getFontMetrics().stringWidth(hint);
            textOutlined(g, hint, 160 - hw / 2, 152, hf, WHITE, OUT, 1);
        }
    }

    // ------------------------------------------------------------------
    // 선수 그리기
    // ------------------------------------------------------------------

    /** 멀리서 본 투수 (던진 직후, 정면) */
    static void drawPitcherFrontSmall(Graphics2D h, double cx, double foot) {
        double top = foot - 24;
        line(h, WHITE, 3, cx - 2, top + 16, cx - 6, foot - 2);
        line(h, WHITE, 3, cx + 2, top + 16, cx + 5, foot - 2);
        rect(h, BLACK, cx - 8, foot - 2, 4, 2);
        rect(h, BLACK, cx + 4, foot - 2, 4, 2);
        h.setColor(WHITE);
        h.fill(new RoundRectangle2D.Double(cx - 5, top + 7, 10, 10, 3, 3));
        line(h, RED, 2, cx + 4, top + 9, cx - 6, top + 18);
        oval(h, LEATHER, cx + 4, top + 8, 5, 5);
        oval(h, SKIN, cx - 3, top + 2, 6, 6);
        rect(h, NAVY, cx - 4, top, 8, 3);
        rect(h, NAVY, cx - 4, top + 3, 9, 1);
    }

    /** 가까이서 본 주심 (마스크를 벗고 판정하려는 순간) */
    static void drawUmpireBig(BufferedImage L, Graphics2D h, double u) {
        double raise = easeOut(seg(u, 0.1, 0.7));
        // 몸: 어깨너비는 머리 폭(52)의 약 2.3배. 안에 찬 프로텍터 때문에 가슴이 조금 두툼하다
        poly(h, UMP, 64, 190, 70, 140, 84, 122, 108, 113, 152, 113, 176, 122, 190, 140, 196, 190);
        poly(h, UMP_L, 96, 124, 164, 124, 170, 150, 162, 186, 98, 186, 90, 150);
        poly(h, UMP, 99, 127, 161, 127, 166, 150, 159, 183, 101, 183, 94, 150);
        poly(h, WHITE, 116, 113, 144, 113, 130, 128);
        line(h, UMP_L, 2, 116, 113, 130, 130);
        line(h, UMP_L, 2, 144, 113, 130, 130);
        rect(h, c(0x3c64b4), 146, 134, 12, 9);
        rect(h, WHITE, 148, 136, 8, 1);
        rect(h, WHITE, 148, 139, 5, 1);
        // 목 (굵고 짧게)
        rect(h, SKIN, 115, 94, 30, 24);
        rect(h, SKIN_S, 115, 94, 30, 7);
        // 반팔 상의: 위팔 절반은 남색 소매, 아래팔은 맨살이라 어두운 배경과 구분된다
        Color sleeve = UMP_L, sleeveS = UMP;
        double[] P_UARM = {0, 13, 13, 0.35, 13.5, 12.5, 1, 10.5, 10.5};
        double[] P_FARM = {0, 9.5, 9.5, 0.3, 10.5, 10, 1, 7.5, 7.5};
        // 마스크 든 팔 (아래로 내림)
        flesh(h, 196, 166, 202, 196, 1, 0, P_FARM, SKIN, SKIN_S);
        flesh(h, 180, 132, 196, 166, 1, 0, P_UARM, sleeve, sleeveS);
        rect(h, c(0x8a6a4a), 186, 160, 28, 30);
        g2Cage(h, 186, 160, 28, 30);
        oval(h, SKIN_S, 188, 166, 20, 17);
        oval(h, SKIN, 189, 166, 17, 15);
        // 손을 드는 팔: 관절 각도로 움직인다 (길이가 변하지 않게)
        //  위팔: 아래로 늘어뜨림 → 옆 위로 약 45도,  아래팔: 아래 → 위로 세우고 머리 쪽으로 살짝 기울임
        double sx0 = 82, sy0 = 130, UA_U = 44, FA_U = 46;
        double aU = lerp(1.75, Math.PI * 2 - 2.62, raise);           // 1.75 → -2.62(=3.66)
        double aF = lerp(1.50, Math.PI * 2 - 1.42, easeOut(seg(u, 0.18, 0.75)));  // 아래팔은 조금 늦게 따라온다
        double ex = sx0 + Math.cos(aU) * UA_U, ey = sy0 + Math.sin(aU) * UA_U;
        double wx = ex + Math.cos(aF) * FA_U, wy = ey + Math.sin(aF) * FA_U;
        // 위팔 (뒤) → 소매 → 아래팔 (앞) 순서. 아래팔은 팔꿈치 쪽이 굵고 손목 쪽이 가늘다
        flesh(h, sx0, sy0, ex, ey, 0, -1, new double[]{0, 13, 13, 0.4, 13, 12.5, 1, 10.5, 10.5}, SKIN, SKIN_S);
        double mx = lerp(sx0, ex, 0.5), my = lerp(sy0, ey, 0.5);
        flesh(h, sx0, sy0, mx, my, 0, -1, new double[]{0, 14.5, 14.5, 1, 14, 14}, sleeve, sleeveS);
        oval(h, sleeve, 66, 116, 30, 28);                    // 어깨(삼각근)
        oval(h, SKIN_S, ex - 10.5, ey - 10.5, 21, 21);       // 팔꿈치
        flesh(h, ex, ey, wx, wy, -1, 0, new double[]{0, 10.5, 10.5, 0.3, 11, 10.5, 0.85, 8, 8, 1, 7.5, 7.5}, SKIN, SKIN_S);
        // 손: 검지를 세운 손 (손바닥 쪽에서, 엄지가 접힌 손가락 위를 가로지름)
        PixelHand.Pose point = new PixelHand.Pose();
        for (int i = 1; i < 4; i++) point.curl(i, 85, 100, 55);
        double ext = easeOut(seg(u, 0.6, 0.78));                  // 팔이 다 올라간 뒤에 검지를 편다
        point.curl(0, 85 * (1 - ext), 100 * (1 - ext), 55 * (1 - ext));
        point.spread = new double[]{-0.04, 0, 0.03, 0.07};
        point.thumb(0.95, 10, 40, 25);
        point.forearm = false;                                     // 팔은 위에서 그린 하나만
        double ang = Math.atan2(wy - ey, wx - ex) + Math.PI / 2;  // 아래팔 방향으로 손을 세운다
        double ux = (wx - ex) / FA_U, uy = (wy - ey) / FA_U;
        PixelHand.draw(L, h, point, wx - ux * 3, wy - uy * 3, 0.58, Math.PI - 0.35, 0.1, ang, null, null);
        // 귀, 얼굴
        oval(h, SKIN, 100, 64, 10, 18);
        oval(h, SKIN, 150, 64, 10, 18);
        Area face = new Area(new Ellipse2D.Double(104, 36, 52, 62));
        face.add(new Area(polyShape(106, 74, 154, 74, 150, 94, 138, 102, 122, 102, 110, 94)));
        h.setColor(SKIN);
        h.fill(face);
        h.clip(face);
        poly(h, SKIN_S, 144, 50, 160, 50, 160, 104, 134, 104);
        rect(h, SKIN_S, 100, 44, 60, 6);
        h.setClip(null);
        // 눈썹, 눈 (놀라서 크게 뜬 눈)
        line(h, c(0x3a2a20), 2, 112, 53, 126, 51);
        line(h, c(0x3a2a20), 2, 134, 51, 148, 53);
        rect(h, c(0xf0eee8), 114, 58, 11, 7);
        rect(h, c(0x1a1010), 118, 59, 4, 5);
        rect(h, c(0xf0eee8), 136, 58, 11, 7);
        rect(h, c(0x1a1010), 139, 59, 4, 5);
        // 코, 콧수염, 입
        line(h, SKIN_D, 2, 131, 64, 128, 76);
        poly(h, c(0x3a2a20), 118, 79, 142, 79, 146, 84, 130, 82, 114, 84);
        double open = u > 0.3 ? 0.5 + 0.5 * Math.abs(Math.sin(u * 14)) : 0.2;
        oval(h, c(0x6a1a1a), 123, 85, 15, 3 + 9 * open);
        // 모자
        h.setColor(BLACK);
        h.fill(new Arc2D.Double(100, 18, 60, 46, 0, 180, Arc2D.CHORD));
        poly(h, BLACK, 96, 40, 164, 40, 170, 47, 90, 47);
        line(h, BLACK_L, 1, 112, 26, 126, 22);
        // 땀
        if (u > 0.4) {
            oval(h, c(0xbfe3ff), 158, 52, 5, 8);
            oval(h, c(0xbfe3ff), 98, 58, 4, 6);
        }
    }

    static void g2Cage(Graphics2D h, double x, double y, double w, double hh) {
        h.setColor(BLACK);
        h.setStroke(new BasicStroke(2));
        h.draw(new RoundRectangle2D.Double(x, y, w, hh, 8, 8));
        for (int i = 1; i < 4; i++) h.draw(new Line2D.Double(x, y + i * hh / 4, x + w, y + i * hh / 4));
        h.draw(new Line2D.Double(x + w / 2, y, x + w / 2, y + hh));
    }

    // ------------------------------------------------------------------
    // 사람 몸 그리기 (옆모습) - 뼈대(관절 위치) 위에 근육 단면 두께를 입힌다
    // 비율: 머리 높이 14 기준 약 7.5두신. 어깨는 흉곽 맨 위, 목은 짧고 굵게.
    // ------------------------------------------------------------------

    static final class Athlete {
        int f = 1;                          // 1: 오른쪽을 봄, -1: 왼쪽을 봄
        double px, py, spine;               // 골반 위치, 척추 각도
        double[] farFoot, nearFoot;
        double[] farElbow, farHand, nearElbow, nearHand;
        Color jersey, jerseyS, pants, pantsS, sleeve, sleeveS, sock, sockS, cap, capS, trim;
        boolean helmet;
        Runnable farItem, midItem, nearItem;
    }

    /** 어깨 관절 위치: 척추 꼭대기에서 조금 아래, 조금 앞 */
    static double[] shoulderOf(double px, double py, double spine, int f) {
        double ux = Math.cos(spine), uy = Math.sin(spine), fx = -uy * f, fy = ux * f;
        return new double[]{px + ux * (TORSO - 3.5) + fx * 0.5, py + uy * (TORSO - 3.5) + fy * 0.5};
    }

    /**
     * 뼈를 따라 두께가 변하는 살 모양.
     * prof = {t, 앞쪽 두께, 뒤쪽 두께, t, 앞, 뒤, ...} (t: 뼈 시작 0 ~ 끝 1)
     */
    static Path2D boneShape(double x1, double y1, double x2, double y2, double fx, double fy, double[] prof, double sc) {
        double dx = x2 - x1, dy = y2 - y1, L = Math.hypot(dx, dy);
        if (L < 1e-3) { dx = 0; dy = 1; L = 1e-3; } else { dx /= L; dy /= L; }
        double nx = -dy, ny = dx;
        if (nx * fx + ny * fy < 0) { nx = -nx; ny = -ny; }
        int k = prof.length / 3;
        Path2D.Double p = new Path2D.Double();
        for (int i = 0; i < k; i++) {
            double t = prof[i * 3] * L, w = prof[i * 3 + 1] * sc;
            double x = x1 + dx * t + nx * w, y = y1 + dy * t + ny * w;
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        // 끝을 둥글게
        double we = (prof[(k - 1) * 3 + 1] + prof[(k - 1) * 3 + 2]) / 2 * sc;
        p.lineTo(x2 + dx * we * 0.6, y2 + dy * we * 0.6);
        for (int i = k - 1; i >= 0; i--) {
            double t = prof[i * 3] * L, w = prof[i * 3 + 2] * sc;
            p.lineTo(x1 + dx * t - nx * w, y1 + dy * t - ny * w);
        }
        double ws = (prof[1] + prof[2]) / 2 * sc;
        p.lineTo(x1 - dx * ws * 0.6, y1 - dy * ws * 0.6);
        p.closePath();
        return p;
    }

    /** 그늘 색으로 전체를 칠하고, 빛 쪽(왼쪽 위)으로 밀어 조금 작게 밝은 색을 덮는다 */
    static void flesh(Graphics2D g, double x1, double y1, double x2, double y2, double fx, double fy, double[] prof, Color c, Color shade) {
        g.setColor(shade);
        g.fill(boneShape(x1, y1, x2, y2, fx, fy, prof, 1));
        g.setColor(c);
        g.fill(boneShape(x1 - 0.8, y1 - 0.8, x2 - 0.8, y2 - 0.8, fx, fy, prof, 0.74));
    }

    static final double[] P_TORSO = {0, 6.0, 7.0, 0.25, 5.4, 5.8, 0.6, 8.0, 6.6, 0.85, 7.6, 6.6, 1.0, 4.6, 5.2};
    static final double[] P_PELVIS = {0, 6.4, 7.8, 1, 6.2, 7.0};
    static final double[] P_UPPERARM = {0, 4.3, 4.3, 0.35, 4.5, 4.0, 1, 3.0, 3.0};
    static final double[] P_FOREARM = {0, 3.2, 3.2, 0.3, 3.7, 3.4, 1, 2.3, 2.3};
    static final double[] P_THIGH = {0, 6.2, 6.4, 0.4, 5.8, 5.2, 1, 4.2, 4.2};
    static final double[] P_SHIN = {0, 4.0, 4.0, 0.3, 3.6, 5.0, 0.8, 2.8, 2.8, 1, 2.4, 2.4};
    static final double[] P_NECK = {0, 3.6, 3.6, 1, 3.2, 3.2};

    static void drawAthlete(Graphics2D h, Athlete A) {
        double ux = Math.cos(A.spine), uy = Math.sin(A.spine);
        double fx = -uy * A.f, fy = ux * A.f;                 // 몸 앞쪽
        double nx = A.px + ux * TORSO, ny = A.py + uy * TORSO;  // 목 밑
        double[] sh = shoulderOf(A.px, A.py, A.spine, A.f);

        // 1. 먼 다리, 먼 팔
        athLeg(h, A, A.px - fx * 0.5, A.py, A.farFoot, true);
        athArm(h, A, sh[0] + ux * 0.5 - fx * 1.5, sh[1] + uy * 0.5 - fy * 1.5, A.farElbow, A.farHand, true);
        if (A.farItem != null) A.farItem.run();

        // 2. 골반(바지), 몸통(상의), 벨트
        flesh(h, A.px - ux * 5, A.py - uy * 5, A.px + ux * 1, A.py + uy * 1, fx, fy, P_PELVIS, A.pants, A.pantsS);
        flesh(h, A.px + ux * 0.5, A.py + uy * 0.5, nx, ny, fx, fy, P_TORSO, A.jersey, A.jerseyS);
        h.setColor(A.trim);
        h.fill(boneShape(A.px - ux * 0.5, A.py - uy * 0.5, A.px + ux * 2, A.py + uy * 2, fx, fy, new double[]{0, 6.2, 7.2, 1, 5.9, 6.4}, 1));
        // 단추 줄 파이핑 (가슴 앞선을 따라)
        line(h, A.trim, 1, A.px + ux * 3 + fx * 5.5, A.py + uy * 3 + fy * 5.5, nx - ux * 3 + fx * 7, ny - uy * 3 + fy * 7);

        // 3. 목, 머리
        flesh(h, nx - ux, ny - uy, nx + ux * 4.5, ny + uy * 4.5, fx, fy, P_NECK, SKIN, SKIN_S);
        drawHeadSide(h, nx + ux * 10, ny + uy * 10, ux, uy, fx, fy, A);

        // 4. 가까운 다리
        athLeg(h, A, A.px + fx * 0.5, A.py, A.nearFoot, false);
        if (A.midItem != null) A.midItem.run();

        // 5. 가까운 팔
        athArm(h, A, sh[0], sh[1], A.nearElbow, A.nearHand, false);
        if (A.nearItem != null) A.nearItem.run();
    }

    static void drawHeadSide(Graphics2D h, double cx, double cy, double ux, double uy, double fx, double fy, Athlete A) {
        // 머리뼈 + 얼굴 옆선 (이마 → 코 → 입 → 턱)
        oval(h, SKIN_S, cx - fx * 0.5 - 6.8, cy - fy * 0.5 - 7.2, 13.6, 14.4);
        oval(h, SKIN, cx - fx * 0.2 - 6.2, cy - fy * 0.2 - 6.8, 12.4, 13);
        polyRel(h, SKIN, cx, cy, fx, fy, ux, uy, 4.5, 3.5, 6.0, 1.5, 7.4, -0.6, 6.0, -1.6, 6.2, -3.6, 5.6, -4.6, 4.6, -6.8, 1.0, -7.0, -1.5, -5.0, 0, 0);
        // 턱 아래 그늘, 귀, 눈, 눈썹, 입
        polyRel(h, SKIN_S, cx, cy, fx, fy, ux, uy, 4.6, -6.8, 1.0, -7.0, -1.5, -5.0, 1.5, -5.6);
        double[] ear = pt2(cx, cy, fx, fy, ux, uy, -1.2, -0.8);
        oval(h, SKIN_S, ear[0] - 1.8, ear[1] - 2.3, 3.6, 4.6);
        double[] eye = pt2(cx, cy, fx, fy, ux, uy, 4.2, 1.0);
        rect(h, c(0x1a1010), eye[0] - 0.8, eye[1] - 1, 1.8, 2);
        double[] b1 = pt2(cx, cy, fx, fy, ux, uy, 3.0, 2.8), b2 = pt2(cx, cy, fx, fy, ux, uy, 5.8, 2.6);
        line(h, c(0x3a2418), 1, b1[0], b1[1], b2[0], b2[1]);
        double[] m = pt2(cx, cy, fx, fy, ux, uy, 5.4, -3.4);
        rect(h, c(0x8a4a3a), m[0] - 1, m[1], 2, 1);
        // 모자 또는 헬멧
        double R = A.helmet ? 8.2 : 7.5, lift = A.helmet ? 0.8 : 1.5;
        double ccx = cx + ux * lift, ccy = cy + uy * lift;
        double end = A.helmet ? Math.PI * 1.22 : Math.PI;
        double[] dome = new double[2 * 18];
        for (int i = 0; i < 18; i++) {
            double th = end * i / 17;
            dome[i * 2] = ccx + R * (Math.cos(th) * fx + Math.sin(th) * ux);
            dome[i * 2 + 1] = ccy + R * (Math.cos(th) * fy + Math.sin(th) * uy);
        }
        poly(h, A.cap, dome);
        double[] hl1 = pt2(ccx, ccy, fx, fy, ux, uy, -1, R - 1.5), hl2 = pt2(ccx, ccy, fx, fy, ux, uy, 3, R - 2.2);
        line(h, A.helmet ? RED_L : NAVY_L, 1, hl1[0], hl1[1], hl2[0], hl2[1]);
        if (A.helmet) {   // 귀덮개
            double[] e = pt2(cx, cy, fx, fy, ux, uy, -1.0, -1.2);
            oval(h, A.cap, e[0] - 4.6, e[1] - 4.6, 9.2, 9.2);
            oval(h, A.capS, e[0] - 1.2, e[1] - 1.2, 2.4, 2.4);
        }
        // 챙
        double brim = A.helmet ? 10.5 : 12.5;
        polyRel(h, A.capS, ccx, ccy, fx, fy, ux, uy, 4.5, 0.6, brim, -0.4, brim - 0.5, -2.2, 4.5, -1.6);
    }

    static double[] pt2(double cx, double cy, double fx, double fy, double ux, double uy, double a, double b) {
        return new double[]{cx + fx * a + ux * b, cy + fy * a + uy * b};
    }

    /** (앞으로 a, 위로 b) 좌표 쌍들로 다각형을 그린다 */
    static void polyRel(Graphics2D g, Color c, double cx, double cy, double fx, double fy, double ux, double uy, double... ab) {
        double[] p = new double[ab.length];
        for (int i = 0; i < ab.length; i += 2) {
            p[i] = cx + fx * ab[i] + ux * ab[i + 1];
            p[i + 1] = cy + fy * ab[i] + uy * ab[i + 1];
        }
        poly(g, c, p);
    }

    static void athArm(Graphics2D h, Athlete A, double sx, double sy, double[] e, double[] hd, boolean far) {
        double ux = Math.cos(A.spine), uy = Math.sin(A.spine), fx = -uy * A.f, fy = ux * A.f;
        Color sl = far ? darker(A.sleeve) : A.sleeve, slS = far ? darker(A.sleeveS) : A.sleeveS;
        Color je = far ? darker(A.jersey) : A.jersey, jeS = far ? darker(A.jerseyS) : A.jerseyS;
        Color sk = far ? SKIN_S : SKIN, skS = far ? SKIN_D : SKIN_S;
        flesh(h, e[0], e[1], hd[0], hd[1], fx, fy, P_FOREARM, sl, slS);
        flesh(h, sx, sy, e[0], e[1], fx, fy, P_UPPERARM, sl, slS);
        // 반팔 소매 (위팔의 40%)
        double mx = lerp(sx, e[0], 0.42), my = lerp(sy, e[1], 0.42);
        flesh(h, sx, sy, mx, my, fx, fy, new double[]{0, 4.8, 4.8, 1, 4.6, 4.6}, je, jeS);
        oval(h, sk, hd[0] - 3.2, hd[1] - 3.2, 6.4, 6.4);
        oval(h, skS, hd[0] - 1.5 + fx, hd[1] - 1.5 + fy, 3, 3);
    }

    static void athLeg(Graphics2D h, Athlete A, double hx, double hy, double[] foot, boolean far) {
        double[] k = ik(hx, hy, foot[0], foot[1], THIGH, SHIN, A.f);
        double dx = foot[0] - k[0], dy = foot[1] - k[1], l = Math.hypot(dx, dy);
        double ax = k[0] + dx / l * SHIN, ay = k[1] + dy / l * SHIN;
        double fx = A.f, fy = 0;
        Color pa = far ? darker(A.pants) : A.pants, paS = far ? darker(A.pantsS) : A.pantsS;
        Color so = far ? darker(A.sock) : A.sock, soS = far ? darker(A.sockS) : A.sockS;
        flesh(h, k[0], k[1], ax, ay, fx, fy, P_SHIN, so, soS);
        flesh(h, hx, hy, k[0], k[1], fx, fy, P_THIGH, pa, paS);
        // 무릎 아래까지 오는 야구 바지
        double bx = lerp(k[0], ax, 0.38), by = lerp(k[1], ay, 0.38);
        flesh(h, k[0], k[1], bx, by, fx, fy, new double[]{0, 4.6, 4.6, 1, 4.4, 5.2}, pa, paS);
        // 스파이크
        Color shoe = far ? c(0x0c0c12) : BLACK;
        int f = A.f;
        poly(h, shoe, ax - f * 3.5, ay - 3, ax + f * 4.5, ay - 3.6, ax + f * 9.5, ay + 0.2, ax + f * 9, ay + 1.2, ax - f * 4, ay + 1.2);
        rect(h, far ? c(0x2a2a33) : c(0x55555f), Math.min(ax - f * 4, ax + f * 9), ay + 0.6, 13, 0.8);
    }

    static Color darker(Color c) {
        return new Color((int) (c.getRed() * 0.78), (int) (c.getGreen() * 0.78), (int) (c.getBlue() * 0.82));
    }

    // ------------------------------------------------------------------
    // 배경 도구
    // ------------------------------------------------------------------

    static void lightTower(Graphics2D g, double x, double y, double s, double t) {
        for (int i = 4; i >= 1; i--) {
            double r = s * (8 + i * 8);
            oval(g, ca(0xfff2c0, 18), x - r, y - r * 0.7, r * 2, r * 1.4);
        }
        rect(g, c(0x2b3047), x - s, y, 2 * s, 200);
        rect(g, c(0x3a4060), x - 10 * s, y - 6 * s, 20 * s, 12 * s);
        for (int i = 0; i < 5; i++)
            for (int j = 0; j < 3; j++)
                rect(g, (i + j) % 2 == 0 ? c(0xffffff) : c(0xfff0b0), x - 9 * s + i * 4 * s, y - 5 * s + j * 4 * s, 3 * s, 3 * s);
    }

    static void bigTower(Graphics2D g, double x, double y, double t) {
        for (int i = 6; i >= 1; i--) {
            double r = 14 + i * 15;
            oval(g, ca(0xfff2c0, 16), x - r, y - r * 0.8, r * 2, r * 1.6);
        }
        line(g, ca(0xbcd4ff, 70), 1, x - 160, y, x + 160, y);
        rect(g, c(0x262b44), x - 2, y + 10, 4, H);
        rect(g, c(0x343a5c), x - 23, y - 11, 46, 22);
        for (int i = 0; i < 10; i++)
            for (int j = 0; j < 4; j++) {
                boolean f = (hash(i, j, (int) (t * 4)) & 15) == 0;
                rect(g, f ? c(0xfff0b0) : c(0xffffff), x - 21 + i * 4.3, y - 9 + j * 4.7, 3, 3);
            }
        g.setColor(ca(0xfff2c0, 60));
        g.setStroke(new BasicStroke(1));
        g.draw(new Ellipse2D.Double(x + 34, y + 22, 16, 16));
        g.draw(new Ellipse2D.Double(x + 58, y + 38, 8, 8));
    }

    static void crowd(BufferedImage img, Graphics2D g, int y0, int y1, int seed, double t, int base) {
        if (y0 >= H) return;
        rect(g, c(base), 0, y0, W, y1 - y0);
        int[] cols = {0x3a4270, 0x5a4a6a, 0x6b5f45, 0x2e5a6a, 0x7a3b3b, 0x4a5a3a, 0x8a8a9a};
        for (int y = y0 + 1; y < Math.min(H, y1) - 1; y += 3)
            for (int x = (y / 3) % 2; x < W; x += 2) {
                int h = hash(x, y, seed);
                if ((h & 3) == 0) continue;
                px(img, x, y, cols[(h >>> 3) % cols.length]);
                if (((h >>> 8) & 7) == 0) px(img, x, y + 1, 0x20243f);
            }
        int fr = (int) (t * 10);
        for (int i = 0; i < 14; i++) {
            int h = hash(i, fr, seed);
            if (((h >>> 20) & 3) != 0) continue;
            int x = (h >>> 1) % W, y = y0 + (h >>> 12) % Math.max(1, y1 - y0);
            px(img, x, y, 0xffffff);
            px(img, x + 1, y, 0xfff6c8);
            px(img, x - 1, y, 0xfff6c8);
            px(img, x, y - 1, 0xfff6c8);
            px(img, x, y + 1, 0xfff6c8);
        }
    }

    static void bokeh(Graphics2D g, int seed, double t, int count) {
        Random r = new Random(seed);
        for (int i = 0; i < count; i++) {
            double x = r.nextInt(W), y = r.nextInt(H);
            int rad = 6 + r.nextInt(18);
            int col = r.nextBoolean() ? 0xffe9a8 : 0xbcd4ff;
            double fl = 0.6 + 0.4 * Math.sin(t * 3 + i);
            int a = (int) (36 * fl) + 8;
            oval(g, ca(col, a), x - rad, y - rad, rad * 2, rad * 2);
            oval(g, ca(col, a), x - rad * 0.6, y - rad * 0.6, rad * 1.2, rad * 1.2);
        }
    }

    static void stars(BufferedImage img, int yMax, double t, int seed) {
        Random r = new Random(seed);
        for (int i = 0; i < 40; i++) {
            int x = r.nextInt(W), y = r.nextInt(Math.max(1, yMax));
            boolean tw = ((int) (t * 6) + i) % 7 == 0;
            px(img, x, y, tw ? 0xffffff : 0x8a93c4);
        }
    }

    static void speedLines(Graphics2D g, double cx, double cy, double k, double t) {
        if (k <= 0.02) return;
        Random r = new Random(31 + (int) (t * 20));
        for (int i = 0; i < 40; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double r1 = 50 + r.nextDouble() * 60 * (1 - k) + 10;
            double len = 20 + k * 140 * r.nextDouble();
            line(g, ca(0xffffff, (int) (50 + 130 * k)), 1, cx + Math.cos(a) * r1, cy + Math.sin(a) * r1 * 0.8,
                    cx + Math.cos(a) * (r1 + len), cy + Math.sin(a) * (r1 + len) * 0.8);
        }
    }

    static void sparkle(BufferedImage img, int x, int y, int len) {
        for (int i = -len; i <= len; i++) {
            int col = Math.abs(i) <= 1 ? 0xffffff : 0xfff3b0;
            px(img, x + i, y, col);
            px(img, x, y + i, col);
        }
    }

    static void star(Graphics2D g, double cx, double cy, int n, double R, double r, Color c) {
        double[] p = new double[n * 4];
        for (int i = 0; i < n * 2; i++) {
            double a = Math.PI * i / n;
            double rr = i % 2 == 0 ? R : r;
            p[i * 2] = cx + Math.cos(a) * rr;
            p[i * 2 + 1] = cy + Math.sin(a) * rr;
        }
        poly(g, c, p);
    }

    // ------------------------------------------------------------------
    // 야구공: 픽셀마다 명암을 4단계로 나누고, 구 위의 실밥 곡선을 찍는다
    // ------------------------------------------------------------------

    static void ball(BufferedImage img, double cx, double cy, double r, double spin, double tilt) {
        if (r < 1.6) {
            px(img, (int) cx, (int) cy, 0xf4f1e8);
            return;
        }
        double lx = -0.45, ly = -0.6, lz = 0.66;
        double ln = Math.sqrt(lx * lx + ly * ly + lz * lz);
        lx /= ln; ly /= ln; lz /= ln;
        double edge = Math.max(0, 1 - 1.3 / r);
        for (int y = (int) Math.floor(cy - r - 1); y <= (int) Math.ceil(cy + r + 1); y++)
            for (int x = (int) Math.floor(cx - r - 1); x <= (int) Math.ceil(cx + r + 1); x++) {
                double dx = (x + 0.5 - cx) / r, dy = (y + 0.5 - cy) / r, d2 = dx * dx + dy * dy;
                if (d2 > 1) continue;
                int col;
                if (d2 > edge * edge) col = 0x2a2230;
                else {
                    double nz = Math.sqrt(1 - d2), d = dx * lx + dy * ly + nz * lz;
                    col = d > 0.8 ? 0xffffff : d > 0.45 ? 0xf1ecdf : d > 0.1 ? 0xd3cab5 : 0xa59c86;
                }
                px(img, x, y, col);
            }
        if (r < 4) return;
        double a = 0.62, b = 0.38, k = 2 * Math.sqrt(a * b);
        int n = (int) (r * 16) + 60;
        double cs = Math.cos(spin), sn = Math.sin(spin), ct = Math.cos(tilt), st = Math.sin(tilt);
        int stitch = Math.max(3, (int) (n / (r * 1.3)));
        double prevx = Double.NaN, prevy = 0, rr = r - 1.6;
        for (int i = 0; i <= n; i++) {
            double tt = 2 * Math.PI * i / n;
            double x = a * Math.cos(tt) + b * Math.cos(3 * tt), y = a * Math.sin(tt) - b * Math.sin(3 * tt), z = k * Math.sin(2 * tt);
            double x2 = x * cs + z * sn, z2 = -x * sn + z * cs;
            double y3 = y * ct - z2 * st, z3 = y * st + z2 * ct;
            if (z3 < 0.12) { prevx = Double.NaN; continue; }
            double sx = cx + x2 * rr, sy = cy + y3 * rr;
            if (r > 10 && i % stitch == 0 && !Double.isNaN(prevx)) {
                double tx = sx - prevx, ty = sy - prevy, tl = Math.hypot(tx, ty);
                if (tl > 0) {
                    double nx = -ty / tl, ny = tx / tl, len = Math.max(1.5, r / 13);
                    for (double s = -len; s <= len; s += 0.6) px(img, (int) (sx + nx * s), (int) (sy + ny * s), 0x8e1a1d);
                }
            }
            px(img, (int) sx, (int) sy, 0xc8282b);
            if (r > 22) px(img, (int) sx + 1, (int) sy, 0xc8282b);
            prevx = sx; prevy = sy;
        }
    }

    // ------------------------------------------------------------------
    // 도트 글자 (5x7)
    // ------------------------------------------------------------------

    private static final Map<Character, String[]> GL = new HashMap<>();
    static {
        GL.put('B', new String[]{"11110", "10001", "10001", "11110", "10001", "10001", "11110"});
        GL.put('A', new String[]{"01110", "10001", "10001", "11111", "10001", "10001", "10001"});
        GL.put('S', new String[]{"01111", "10000", "10000", "01110", "00001", "00001", "11110"});
        GL.put('E', new String[]{"11111", "10000", "10000", "11110", "10000", "10000", "11111"});
        GL.put('U', new String[]{"10001", "10001", "10001", "10001", "10001", "10001", "01110"});
        GL.put('P', new String[]{"11110", "10001", "10001", "11110", "10000", "10000", "10000"});
        GL.put('1', new String[]{"00100", "01100", "00100", "00100", "00100", "00100", "01110"});
        GL.put('?', new String[]{"01110", "10001", "00001", "00010", "00100", "00000", "00100"});
        GL.put('!', new String[]{"00100", "00100", "00100", "00100", "00100", "00000", "00100"});
    }

    static void glyphs(Graphics2D g, String s, double x, double y, int sc, Color c) {
        g.setColor(c);
        double cx = x;
        for (char ch : s.toCharArray()) {
            String[] rows = GL.get(ch);
            if (rows != null)
                for (int j = 0; j < 7; j++)
                    for (int i = 0; i < 5; i++)
                        if (rows[j].charAt(i) == '1') g.fill(new Rectangle2D.Double(cx + i * sc, y + j * sc, sc, sc));
            cx += 6 * sc;
        }
    }

    private static String family;

    static Font kfont(int style, int size) {
        if (family == null) {
            Set<String> have = new HashSet<>(Arrays.asList(
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
            family = "Dialog";
            for (String f : new String[]{"KBO Dia Gothic", "KBO Dia Gothic_bold", "Malgun Gothic", "맑은 고딕", "Noto Sans CJK KR", "NanumGothic"})
                if (have.contains(f)) { family = f; break; }
        }
        return new Font(family, style, size);
    }

    static void textOutlined(Graphics2D g, String s, double x, double y, Font f, Color fill, Color out, int w) {
        g.setFont(f);
        g.setColor(out);
        for (int dx = -w; dx <= w; dx++)
            for (int dy = -w; dy <= w; dy++)
                if (dx != 0 || dy != 0) g.drawString(s, (float) (x + dx), (float) (y + dy));
        g.setColor(fill);
        g.drawString(s, (float) x, (float) y);
    }

    // ------------------------------------------------------------------
    // 그리기 도구
    // ------------------------------------------------------------------

    static final int[] BAYER = {0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};

    /** 세로 그라데이션을 4x4 디더링으로 (레트로 느낌) */
    static void vgrad(BufferedImage img, int y0, int y1, int... stops) {
        int n = stops.length - 1;
        for (int y = Math.max(0, y0); y < Math.min(img.getHeight(), y1); y++) {
            double f = (y - y0) / (double) Math.max(1, y1 - y0 - 1) * n;
            int i = Math.min(n - 1, (int) f);
            double fr = f - i;
            for (int x = 0; x < img.getWidth(); x++) {
                double th = (BAYER[(y & 3) * 4 + (x & 3)] + 0.5) / 16.0;
                img.setRGB(x, y, fr > th ? stops[i + 1] : stops[i]);
            }
        }
    }

    /**
     * 캐릭터 레이어 마무리:
     * 1) 둘레에 몸 색을 어둡게 한 외곽선 (검정 일색이 아니라 색이 살아 있는 외곽선)
     * 2) 빛이 오는 쪽(lx, ly) 가장자리에 림 라이트
     * 3) 역광 장면이면 몸 전체를 조금 어둡게(dark)
     */
    static void shadePass(BufferedImage L, int lx, int ly, int rim, double rimAmt, double dark) {
        int w = L.getWidth(), h = L.getHeight();
        int[] p = L.getRGB(0, 0, w, h, null, 0, w);
        int[] o = new int[p.length];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                if ((p[i] >>> 24) != 0) {
                    int col = p[i] & 0xffffff;
                    if (dark > 0) col = mix(col, 0x0a0c1e, dark);
                    int nx = x + lx, ny = y + ly, nx2 = x + 2 * lx, ny2 = y + 2 * ly;
                    boolean lit = nx >= 0 && ny >= 0 && nx < w && ny < h && (p[ny * w + nx] >>> 24) == 0;
                    boolean lit2 = !lit && nx2 >= 0 && ny2 >= 0 && nx2 < w && ny2 < h && (p[ny2 * w + nx2] >>> 24) == 0;
                    if (lit) col = mix(col, rim, rimAmt);
                    else if (lit2) col = mix(col, rim, rimAmt * 0.4);
                    o[i] = 0xff000000 | col;
                } else {
                    int n = -1;
                    if (x > 0 && (p[i - 1] >>> 24) != 0) n = p[i - 1];
                    else if (x < w - 1 && (p[i + 1] >>> 24) != 0) n = p[i + 1];
                    else if (y > 0 && (p[i - w] >>> 24) != 0) n = p[i - w];
                    else if (y < h - 1 && (p[i + w] >>> 24) != 0) n = p[i + w];
                    if (n != -1) o[i] = 0xff000000 | mix(mix(n & 0xffffff, 0, 0.7), 0x0b0a18, 0.35);
                }
            }
        L.setRGB(0, 0, w, h, o, 0, w);
    }

    static int mix(int a, int b, double f) {
        int r = (int) (((a >> 16) & 255) * (1 - f) + ((b >> 16) & 255) * f);
        int g = (int) (((a >> 8) & 255) * (1 - f) + ((b >> 8) & 255) * f);
        int bl = (int) ((a & 255) * (1 - f) + (b & 255) * f);
        return (r << 16) | (g << 8) | bl;
    }

    /** 굵기가 변하는 캡슐 (팔·다리 한 마디) */
    static void limb(Graphics2D g, double x1, double y1, double x2, double y2, double w1, double w2, Color c) {
        double dx = x2 - x1, dy = y2 - y1, l = Math.hypot(dx, dy);
        if (l > 0.01) {
            double nx = -dy / l, ny = dx / l;
            poly(g, c, x1 + nx * w1 / 2, y1 + ny * w1 / 2, x2 + nx * w2 / 2, y2 + ny * w2 / 2,
                    x2 - nx * w2 / 2, y2 - ny * w2 / 2, x1 - nx * w1 / 2, y1 - ny * w1 / 2);
        }
        oval(g, c, x1 - w1 / 2, y1 - w1 / 2, w1, w1);
        oval(g, c, x2 - w2 / 2, y2 - w2 / 2, w2, w2);
    }

    /** 그늘 색으로 깐 뒤, 빛 쪽(왼쪽 위)으로 조금 밀어서 밝은 색을 덮는다 */
    static void limbShaded(Graphics2D g, double x1, double y1, double x2, double y2, double w1, double w2, Color c, Color shade) {
        limb(g, x1, y1, x2, y2, w1, w2, shade);
        double o = Math.min(w1, w2) * 0.18;
        limb(g, x1 - o, y1 - o, x2 - o, y2 - o, w1 * 0.72, w2 * 0.72, c);
    }

    /** 키프레임 여러 채널을 부드럽게 보간 */
    static double[] kfa(double s, double[] ts, double[][] vals) {
        if (s <= ts[0]) return vals[0];
        for (int i = 1; i < ts.length; i++)
            if (s <= ts[i]) {
                double f = easeInOut((s - ts[i - 1]) / (ts[i] - ts[i - 1]));
                double[] r = new double[vals[i].length];
                for (int k = 0; k < r.length; k++) r[k] = lerp(vals[i - 1][k], vals[i][k], f);
                return r;
            }
        return vals[vals.length - 1];
    }

    static double kf1(double s, double[] ts, double[] vals) {
        if (s <= ts[0]) return vals[0];
        for (int i = 1; i < ts.length; i++)
            if (s <= ts[i]) return lerp(vals[i - 1], vals[i], easeInOut((s - ts[i - 1]) / (ts[i] - ts[i - 1])));
        return vals[vals.length - 1];
    }

    static void cover(Graphics2D g, int rgb, double a) {
        rect(g, ca(rgb, (int) (255 * clamp(a))), 0, 0, W, H);
    }

    static BufferedImage layer() { return new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB); }

    static Graphics2D pg(BufferedImage b) {
        Graphics2D g = b.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        return g;
    }

    static void px(BufferedImage img, int x, int y, int rgb) {
        if (x >= 0 && y >= 0 && x < img.getWidth() && y < img.getHeight()) img.setRGB(x, y, 0xff000000 | rgb);
    }

    static void rect(Graphics2D g, Color c, double x, double y, double w, double h) {
        g.setColor(c);
        g.fill(new Rectangle2D.Double(x, y, w, h));
    }

    static void oval(Graphics2D g, Color c, double x, double y, double w, double h) {
        g.setColor(c);
        g.fill(new Ellipse2D.Double(x, y, w, h));
    }

    static Path2D polyShape(double... p) {
        Path2D.Double s = new Path2D.Double();
        s.moveTo(p[0], p[1]);
        for (int i = 2; i < p.length; i += 2) s.lineTo(p[i], p[i + 1]);
        s.closePath();
        return s;
    }

    static void poly(Graphics2D g, Color c, double... p) {
        g.setColor(c);
        g.fill(polyShape(p));
    }

    static void line(Graphics2D g, Color c, float w, double x1, double y1, double x2, double y2) {
        g.setColor(c);
        g.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    static void curve(Graphics2D g, Color c, float w, double x1, double y1, double qx, double qy, double x2, double y2) {
        g.setColor(c);
        g.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new QuadCurve2D.Double(x1, y1, qx, qy, x2, y2));
    }

    static double[] kf(double u, double[] ts, double[][] ps) {
        if (u <= ts[0]) return ps[0];
        for (int i = 1; i < ts.length; i++)
            if (u <= ts[i]) {
                double f = easeInOut((u - ts[i - 1]) / (ts[i] - ts[i - 1]));
                return new double[]{lerp(ps[i - 1][0], ps[i][0], f), lerp(ps[i - 1][1], ps[i][1], f)};
            }
        return ps[ps.length - 1];
    }

    static double clamp(double v) { return v < 0 ? 0 : v > 1 ? 1 : v; }
    static double seg(double t, double a, double b) { return clamp((t - a) / (b - a)); }
    static double lerp(double a, double b, double f) { return a + (b - a) * f; }
    static double easeOut(double x) { return 1 - Math.pow(1 - x, 3); }
    static double easeIn(double x) { return x * x * x; }
    static double easeInOut(double x) { return x < .5 ? 4 * x * x * x : 1 - Math.pow(-2 * x + 2, 3) / 2; }
    static double easeOutBack(double x) {
        double c1 = 1.70158, c3 = c1 + 1;
        return 1 + c3 * Math.pow(x - 1, 3) + c1 * Math.pow(x - 1, 2);
    }
    static double bounce(double x) {
        double n1 = 7.5625, d1 = 2.75;
        if (x < 1 / d1) return n1 * x * x;
        if (x < 2 / d1) { x -= 1.5 / d1; return n1 * x * x + 0.75; }
        if (x < 2.5 / d1) { x -= 2.25 / d1; return n1 * x * x + 0.9375; }
        x -= 2.625 / d1;
        return n1 * x * x + 0.984375;
    }
    static int hash(int a, int b, int c) {
        int h = a * 374761393 + b * 668265263 + c * 1274126177;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }

    static Color c(int rgb) { return new Color(rgb); }
    static Color ca(int rgb, int a) { return new Color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, Math.max(0, Math.min(255, a))); }

    // 색 (BASEUP 홈팀: 남색·흰색·빨강 / 원정 타자: 회색·빨강 / 주심: 검정·남색·회색)
    static final Color OUT = c(0x141020);
    static final Color SKIN = c(0xf1c27d), SKIN_S = c(0xd49a5a), SKIN_D = c(0xa86b3c), SKIN_L = c(0xf8d9a8);
    static final Color NAVY = c(0x1b2a5e), NAVY_L = c(0x2f4488), NAVY_D = c(0x111a3d);
    static final Color WHITE = c(0xf4f4f0), WHITE_S = c(0xc9cdd6);
    static final Color RED = c(0xc8282b), RED_D = c(0x8e1a1d), RED_L = c(0xe8524f);
    static final Color GRAY = c(0xa7a9ad), GRAY_S = c(0x7d8087), GRAY_D = c(0x5a5d66);
    static final Color WOOD = c(0xd9a35b), WOOD_S = c(0xa8742f), WOOD_L = c(0xf2c98a);
    static final Color LEATHER = c(0x8b5a2b), LEATHER_D = c(0x5e3a1a);
    static final Color BLACK = c(0x1a1a22), BLACK_L = c(0x2e3140);
    static final Color UMP = c(0x1c2233), UMP_L = c(0x2c3550);
    static final Color GRASS = c(0x2f7d32), GRASS_L = c(0x3a8f3c), GRASS_D = c(0x23602a);
    static final Color DIRT = c(0xb07a45), DIRT_L = c(0xc98d55), DIRT_D = c(0x8a5c32);
    static final Color YELLOW = c(0xffd23f);
    static final Color WALL = c(0x1f4f3a), WALL_D = c(0x163a2b);

    /** 혼자 실행해서 확인할 때: java baseup.ui.IntroAnimation */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("BASEUP 인트로");
            IntroAnimation intro = new IntroAnimation(f::dispose);
            f.setContentPane(intro);
            f.setSize(1280, 760);
            f.setLocationRelativeTo(null);
            f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            f.setVisible(true);
            intro.start();
        });
    }
}
