package baseup;

import baseup.game.GameState;
import baseup.game.GameState.Motion;
import baseup.game.GameState.Pitch;

import java.util.Random;

/**
 * 타격 게임 규칙(GameState) 테스트. 화면 없이 JDK만으로 실행한다.
 * 실행: java -cp "build/classes;build/test-classes" baseup.GameStateTest
 */
public final class GameStateTest {
    private static int checks;
    private static void check(boolean value, String name) {
        if (!value) throw new AssertionError(name);
        checks++;
    }

    /** 랜덤 값을 정해 둔 가짜 Random: nextDouble은 항상 d, nextBoolean은 항상 b */
    private static final class Fixed extends Random {
        private final double d; private final boolean b;
        Fixed(double d, boolean b) { this.d = d; this.b = b; }
        @Override public double nextDouble() { return d; }
        @Override public boolean nextBoolean() { return b; }
    }
    /** 특별한 일(낫아웃·폭투·파울팁·병살 등)이 절대 안 일어나는 게임 */
    private static GameState plain() { return new GameState(new Fixed(0.99, false)); }

    private static final Pitch STRIKE = new Pitch(true, true, false, false);    // 존 한가운데
    private static final Pitch BALL = new Pitch(false, false, false, false);    // 칠 수 없는 높이
    private static final Pitch LOW = new Pitch(false, true, true, false);       // 원바운드 낮은 공
    private static final Pitch EDGE = new Pitch(false, true, false, false);     // 존 살짝 밖 (치면 파울)
    private static final Pitch BEAN = new Pitch(false, false, false, true);     // 몸쪽 공

    public static void main(String[] args) {
        // 1. 볼 4개 = 볼넷, 카운트 초기화, 1루 주자
        GameState g = plain();
        for (int i = 0; i < 3; i++) check(g.take(BALL).title().equals("볼"), "ball " + i);
        check(g.balls() == 3, "3 balls");
        check(g.take(BALL).title().equals("볼넷!"), "walk");
        check(g.balls() == 0 && g.strikes() == 0 && g.onBase(0), "walk resets count and runner on 1st");

        // 2. 만루에서 볼넷 = 밀어내기 1점
        g = plain();
        for (int k = 0; k < 4; k++) for (int i = 0; i < 4; i++) g.take(BALL);
        check(g.runs() == 1 && g.onBase(0) && g.onBase(1) && g.onBase(2), "bases-loaded walk scores 1");
        check(g.metRules().contains("밀어내기"), "force-in rule recorded");

        // 3. 1루만 있을 때 볼넷: 2루로 밀림, 점수 없음
        g = plain(); for (int i = 0; i < 8; i++) g.take(BALL);
        check(g.onBase(0) && g.onBase(1) && !g.onBase(2) && g.runs() == 0, "walk pushes runner to 2nd");

        // 4. 루킹 3번 = 삼진 아웃
        g = plain();
        g.take(STRIKE); g.take(STRIKE);
        check(g.strikes() == 2, "2 strikes");
        GameState.Call k = g.take(STRIKE);
        check(k.title().startsWith("삼진"), "strikeout");
        check(g.outs() == 1 && g.strikes() == 0, "strikeout adds an out");

        // 5. 2스트라이크 뒤 파울은 카운트 그대로
        g = plain(); g.take(STRIKE); g.take(STRIKE);
        GameState.Call f = g.swing(EDGE, 0.0);
        check(f.motion() == Motion.FOUL && g.strikes() == 2, "foul with 2 strikes keeps count");
        g = plain();
        g.swing(EDGE, 0.1);
        check(g.strikes() == 1, "foul with 0 strikes is a strike");

        // 6. 헛스윙은 공이 존 밖이어도 스트라이크
        g = plain(); g.swing(BALL, 0.0);
        check(g.strikes() == 1, "swing and miss is a strike");

        // 7. 몸에 맞는 공: 안 치면 1루, 치다 맞으면 스트라이크
        g = plain();
        check(g.take(BEAN).motion() == Motion.HIT_BY_PITCH && g.onBase(0), "hit by pitch -> 1st base");
        g = plain(); g.swing(BEAN, 0.0);
        check(g.strikes() == 1 && !g.onBase(0), "swinging hit-by-pitch is a strike");

        // 8. 3아웃이면 이닝 끝, 다음 이닝에 초기화
        g = plain();
        for (int i = 0; i < 9; i++) g.take(STRIKE);
        check(g.inningOver() && g.outs() == 3, "three outs end the inning");
        check(g.breakTip().contains("삼진"), "break tip lists strikeouts");
        g.nextInning();
        check(g.inning() == 2 && g.outs() == 0 && !g.inningOver() && g.metRules().isEmpty(), "next inning resets");

        // 9. 낫아웃: 1루가 비었으면 뛸 수 있다 (random 0.1 < 0.2, nextBoolean true = 세이프)
        g = new GameState(new Fixed(0.1, true));
        g.take(STRIKE); g.take(STRIKE);
        GameState.Call d = g.take(STRIKE);
        check(d.motion() == Motion.DROPPED_THIRD && g.onBase(0) && g.outs() == 0, "dropped third strike, safe at 1st");
        // 1루에 주자가 있고 2아웃 전이면 낫아웃 불가 → 그냥 삼진
        g = new GameState(new Fixed(0.1, true));
        for (int i = 0; i < 4; i++) g.take(BALL);                  // 볼넷으로 1루 주자 (BALL은 원바운드가 아니라 폭투 없음)
        g.take(STRIKE); g.take(STRIKE);
        check(g.take(STRIKE).title().startsWith("삼진") && g.outs() == 1, "no dropped third with runner on 1st");

        // 10. 폭투: 주자 있을 때 원바운드 공을 못 잡으면 주자가 한 베이스씩
        g = new GameState(new Fixed(0.1, false));
        for (int i = 0; i < 4; i++) g.take(BALL);                  // 1루 주자
        check(g.take(LOW).motion() == Motion.WILD_PITCH && g.onBase(1) && !g.onBase(0) && g.balls() == 1, "wild pitch advances runner");

        // 11. 홈런: 주자 전원 + 타자 득점 (타이밍 거의 완벽 + random 0.1 < 0.45)
        g = loadedThenHomeRun();
        check(g.runs() == 4 && !g.onBase(0) && !g.onBase(1) && !g.onBase(2), "grand slam scores 4");
        check(g.metRules().contains("만루 홈런"), "grand slam recorded");

        // 12. 상황 이름 → 규칙 카드 id
        for (String s : GameState.SITUATIONS) check(GameState.ruleIdOf(s) != null, "rule card for " + s);
        check(GameState.ruleIdOf("폭투") == null, "no card for wild pitch yet");

        System.out.println("PASS: " + checks + " game rule checks (count, walk, force-in, strikeout, foul, HBP, dropped third, wild pitch, home run)");
    }

    /** 볼넷 3번으로 만루를 만든 뒤, 홈런이 나오게 정해진 Random으로 한 번 친다 */
    private static GameState loadedThenHomeRun() {
        GameState g = new GameState(new Fixed(0.1, false));
        for (int i = 0; i < 12; i++) g.take(BALL);
        check(g.onBase(0) && g.onBase(1) && g.onBase(2) && g.runs() == 0, "bases loaded before home run");
        check(g.swing(STRIKE, 0.0).motion() == Motion.HOME_RUN, "perfect timing home run");
        return g;
    }
}
