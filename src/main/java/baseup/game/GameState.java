package baseup.game;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;

/**
 * 타격 게임의 규칙과 기록 (화면 코드 없음).
 *
 * 볼·스트라이크·아웃 카운트, 1·2·3루 주자, 점수를 기억하고,
 * 투구 하나를 판정해서 그 결과(Call)를 돌려준다. 화면(PitchGame)은 Call을 보고 글자와 공 움직임만 그린다.
 * 화면과 떨어져 있어서 규칙만 따로 테스트할 수 있고(GameStateTest),
 * 메인 화면을 다시 만들어도 이 객체만 들고 있으면 경기가 이어진다.
 */
public final class GameState {

    /** 결과 글자 색의 종류: 좋은 일(초록) / 카운트(노랑) / 아웃·홈런(빨강) / 파울 */
    public enum Tone { GOOD, COUNT, BIG, FOUL }

    /** 판정 뒤 공이 어떻게 움직이는지 (화면이 애니메이션을 고르는 데 쓴다) */
    public enum Motion {
        CAUGHT, HIT_BY_PITCH, WILD_PITCH, DROPPED_THIRD,
        FOUL_TIP, FOUL_FLY, FOUL, GROUNDER, FLY_OUT, SINGLE, DOUBLE, GROUND_RULE_DOUBLE, HOME_RUN;
        /** 배트에 맞은 공인지 */
        public boolean contact() { return ordinal() >= FOUL_TIP.ordinal(); }
    }

    /** 판정 결과: 큰 제목, 규칙 설명 한 줄, 색 종류, 공 움직임 */
    public record Call(String title, String why, Tone tone, Motion motion) { }

    /** 던진 공의 성질: 존 안인지, 칠 수 있는 높이인지, 원바운드인지, 몸쪽 공인지 */
    public record Pitch(boolean inZone, boolean reachable, boolean inDirt, boolean beanball) { }

    /** 규칙 카드와 연결된 게임 상황 이름들 */
    public static final List<String> SITUATIONS = List.of("낫아웃", "파울팁", "파울", "볼넷", "밀어내기", "병살",
            "희생플라이", "삼진", "홈런", "만루 홈런", "땅볼 아웃", "3아웃");

    /** 게임 상황 이름 → 규칙 카드 id (data/rules.tsv). 카드가 없는 상황은 null */
    public static String ruleIdOf(String situation) {
        return switch (situation) {
            case "낫아웃" -> "dropped-third"; case "파울팁" -> "foul-tip"; case "파울" -> "foul";
            case "볼넷" -> "walk"; case "밀어내기" -> "walk-loaded"; case "병살" -> "doubleplay";
            case "희생플라이" -> "tagup"; case "삼진" -> "count"; case "홈런" -> "homerun";
            case "만루 홈런" -> "loaded"; case "땅볼 아웃" -> "force"; case "3아웃" -> "outs";
            default -> null;
        };
    }

    private final Random random;
    private int balls, strikes, outs, runs, hits, inning = 1, inningRuns, inningHits;
    private final boolean[] bases = new boolean[3];                    // 1루, 2루, 3루
    private final LinkedHashSet<String> metRules = new LinkedHashSet<>();   // 이번 이닝에 나온 규칙

    public GameState() { this(new Random()); }
    /** 테스트에서는 결과가 정해진 Random을 넣는다 */
    public GameState(Random random) { this.random = random; }

    // ---------- 읽기 ----------
    public int balls() { return balls; }
    public int strikes() { return strikes; }
    public int outs() { return outs; }
    public int runs() { return runs; }
    public int hits() { return hits; }
    public int inning() { return inning; }
    public boolean onBase(int base) { return bases[base]; }
    public boolean inningOver() { return outs >= 3; }
    /** 아직 한 번도 안 던졌는지 (이어 하기 안내에 씀) */
    public boolean fresh() { return inning == 1 && runs == 0 && hits == 0 && outs == 0 && balls == 0 && strikes == 0; }
    public List<String> metRules() { return new ArrayList<>(metRules); }

    /** 투구 전 상황 안내: 지금 카운트·주자에서 알아 둘 규칙 */
    public String situation() {
        if (bases[2] && outs < 2) return "3루 주자! 외야로 띄우면 희생플라이로 1점이 들어올 수 있어요";
        if (bases[0] && outs < 2) return "1루 주자 있음 · 땅볼이면 병살(아웃 2개)을 조심!";
        if (balls == 3 && strikes == 2) return "풀카운트(3볼 2스트라이크)! 다음 공이 승부예요";
        if (balls == 3) return "3볼! 하나만 더 볼이면 볼넷이에요";
        if (strikes == 2) return "2스트라이크! 이제 파울은 카운트가 그대로예요 (파울팁은 삼진)";
        if (outs == 2) return "2아웃! 다음 아웃이면 공수 교대예요";
        return "투수 와인드업... 공을 잘 보세요!";
    }

    // ---------- 판정 ----------
    /** 치지 않았을 때 */
    public Call take(Pitch p) {
        if (p.beanball()) {
            force(); resetCount(); metRules.add("몸에 맞는 공");
            return new Call("몸에 맞는 공!", "스윙하지 않았는데 공이 몸에 맞으면 타자는 1루로 나가요(사구).", Tone.GOOD, Motion.HIT_BY_PITCH);
        }
        if (p.inZone()) return strike("스트라이크! (루킹)", "치지 않아도 공이 스트라이크 존(무릎~가슴)을 지나가면 스트라이크예요.", null);
        boolean runnersOn = bases[0] || bases[1] || bases[2];
        if (p.inDirt() && runnersOn && random.nextDouble() < 0.35) {          // 원바운드 공을 포수가 놓침 → 폭투
            balls++; metRules.add("폭투");
            if (bases[2]) score(1);
            bases[2] = bases[1]; bases[1] = bases[0]; bases[0] = false;
            if (balls >= 4) {
                force(); resetCount();
                return new Call("폭투 + 볼넷!", "포수가 놓친 사이 주자가 한 베이스씩 가요(폭투). 볼 4개라 타자도 1루로!", Tone.GOOD, Motion.WILD_PITCH);
            }
            return new Call("폭투!", "투수가 던진 공을 포수가 못 잡으면 그 사이 주자가 다음 베이스로 갈 수 있어요.", Tone.GOOD, Motion.WILD_PITCH);
        }
        balls++;
        if (balls >= 4) {
            force(); resetCount(); metRules.add("볼넷");
            return new Call("볼넷!", "볼 4개면 타자는 1루로 걸어 나가요. 앞 주자는 밀려서 한 베이스씩 가요.", Tone.GOOD, Motion.CAUGHT);
        }
        return new Call("볼", "존을 벗어난 공은 치지 않으면 볼이에요. 볼 4개면 볼넷!", Tone.GOOD, Motion.CAUGHT);
    }

    /** 스윙했을 때. off = 배트가 닿은 시각 - 공이 도착한 시각(초, 음수 = 빠름) */
    public Call swing(Pitch p, double off) {
        double gap = Math.abs(off);
        if (p.beanball())
            return strike("스윙 중 몸에 맞음 → 스트라이크", "스윙하다 공에 맞으면 몸에 맞는 공(사구)이 아니라 스트라이크예요.", "스윙 중 사구");
        if (p.reachable() && gap <= 0.06) {
            if (!p.inZone()) return foul(off);
            double r = random.nextDouble();
            if (gap <= 0.025 && r < 0.45) return homeRun();
            if (r < 0.30) return groundBall();
            if (r < 0.52) return flyBall();
            if (r < 0.85) return single();
            return doubleHit();
        }
        if (p.reachable() && gap <= 0.13) return foul(off);
        return strike("헛스윙 스트라이크!", "배트를 휘둘렀는데 못 맞히면 공이 존 밖이어도 스트라이크예요.", null);
    }

    private Call strike(String title, String why, String rule) {
        if (rule != null) metRules.add(rule);
        strikes++;
        if (strikes < 3) return new Call(title, why, Tone.COUNT, Motion.CAUGHT);
        // 세 번째 스트라이크: 가끔 포수가 공을 놓친다 → 스트라이크 낫아웃 (1루가 비었거나 2아웃일 때만)
        boolean canRun = !bases[0] || outs == 2;
        if (canRun && random.nextDouble() < 0.2) {
            metRules.add("낫아웃"); resetCount();
            if (random.nextBoolean()) {
                force();
                return new Call("스트라이크 낫아웃! 타자 1루 세이프", "3스트라이크인데 포수가 공을 놓치면 타자는 1루로 뛸 수 있어요. 먼저 도착해서 살았어요!", Tone.GOOD, Motion.DROPPED_THIRD);
            }
            return out("스트라이크 낫아웃 → 1루 송구 아웃", "포수가 놓쳐서 타자가 뛰었지만, 1루로 먼저 던져서 아웃이에요.", Motion.DROPPED_THIRD);
        }
        metRules.add("삼진");
        return out("삼진 아웃!", "스트라이크 3개면 삼진 아웃이에요.", Motion.CAUGHT);
    }

    private Call foul(double off) {
        if (strikes == 2 && random.nextDouble() < 0.15) {                    // 파울팁: 포수가 바로 잡음
            metRules.add("파울팁");
            return out("파울팁 삼진!", "2스트라이크에서 배트에 살짝 스친 공을 포수가 바로 잡으면 삼진이에요.", Motion.FOUL_TIP);
        }
        if (random.nextDouble() < 0.18) {                                     // 떠오른 파울을 수비가 잡음
            metRules.add("파울플라이");
            return out("파울플라이 아웃", "파울이어도 땅에 떨어지기 전에 수비가 잡으면 아웃이에요.", Motion.FOUL_FLY);
        }
        metRules.add("파울");
        if (strikes < 2) strikes++;                                          // 2스트라이크 뒤에는 그대로
        String title = off < 0 ? "파울! (타이밍 빠름)" : off > 0 ? "파울! (타이밍 늦음)" : "파울!";
        return new Call(title, "파울은 2스트라이크 전까지 스트라이크. 2스트라이크 뒤 파울은 카운트가 그대로예요.", Tone.FOUL, Motion.FOUL);
    }

    private Call groundBall() {
        if (bases[0] && outs < 2 && random.nextDouble() < 0.45) {           // 병살: 1루 주자 + 타자
            metRules.add("병살"); bases[0] = false; outs++;
            return out("병살타! 아웃 2개", "땅볼을 잡아 2루 → 1루로 연달아 던지면 주자와 타자가 한 번에 아웃돼요(더블 플레이).", Motion.GROUNDER);
        }
        metRules.add("땅볼 아웃");
        return out("땅볼 아웃", "공을 쳐도 수비가 잡아서 타자보다 먼저 1루에 던지면 아웃이에요.", Motion.GROUNDER);
    }

    private Call flyBall() {
        if (bases[2] && outs < 2) {                                            // 희생플라이
            metRules.add("희생플라이"); bases[2] = false; score(1);
            return out("희생플라이! 1점", "뜬공이 잡혀 타자는 아웃이지만, 3루 주자가 잡힌 뒤 출발해 홈으로 들어오면 1점이에요.", Motion.FLY_OUT);
        }
        metRules.add("뜬공 아웃");
        return out("뜬공 아웃", "친 공이 땅에 닿기 전에 수비가 잡으면 아웃이에요.", Motion.FLY_OUT);
    }

    private Call single() {
        hits++; inningHits++;
        if (bases[2]) score(1);
        bases[2] = bases[1]; bases[1] = bases[0]; bases[0] = true; resetCount();
        return new Call("안타!", "공을 쳐서 수비보다 먼저 1루에 도착하면 안타예요. 주자도 한 베이스씩 가요.", Tone.GOOD, Motion.SINGLE);
    }

    private Call doubleHit() {
        hits++; inningHits++; resetCount();
        score((bases[2] ? 1 : 0) + (bases[1] ? 1 : 0));
        bases[2] = bases[0]; bases[1] = true; bases[0] = false;
        if (random.nextDouble() < 0.3) {                                       // 인정 2루타
            metRules.add("인정 2루타");
            return new Call("인정 2루타!", "친 공이 땅에 튄 뒤 담장을 넘어가면 홈런이 아니라 2루타예요.", Tone.GOOD, Motion.GROUND_RULE_DOUBLE);
        }
        metRules.add("2루타");
        return new Call("2루타!", "타자가 2루까지 살아서 가면 2루타예요.", Tone.GOOD, Motion.DOUBLE);
    }

    private Call homeRun() {
        hits++; inningHits++;
        int scored = 1 + (bases[0] ? 1 : 0) + (bases[1] ? 1 : 0) + (bases[2] ? 1 : 0);
        score(scored); bases[0] = bases[1] = bases[2] = false; resetCount();
        metRules.add(scored == 4 ? "만루 홈런" : "홈런");
        String title = scored == 4 ? "만루 홈런!!" : scored > 1 ? scored + "점 홈런!" : "홈런!";
        return new Call(title, "공이 땅에 닿지 않고 외야 담장을 넘어가면 홈런. 주자도 모두 홈으로 들어와요.", Tone.BIG, Motion.HOME_RUN);
    }

    private Call out(String title, String why, Motion motion) {
        outs++; resetCount();
        if (outs >= 3) { metRules.add("3아웃"); return new Call(title + "  3아웃!", why + " 아웃 3개면 공수 교대예요.", Tone.BIG, motion); }
        return new Call(title, why, Tone.BIG, motion);
    }

    /** 볼넷·사구처럼 밀려서 가는 진루: 뒤에서 밀려야만 앞 주자가 움직인다. */
    private void force() {
        if (bases[0]) {
            if (bases[1]) {
                if (bases[2]) { score(1); metRules.add("밀어내기"); }
                bases[2] = true;
            }
            bases[1] = true;
        }
        bases[0] = true;
    }
    private void score(int n) { runs += n; inningRuns += n; }
    private void resetCount() { balls = 0; strikes = 0; }

    // ---------- 이닝 ----------
    public String breakTitle() { return "3아웃 체인지!  이번 이닝 " + inningRuns + "점 · 안타 " + inningHits; }
    public String breakTip() {
        return metRules.isEmpty() ? "공수 교대 규칙이 궁금하다면 규칙 학습에서 이닝 흐름을 배워 봐요!"
                : "이번 이닝에 만난 규칙: " + String.join(" · ", metRules) + "  →  규칙 학습에서 자세히 배워 봐요!";
    }
    public void nextInning() {
        outs = 0; bases[0] = bases[1] = bases[2] = false; inning++; inningRuns = 0; inningHits = 0; metRules.clear(); resetCount();
    }
}
