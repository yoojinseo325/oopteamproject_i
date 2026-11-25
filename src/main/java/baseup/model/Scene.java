package baseup.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 좌표는 800 x 500 가상 야구장 기준. 창 크기에 맞게 화면에서 확대/축소한다. */
public record Scene(String id, String caption, List<Token> tokens) implements Identifiable {
    public Scene { tokens = List.copyOf(tokens); }
    public record Token(String id, String label, String kind, int x, int y, Player player) {
        public Token(String id, String label, String kind, int x, int y) { this(id,label,kind,x,y,null); }
        public String displayLabel() { return player == null ? label : player.displayName() + " · " + label; }
    }

    /**
     * 장면 from에서 장면 to로 넘어가는 중간 모습(시나리오 재생, 교수님 피드백 9).
     * progress 0이면 from, 1이면 to와 같다. 두 장면에 모두 있는 대상(같은 id)은 부드럽게 움직이고,
     * 한쪽에만 있는 대상은 중간(0.5)에서 사라지거나 나타난다. 처음과 끝은 천천히 움직인다(ease-in-out).
     */
    public static Scene between(Scene from, Scene to, double progress) {
        double t = Math.max(0, Math.min(1, progress));
        double eased = t < .5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
        boolean secondHalf = t >= .5;
        Map<String, Token> target = to.tokens().stream().collect(Collectors.toMap(Token::id, Function.identity()));
        List<Token> result = new ArrayList<>();
        for (Token a : from.tokens()) {
            Token b = target.get(a.id());
            if (b == null) { if (!secondHalf) result.add(a); continue; }
            Token shown = secondHalf ? b : a; // 이름·종류는 중간에 바뀐다
            result.add(new Token(a.id(), shown.label(), shown.kind(),
                    (int) Math.round(a.x() + (b.x() - a.x()) * eased), (int) Math.round(a.y() + (b.y() - a.y()) * eased), shown.player()));
        }
        if (secondHalf) for (Token b : to.tokens())
            if (from.tokens().stream().noneMatch(a -> a.id().equals(b.id()))) result.add(b);
        return new Scene(from.id() + "->" + to.id(), secondHalf ? to.caption() : from.caption(), result);
    }
}
