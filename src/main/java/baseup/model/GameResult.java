package baseup.model;

/**
 * 응원팀 경기 한 줄(data/games.tsv). 점수 칸이 비어 있으면 아직 안 한 경기(예정)다.
 * 날짜는 2026-11-20 처럼 적고, 팀은 teams.tsv 의 id(KIA, SAMSUNG ...)를 쓴다.
 */
public record GameResult(String id, String date, String time, String home, String away, String place,
                         Integer homeScore, Integer awayScore) implements Identifiable {
    public boolean played() { return homeScore != null && awayScore != null; }
    public boolean involves(String teamId) { return home.equals(teamId) || away.equals(teamId); }
    public boolean isHome(String teamId) { return home.equals(teamId); }
    public String opponent(String teamId) { return isHome(teamId) ? away : home; }
    public int myScore(String teamId) { return isHome(teamId) ? homeScore : awayScore; }
    public int theirScore(String teamId) { return isHome(teamId) ? awayScore : homeScore; }

    public static final String HEADER = "id\tdate\ttime\thome\taway\tplace\thomeScore\tawayScore";
    public static GameResult decode(String[] f) {
        return new GameResult(f[0], f[1], f[2], f[3], f[4], f[5],
                f[6].isBlank() ? null : Integer.valueOf(f[6]), f[7].isBlank() ? null : Integer.valueOf(f[7]));
    }
    public static String encode(GameResult g) {
        return String.join("\t", g.id, g.date, g.time, g.home, g.away, g.place,
                g.homeScore == null ? "" : g.homeScore.toString(), g.awayScore == null ? "" : g.awayScore.toString());
    }
}
