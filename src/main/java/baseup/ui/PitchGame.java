package baseup.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import baseup.game.GameState;

/**
 * 메인 화면 미니게임: 옆에서 보는 레트로 도트 타격 게임.
 *
 * 왼쪽에 타자(+포수, 주심), 오른쪽 마운드에 투수가 같은 높이에 서 있다.
 * 배경 위쪽은 야구장 사진(data/images/stadium_night.jpg)을 촘촘한 도트(2배 해상도)로 바꿔 쓰고,
 * 땅과 선수는 굵은 도트로 직접 그린다. 그림 가로 길이(cw)는 화면 비율에 맞춰 바뀌어서
 * 창이 넓든 좁든 빈 공간 없이 채워진다.
 *
 * 판정 규칙 (실제 야구 규칙을 단순하게)
 *  - 존(무릎~가슴) 안의 공을 안 치면 스트라이크, 존 밖의 공을 안 치면 볼
 *  - 헛스윙은 스트라이크, 파울은 2스트라이크 전까지만 스트라이크
 *  - 3스트라이크 = 삼진, 4볼 = 볼넷, 3아웃 = 공수 교대 → 게임이 멈추고 규칙 공부를 권한다
 */
final class PitchGame extends JComponent {
    // ---------- 1. 장면 좌표 (굵은 도트 1칸 기준) ----------
    static final int GH = 96;                      // 그림 세로 칸 수 (고정)
    private static final int SCENE = 250;          // 타자~투수가 들어가는 가운데 장면 폭
    private static final int WALL = 50;            // 외야 담장 위쪽 (여기 위는 사진 배경)
    private static final int GROUND = 84;          // 땅 높이 (발이 닿는 곳)
    private static final int PLATE = 66;           // 홈플레이트 x
    private static final int MOUND = 214;          // 투수판 x
    private static final int ZONE_TOP = 69, ZONE_BOTTOM = 79;   // 스트라이크 존 (가슴~무릎)
    private static final double SWING_TIME = 0.24, CONTACT_AT = 0.55;   // 스윙 길이(초), 공 맞는 순간(비율)

    private static final Color OUT = new Color(0x141820), SKIN = new Color(0xE9B48A), UNI = new Color(0xF4F5F7),
            PANTS = new Color(0xD3D8E0), GLOVE = new Color(0x9A5B2B), BAT = new Color(0xE0B070), BAT_DARK = new Color(0x7A4E24),
            NAVY = new Color(0x263552), UMP = new Color(0x2B2F38), GREY = new Color(0x8D95A3);

    // ---------- 2. 도트 캐릭터 (k 테두리, s 피부, w 상의, r 팀색, n 바지, g 글러브, p 보호대, d 심판복, q 회색바지) ----------
    private static final String[] BATTER_TOP = {
            "....kkkk......", "...krrrrk.....", "..krrrrrrk....", "..krrrrrkkk...", "...kssssk.....", "...ksssk......",
            "....kssk......", "...kwwwwk.....", "..kwwwwwwk....", "..kwwrwwwk....", "..kwwwwwwk....", "...kwwwwk.....",
            "...krrrrk....."};
    private static final String[] BATTER_LEGS_STANCE = {
            "...knnnnk.....", "..knnnnnnk....", "..knnk.knnk...", ".knnk...knnk..", ".knnk...knnk..", ".knnk....knk..",
            ".krrk....krk..", ".krrk....krk..", "kkkk.....kkkk."};
    private static final String[] BATTER_LEGS_SWING = {
            "...knnnnk.....", "...knnnnnk....", "...knnknnnk...", "..knnk..knnk..", "..knk....knnk.", ".knk......knk.",
            ".krk......krk.", "kkk.......krk.", "..........kkkk"};
    private static final String[] PITCHER_TOP = {
            "......kkkk....", ".....krrrrk...", "....krrrrrrk..", "...kkkrrrrrk..", ".....kssssk...", "......ksssk...",
            "......kssk....", ".....kwwwwk...", "....kwwwwwwk..", "....kwwrwwwk..", "....kwwwwwwk..", ".....kwwwwk...",
            ".....krrrrk..."};
    private static final String[] PITCHER_LEGS_SET = {
            ".....knnnnk...", "....knnnnnnk..", "....knnk.knnk.", "...knnk...knnk", "...knnk...knnk", "...knk.....knk",
            "...krk.....krk", "...krk.....krk", "..kkkk....kkkk"};
    private static final String[] PITCHER_LEGS_KICK = {
            ".....knnnnk...", "..knnnnnnnk...", ".knnnnk.knk...", ".kk.knk..knk..", "....krk..knk..", "....kkk..knk..",
            ".........krk..", ".........krk..", "........kkkk.."};
    private static final String[] PITCHER_LEGS_STRIDE = {
            ".....knnnnk...", "....knnnnnnk..", "...knnk..knnk.", "..knnk....knnk", ".knnk......knk", "knnk.......knk",
            "krk.........kk", "krk...........", "kkkk.........."};
    private static final String[] CATCHER = {
            "....kkkk.....", "...kppppk....", "...kpskkk....", "...kppppk....", "..kppppppk...", "..kppppppkgg.",
            "..kppppppkggg", "..kwwwwwwk.gg", ".knnnnnnnk...", ".knnk.knnk...", "knnk...knnk..", "kpk.....kpk..", "kkk.....kkk.."};
    private static final String[] UMPIRE = {
            "...kkkk...", "..kddddk..", "..kdsddk..", "...kssk...", "..kddddk..", ".kddddddk.", ".kddddddk.",
            ".kddddddk.", "..kqqqqk..", "..kqqqqk..", ".kqqk.kqqk", ".kqk...kqk", ".kqk...kqk", ".kkk...kkk"};

    // ---------- 3. 화면 상태 (규칙과 기록은 GameState가 맡는다) ----------
    enum Phase { IDLE, WINDUP, PITCH, RESULT, BREAK }
    private Phase phase = Phase.IDLE;
    private boolean paused;
    private double phaseTime, travel, startY, targetY, curve;
    private int speedKmh; private String pitchName = "";
    private boolean beanball;                            // 몸 쪽으로 날아오는 공 (몸에 맞는 공 상황)
    private double swingAt = -1;                         // 스윙을 시작한 시각(이번 투구 기준)
    private boolean contact;                             // 배트에 맞았는지
    private double burst = -1, shake = -1;               // 타격 불꽃, 홈런 화면 흔들림
    private String distance = "";
    // 투구가 끝난 뒤 공의 움직임 (중력 + 바운드 + 굴러감)
    private boolean live; private double bx, by, bvx, bvy, catchAt = -1; private boolean overWall;
    private static final double GRAVITY = 170;           // 칸/초²

    private final GameState state;                       // 카운트·주자·점수·판정
    String headline = "클릭해서 플레이 볼!", tip = "공이 타자 앞 점선 존(무릎~가슴)에 들어올 때 클릭하거나 스페이스바로 스윙하세요.";
    private Color headlineColor = Ballpark.LAMP;

    private final Color team;
    private final Random random = new Random();          // 구종·코스·비거리 (화면 연출용)
    private final Timer timer = new Timer(16, e -> tick());
    private long lastNanos;
    private final List<Runnable> listeners = new ArrayList<>();
    private BufferedImage field; private int fieldWidth;
    private BufferedImage frame;                         // 매 프레임 다시 쓰는 그림 (새로 만들지 않음)
    private static BufferedImage photo; private static boolean photoTried;

    /** state를 밖(MainFrame)에서 받아서, 메인 화면을 다시 만들어도 경기가 이어진다 */
    PitchGame(Color teamColor, GameState state) {
        this.team = teamColor == null ? new Color(0xD7263D) : teamColor;
        this.state = state;
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusable(true);
        addMouseListener(new MouseAdapter() { @Override public void mousePressed(MouseEvent e) { requestFocusInWindow(); click(); } });
        // 스페이스바로도 스윙 (창 어디에 포커스가 있든)
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("SPACE"), "swing");
        getActionMap().put("swing", new AbstractAction() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { click(); } });
        if (state.inningOver()) startBreak();                               // 3아웃 휴식 중에 나갔다 왔으면 그대로
        else if (!state.fresh()) { headline = "이어서 하기!"; tip = state.inning() + "회 · " + state.outs() + "아웃 · " + state.balls() + "볼 " + state.strikes() + "스트라이크에서 계속해요. 클릭하면 시작!"; }
    }

    void addListener(Runnable r) { listeners.add(r); }
    private void changed() { listeners.forEach(Runnable::run); }
    GameState state() { return state; }
    List<String> metRules() { return state.metRules(); }

    @Override public void addNotify() { super.addNotify(); lastNanos = System.nanoTime(); timer.start(); }
    @Override public void removeNotify() { timer.stop(); super.removeNotify(); }

    @Override public Dimension getPreferredSize() {
        int w = getWidth() > 0 ? getWidth() : 900;
        return new Dimension(w, Math.max(300, Math.min(620, w * GH / SCENE)));
    }

    // ---------- 4. 버튼에서 부르는 기능 ----------
    boolean paused() { return paused || phase == Phase.BREAK || phase == Phase.IDLE; }
    boolean inningOver() { return phase == Phase.BREAK; }
    /** 조작 버튼에 보일 글자 */
    String controlLabel() { return phase == Phase.IDLE ? "▶ 시작" : phase == Phase.BREAK ? "▶ 다음 이닝" : paused ? "▶ 계속" : "일시정지"; }
    /** 일시정지 ↔ 계속. 시작 전이면 시작, 3아웃 휴식 중이면 다음 이닝. */
    void togglePause() {
        if (phase == Phase.BREAK) { nextInning(); return; }
        if (phase == Phase.IDLE) { startWindup(); return; }
        paused = !paused; lastNanos = System.nanoTime(); changed(); repaint();
    }

    private void click() {
        if (paused) { togglePause(); return; }
        if (phase == Phase.IDLE) { startWindup(); return; }
        if (phase == Phase.BREAK) { nextInning(); return; }
        if (phase == Phase.PITCH && swingAt < 0) swingAt = phaseTime;   // 판정은 배트가 존에 닿는 순간
    }

    // ---------- 5. 매 프레임 ----------
    private void tick() {
        long now = System.nanoTime(); double dt = Math.min(0.05, (now - lastNanos) / 1e9); lastNanos = now;
        if (paused || phase == Phase.IDLE || phase == Phase.BREAK) return;   // 멈춘 화면은 다시 그릴 필요 없음
        phaseTime += dt;
        if (burst >= 0) burst += dt;
        if (shake >= 0) shake += dt;
        moveBall(dt);
        switch (phase) {
            case WINDUP -> { if (phaseTime > 1.15) startPitch(); }
            case PITCH -> {
                double batTime = swingAt < 0 ? -1 : swingAt + SWING_TIME * CONTACT_AT;
                if (swingAt >= 0 && phaseTime >= Math.max(batTime, travel)) { double off = batTime - travel; show(state.swing(pitch(), off), off); }
                else if (swingAt < 0 && phaseTime > travel + (beanball ? 0 : 0.12)) show(state.take(pitch()), 0);
            }
            case RESULT -> { if (phaseTime > 2.4) { if (state.inningOver()) startBreak(); else startWindup(); } }
            default -> { }
        }
        repaint();
    }

    /** 맞은 공·놓친 공: 중력으로 떨어지고, 땅에 닿으면 튀고, 마지막엔 굴러가다 멈춘다. */
    private void moveBall(double dt) {
        if (!live) return;
        if (catchAt >= 0 && phaseTime >= catchAt) { live = false; return; }          // 수비가 잡음
        bvy += GRAVITY * dt; bx += bvx * dt; by += bvy * dt;
        if (!overWall && by >= GROUND - 2 && bvy > 0) {
            by = GROUND - 2;
            if (bvy > 18) { bvy = -bvy * 0.42; bvx *= 0.75; }                          // 바운드
            else { bvy = 0; bvx *= Math.max(0, 1 - 2.2 * dt); }                       // 굴러감 (마찰)
        }
    }
    private void throwBall(double x, double y, double vx, double vy, double catchTime) {
        live = true; bx = x; by = y; bvx = vx; bvy = vy; catchAt = catchTime; overWall = false;
    }

    private void startWindup() {
        phase = Phase.WINDUP; phaseTime = 0; swingAt = -1; contact = false; burst = -1; distance = ""; live = false; changed();
    }

    private void startPitch() {
        phase = Phase.PITCH; phaseTime = 0;
        int type = random.nextInt(10);
        if (type < 5) { pitchName = "직구"; speedKmh = 138 + random.nextInt(15); curve = 2; }
        else if (type < 8) { pitchName = "커브"; speedKmh = 112 + random.nextInt(12); curve = 10; }
        else { pitchName = "체인지업"; speedKmh = 122 + random.nextInt(10); curve = 5; }
        travel = 0.62 + (152 - speedKmh) / 40.0 * 0.5;     // 빠를수록 짧게 (약 0.62~1.1초)
        startY = GROUND - 20;
        beanball = random.nextDouble() < 0.05;              // 가끔 몸 쪽으로 빠지는 공
        if (beanball) { targetY = GROUND - 13; pitchName = "몸쪽 " + pitchName; }
        else if (random.nextDouble() < 0.6) targetY = ZONE_TOP + 1 + random.nextDouble() * (ZONE_BOTTOM - ZONE_TOP - 2);
        else targetY = random.nextBoolean() ? ZONE_TOP - 3 - random.nextInt(5) : ZONE_BOTTOM + 3 + random.nextInt(4);
    }

    private boolean inZone() { return !beanball && targetY >= ZONE_TOP && targetY <= ZONE_BOTTOM; }
    private boolean reachable() { return !beanball && targetY >= ZONE_TOP - 4 && targetY <= ZONE_BOTTOM + 4; }
    private boolean inDirt() { return targetY > ZONE_BOTTOM + 3; }   // 원바운드 되는 낮은 공
    /** 지금 공의 성질을 GameState가 알아듣는 모양으로 */
    private GameState.Pitch pitch() { return new GameState.Pitch(inZone(), reachable(), inDirt(), beanball); }

    // ---------- 6. 판정 결과 보여 주기 (판정 자체는 GameState) ----------
    /** off = 스윙 타이밍(음수 = 빠름). 파울 방향을 정하는 데만 쓴다. */
    private void show(GameState.Call call, double off) {
        contact = call.motion().contact();
        if (contact) burst = 0;
        double x = PLATE + 2, y = targetY;
        switch (call.motion()) {
            case HIT_BY_PITCH -> throwBall(PLATE - 10, targetY, 30, -50, -1);
            case WILD_PITCH -> throwBall(PLATE - 16, GROUND - 4, -55, -25, -1);
            case DROPPED_THIRD -> throwBall(PLATE - 16, GROUND - 6, -40, -30, -1);
            case FOUL_TIP -> throwBall(PLATE, y, -60, -8, 0.15);
            case FOUL_FLY -> throwBall(x, y, off < 0 ? 30 : -35, -125, 1.2);
            case FOUL -> throwBall(x, y, off < 0 ? 45 : -50, -95, -1);
            case GROUNDER -> throwBall(x, y, 95, 10, -1);
            case FLY_OUT -> throwBall(x, y, 80, -120, 1.25);
            case SINGLE -> { throwBall(x, y, 85, -70, -1); distance = (55 + random.nextInt(35)) + "m"; }
            case DOUBLE -> { throwBall(x, y, 110, -95, -1); distance = (85 + random.nextInt(20)) + "m"; }
            case GROUND_RULE_DOUBLE -> { throwBall(x, y, 120, -100, -1); distance = (95 + random.nextInt(15)) + "m"; }
            case HOME_RUN -> { throwBall(x, y, 150, -165, -1); overWall = true; shake = 0; distance = (112 + random.nextInt(24)) + "m"; }
            default -> { }                                                   // CAUGHT: 포수 미트로
        }
        Color color = switch (call.tone()) {
            case GOOD -> LedPainter.GREEN; case COUNT -> LedPainter.AMBER; case BIG -> LedPainter.RED; case FOUL -> Ballpark.LAMP;
        };
        headline = call.title(); tip = call.why(); headlineColor = color; phase = Phase.RESULT; phaseTime = 0; changed();
    }

    /** 3아웃: 게임을 멈추고, 이번 이닝에 만난 규칙을 모아서 공부를 권한다. */
    private void startBreak() {
        phase = Phase.BREAK; phaseTime = 0; live = false;
        headline = state.breakTitle(); tip = state.breakTip();
        headlineColor = LedPainter.AMBER; changed(); repaint();
    }
    private void nextInning() { state.nextInning(); startWindup(); }

    // ---------- 7. 그리기 ----------
    @Override protected void paintComponent(Graphics g0) {
        int cw = Math.max(SCENE, Math.min(460, (int) Math.round(GH * getWidth() / (double) Math.max(1, getHeight()))));
        int ox = (cw - SCENE) / 2;
        if (field == null || fieldWidth != cw) { field = drawField(cw, ox); fieldWidth = cw; }
        // 2배 해상도 그림: 배경 사진은 촘촘하게, 선수는 2x2 굵은 도트로
        if (frame == null || frame.getWidth() != cw * 2) frame = new BufferedImage(cw * 2, GH * 2, BufferedImage.TYPE_INT_RGB);
        BufferedImage img = frame;
        Graphics2D g = img.createGraphics();
        g.drawImage(field, 0, 0, null);
        g.scale(2, 2);
        g.translate(ox, 0);
        drawZone(g);
        sprite(g, UMPIRE, PLATE - 40, GROUND - UMPIRE.length);
        boolean mittPop = phase == Phase.RESULT && !contact && phaseTime < 0.25;
        sprite(g, CATCHER, PLATE - 29 + (mittPop ? -1 : 0), GROUND - CATCHER.length);
        drawPitcher(g);
        drawBatter(g);
        drawBall(g);
        if (burst >= 0 && burst < 0.18) drawBurst(g, PLATE + 3, (int) targetY, burst);
        if (mittPop) { g.setColor(Color.WHITE); g.fillRect(PLATE - 18, GROUND - 12, 1, 1); g.fillRect(PLATE - 15, GROUND - 13, 1, 1); }
        g.dispose();

        Graphics2D out = (Graphics2D) g0.create();
        double scale = Math.min(getWidth() / (double) cw, getHeight() / (double) GH);   // 굵은 도트 1칸의 화면 크기
        int w = (int) (cw * scale), h = (int) (GH * scale);
        int x = (getWidth() - w) / 2, y = (getHeight() - h) / 2;
        if (shake >= 0 && shake < 0.45) { x += (int) ((random.nextDouble() - .5) * scale * 3); y += (int) ((random.nextDouble() - .5) * scale * 3); }
        out.setColor(new Color(0x0B0F16)); out.fillRect(0, 0, getWidth(), getHeight());
        out.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        out.drawImage(img, x, y, w, h, null);
        if (scale >= 4) { out.setColor(new Color(0, 0, 0, 26)); double s = scale / 2; for (double yy = y; yy < y + h; yy += s) out.fillRect(x, (int) (yy + s - 1), w, 1); }
        drawText(out, x, y, w, h, scale);
        out.dispose();
    }

    /** 위: 사진을 도트로 바꾼 관중석과 조명 / 아래: 외야 담장(광고판), 잔디, 흙, 마운드, 홈플레이트 */
    private BufferedImage drawField(int cw, int ox) {
        BufferedImage f = new BufferedImage(cw * 2, GH * 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = f.createGraphics();
        BufferedImage src = photo();
        if (src != null) {
            // 사진의 담장 위쪽에서, 화면 비율에 맞는 띠를 잘라 2배 해상도 도트로 줄인다
            int wallInPhoto = (int) (src.getHeight() * 0.72);
            int cropH = Math.min(wallInPhoto, (int) (src.getWidth() * WALL / (double) cw));
            Image small = src.getSubimage(0, wallInPhoto - cropH, src.getWidth(), cropH)
                    .getScaledInstance(cw * 2, WALL * 2, Image.SCALE_AREA_AVERAGING);
            BufferedImage px = new BufferedImage(cw * 2, WALL * 2, BufferedImage.TYPE_INT_RGB);
            px.getGraphics().drawImage(small, 0, 0, null);
            for (int y = 0; y < px.getHeight(); y++) for (int x = 0; x < px.getWidth(); x++) px.setRGB(x, y, retro(px.getRGB(x, y)));
            g.drawImage(px, 0, 0, null);
        } else {                                                                 // 사진이 없으면 단색 밤하늘
            g.setColor(new Color(0x0E1A33)); g.fillRect(0, 0, cw * 2, WALL * 2);
        }
        g.scale(2, 2);
        g.setColor(new Color(0x16452A)); g.fillRect(0, WALL - 4, cw, 8);         // 외야 담장 + 광고판
        g.setColor(new Color(0xF2F2F2)); g.fillRect(0, WALL - 4, cw, 1);
        for (int x = 4, i = 0; x < cw; x += 46, i++) {
            g.setColor(i % 2 == 0 ? new Color(0x0E2238) : team.darker()); g.fillRect(x, WALL - 3, 40, 7);
            LedPainter.dots(g, "BASEUP", x + 20, WALL - 3, 1, i % 2 == 0 ? Ballpark.LAMP : Color.WHITE);
        }
        for (int y = WALL + 4; y < GROUND; y++) { g.setColor(((y - WALL) / 4 % 2 == 0) ? new Color(0x2F9148) : new Color(0x2A8341)); g.fillRect(0, y, cw, 1); }
        for (int y = GROUND; y < GH; y++) { g.setColor(Ballpark.mix(new Color(0x9C6238), new Color(0x6E4226), (y - GROUND) / 12.0)); g.fillRect(0, y, cw, 1); }
        g.translate(ox, 0);
        g.setColor(new Color(0xB57448)); g.fillRect(PLATE - 40, GROUND - 3, 80, 3);       // 홈 주변 흙
        g.fillRect(MOUND - 26, GROUND - 3, 52, 3);
        g.setColor(new Color(0xC2825A)); g.fillOval(MOUND - 18, GROUND - 6, 36, 9);       // 마운드
        g.setColor(new Color(0xB57448)); g.fillRect(MOUND - 18, GROUND - 2, 36, 2);
        g.setColor(Color.WHITE); g.fillRect(MOUND - 3, GROUND - 6, 6, 1);                 // 투수판
        g.fillRect(PLATE - 4, GROUND - 1, 9, 1);                                            // 홈플레이트
        g.setColor(new Color(0xE8E8E8)); g.drawRect(PLATE - 16, GROUND - 2, 12, 2);        // 타석 선
        g.dispose();
        return f;
    }

    /** 색을 단계별로 끊어서(포스터 효과) 옛날 게임 색감으로. 밝은 조명은 더 밝게. */
    private static int retro(int rgb) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        int lum = (r * 3 + g * 6 + b) / 10;
        if (lum > 170) { r = Math.min(255, r + 30); g = Math.min(255, g + 30); b = Math.min(255, b + 20); }
        r = Math.min(255, (r / 24) * 24 + 8); g = Math.min(255, (g / 24) * 24 + 8); b = Math.min(255, (b / 20) * 20 + 10);
        return (r << 16) | (g << 8) | b;
    }

    private static BufferedImage photo() {
        if (!photoTried) {
            photoTried = true;
            try { photo = ImageIO.read(new File("data/images/stadium_night.jpg")); } catch (Exception e) { photo = null; }
        }
        return photo;
    }

    private void drawZone(Graphics2D g) {
        if (phase == Phase.BREAK) return;
        g.setColor(new Color(255, 255, 255, phase == Phase.PITCH ? 160 : 90));
        for (int y = ZONE_TOP; y <= ZONE_BOTTOM; y += 2) { g.fillRect(PLATE + 4, y, 1, 1); g.fillRect(PLATE + 8, y, 1, 1); }
        for (int x = PLATE + 4; x <= PLATE + 8; x += 2) { g.fillRect(x, ZONE_TOP, 1, 1); g.fillRect(x, ZONE_BOTTOM, 1, 1); }
    }

    /** 투수: 세트 → 다리 들기 → 내딛으며 던지기 → 팔로스루 */
    private void drawPitcher(Graphics2D g) {
        int x = MOUND - 7, top = GROUND - 6 - 22;
        String[] legs = PITCHER_LEGS_SET; int lean = 0;
        double hx, hy;                                           // 던지는 손
        if (phase == Phase.WINDUP && phaseTime > 0.35 && phaseTime < 0.7) { legs = PITCHER_LEGS_KICK; hx = x + 13; hy = top + 4; }
        else if (phase == Phase.WINDUP && phaseTime >= 0.7) { legs = PITCHER_LEGS_STRIDE; lean = -1; hx = x + 14; hy = top + 2; }
        else if (phase == Phase.PITCH && phaseTime < 0.35) { legs = PITCHER_LEGS_STRIDE; lean = -2; hx = x + 1; hy = top + 14; }
        else { hx = x + 6; hy = top + 11; }
        sprite(g, PITCHER_TOP, x + lean, top);
        sprite(g, legs, x, top + PITCHER_TOP.length);
        limb(g, x + 9 + lean, top + 8, hx, hy, UNI);
        g.setColor(GLOVE); g.fillRect(x + 5 + lean, top + 10, 2, 2);                       // 글러브
        if (phase == Phase.WINDUP) { g.setColor(Color.WHITE); g.fillRect((int) hx, (int) hy, 1, 1); }
    }

    /** 타자: 대기 → 테이크백 → 레벨 스윙(방망이가 앞으로 돌며 짧아 보임) → 팔로스루 */
    private void drawBatter(Graphics2D g) {
        int x = PLATE - 17, top = GROUND - 22;
        double k;
        if (swingAt < 0) k = phase == Phase.PITCH ? -0.3 : -1;
        else if (phase == Phase.RESULT) k = 1.4;
        else k = Math.min(1.4, (phaseTime - swingAt) / SWING_TIME);
        boolean swinging = k > 0.25;
        sprite(g, BATTER_TOP, x + (swinging ? 1 : 0), top);
        sprite(g, swinging ? BATTER_LEGS_SWING : BATTER_LEGS_STANCE, x, top + BATTER_TOP.length);
        double hx, hy, dx, dy, len = 15;                     // 손 위치, 방망이 끝 방향, 보이는 길이
        if (k < 0) { double lift = k > -0.5 ? 1 : 0; hx = x + 6 - lift; hy = top + 8 - lift; dx = -0.35; dy = -0.94; }
        else if (k < 0.25) { double t = k / 0.25; hx = x + 6 + t; hy = top + 9 + t * 2; dx = -0.35 - 0.65 * t; dy = -0.94 + 0.98 * t; }
        else if (k < 0.85) {
            double t = (k - 0.25) / 0.6, angle = Math.PI * (1 - t);   // 뒤(180°) → 앞(0°)으로 수평 회전
            hx = x + 7 + t * 4; hy = top + 11 + Math.sin(t * Math.PI);
            dx = Math.cos(angle); dy = 0.08; len = 15 * Math.max(0.28, Math.abs(Math.cos(angle)));
        } else {
            double t = Math.min(1, (k - 0.85) / 0.4);
            hx = x + 11 - t * 3; hy = top + 11 - t * 3; double a = -t * 2.4;   // 앞 → 위 → 어깨 뒤로
            dx = Math.cos(a); dy = Math.sin(a);
        }
        limb(g, x + 5 + (swinging ? 1 : 0), top + 8, hx, hy, UNI);
        int ex = (int) Math.round(hx + dx * len), ey = (int) Math.round(hy + dy * len);
        Stroke old = g.getStroke();
        g.setColor(BAT_DARK); g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.drawLine((int) hx, (int) hy, ex, ey);
        g.setColor(BAT); g.setStroke(new BasicStroke(1)); g.drawLine((int) hx, (int) hy, ex, ey);
        g.setStroke(old);
        if (k >= 0.3 && k < 0.85) { g.setColor(new Color(255, 255, 255, 70)); g.drawArc((int) hx - 12, (int) hy - 3, 24, 7, 200, 160); }   // 스윙 잔상
        g.setColor(SKIN); g.fillRect((int) hx, (int) hy, 2, 2);
    }

    private void drawBall(Graphics2D g) {
        double x, y;
        if (phase == Phase.PITCH) {
            double t = phaseTime / travel, rx = MOUND - 10;
            double endX = beanball ? PLATE - 10 : PLATE + 6, rlen = rx - endX;
            double tt = Math.min(1, t);
            x = rx - rlen * tt;
            y = pitchY(tt);
            if (t > 1) { double k = Math.min(1, (t - 1) / 0.12); x = endX + (PLATE - 18 - endX) * k; y = y + (GROUND - 8 - y) * k; }   // 포수 미트로
            for (int i = 1; i <= 3; i++) {                                                     // 잔상
                double t1 = Math.max(0, Math.min(1, t) - i * 0.03);
                g.setColor(new Color(255, 255, 255, 90 - i * 25)); g.fillRect((int) (rx - rlen * t1), (int) pitchY(t1), 2, 2);
            }
        } else if (live) { x = bx; y = by; }
        else if (phase == Phase.RESULT && !contact && phaseTime < 0.3 && !beanball) { x = PLATE - 18; y = GROUND - 8; }   // 포수 미트 안
        else return;
        if (!overWall || y < GROUND) { g.setColor(new Color(0, 0, 0, 80)); g.fillRect((int) x - 1, GROUND - 1, 3, 1); }   // 그림자
        g.setColor(Color.WHITE); g.fillRect((int) x, (int) y, 2, 2);
        g.setColor(new Color(0xE0483C)); g.fillRect((int) x + 1, (int) y, 1, 1);
    }

    /** 투구 높이: 살짝 떠올랐다가 목표로. 땅에 닿으면 튀어 오른다(원바운드). */
    private double pitchY(double t) {
        double y = startY + (targetY - startY) * t - Math.sin(t * Math.PI) * curve;
        if (inDirt() && t > 0.75) {                                                           // 홈 앞에서 땅에 한 번 튐
            double k = (t - 0.75) / 0.25; y = (GROUND - 2) - Math.sin(k * Math.PI) * 5 - (1 - k) * 0;
        }
        return Math.min(y, GROUND - 2);
    }

    private static void drawBurst(Graphics2D g, int cx, int cy, double t) {
        int r = 2 + (int) (t * 30);
        g.setColor(new Color(255, 230, 120));
        for (int i = 0; i < 8; i++) { double a = i * Math.PI / 4; g.fillRect(cx + (int) (Math.cos(a) * r), cy + (int) (Math.sin(a) * r), 1, 1); }
        g.setColor(Color.WHITE); g.fillRect(cx - 1, cy - 1, 3, 3);
    }

    /** 팔: 어깨에서 손까지 굵은 선 */
    private static void limb(Graphics2D g, double sx, double sy, double hx, double hy, Color c) {
        Stroke old = g.getStroke();
        g.setColor(OUT); g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.drawLine((int) sx, (int) sy, (int) hx, (int) hy);
        g.setColor(c); g.setStroke(new BasicStroke(1.2f)); g.drawLine((int) sx, (int) sy, (int) hx, (int) hy);
        g.setStroke(old);
    }

    private void sprite(Graphics2D g, String[] rows, int x, int y) {
        for (int r = 0; r < rows.length; r++) for (int c = 0; c < rows[r].length(); c++) {
            Color color = switch (rows[r].charAt(c)) {
                case 'k' -> OUT; case 's' -> SKIN; case 'w' -> UNI; case 'r' -> team; case 'n' -> PANTS;
                case 'g' -> GLOVE; case 'p' -> NAVY; case 'd' -> UMP; case 'q' -> GREY; default -> null;
            };
            if (color != null) { g.setColor(color); g.fillRect(x + c, y + r, 1, 1); }
        }
    }

    /** 한글 안내는 실제 글꼴로 그 위에 얹는다. */
    private void drawText(Graphics2D g, int x, int y, int w, int h, double scale) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int pad = (int) Math.max(10, scale * 3), small = (int) Math.max(12, Math.min(18, scale * 2.6));
        if (phase == Phase.PITCH || phase == Phase.RESULT) {                               // 구종 · 구속 · 비거리
            g.setFont(Theme.font(small, true));
            String s = pitchName + "  " + speedKmh + " km/h" + (distance.isEmpty() ? "" : "   ·   비거리 " + distance);
            box(g, s, x + pad, y + pad, new Color(10, 14, 22, 190), Color.WHITE);
        }
        if (phase == Phase.WINDUP) { g.setFont(Theme.font(small, true)); box(g, state.situation(), x + pad, y + pad, new Color(10, 14, 22, 200), Ballpark.LAMP); }
        if (phase == Phase.IDLE || phase == Phase.RESULT || phase == Phase.BREAK) {
            if (phase == Phase.BREAK) { g.setColor(new Color(5, 8, 14, 170)); g.fillRect(x, y, w, h); }
            g.setFont(Theme.font((int) Math.max(20, Math.min(46, scale * (phase == Phase.BREAK ? 5 : 6.5))), true));
            FontMetrics big = g.getFontMetrics();
            int tw = big.stringWidth(headline), ty = y + (int) (h * (phase == Phase.BREAK ? 0.40 : 0.24));
            g.setColor(new Color(0, 0, 0, 170)); g.drawString(headline, x + (w - tw) / 2 + 3, ty + 3);
            g.setColor(headlineColor); g.drawString(headline, x + (w - tw) / 2, ty);
            g.setFont(Theme.font(small, true));
            FontMetrics fm = g.getFontMetrics();
            String line = tip;
            while (fm.stringWidth(line) > w - pad * 4 && line.length() > 4) line = line.substring(0, line.length() - 2);
            if (!line.equals(tip)) line += "…";
            int bw = fm.stringWidth(line) + pad * 2, bh = fm.getHeight() + pad;
            int bx = x + (w - bw) / 2, by = phase == Phase.BREAK ? ty + pad * 2 : y + h - bh - pad;
            g.setColor(new Color(10, 14, 22, 225)); g.fillRoundRect(bx, by, bw, bh, 14, 14);
            g.setColor(new Color(255, 255, 255, 70)); g.drawRoundRect(bx, by, bw, bh, 14, 14);
            g.setColor(Color.WHITE); g.drawString(line, bx + pad, by + pad / 2 + fm.getAscent());
            if (phase == Phase.BREAK) {
                String hint = "화면을 클릭하거나 '다음 이닝' 버튼을 누르면 이어서 해요";
                g.setColor(new Color(255, 255, 255, 170));
                g.drawString(hint, x + (w - fm.stringWidth(hint)) / 2, by + bh + pad * 2);
            }
        }
        if (paused) {
            g.setColor(new Color(5, 8, 14, 160)); g.fillRect(x, y, w, h);
            g.setFont(Theme.font((int) Math.max(22, Math.min(48, scale * 6)), true)); g.setColor(Color.WHITE);
            String s = "일시정지  ·  클릭하면 계속"; FontMetrics fm = g.getFontMetrics();
            g.drawString(s, x + (w - fm.stringWidth(s)) / 2, y + h / 2);
        }
    }

    private static void box(Graphics2D g, String s, int x, int y, Color bg, Color fg) {
        FontMetrics fm = g.getFontMetrics(); int p = 8;
        g.setColor(bg); g.fillRoundRect(x, y, fm.stringWidth(s) + p * 2, fm.getHeight() + p, 10, 10);
        g.setColor(fg); g.drawString(s, x + p, y + p / 2 + fm.getAscent());
    }
}
