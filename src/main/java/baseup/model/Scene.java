package baseup.model;

import java.util.List;

/** 좌표는 800 x 500 가상 야구장 기준. 창 크기에 맞게 화면에서 확대/축소한다. */
public record Scene(String id, String caption, List<Token> tokens) implements Identifiable {
    public Scene { tokens = List.copyOf(tokens); }
    public record Token(String id, String label, String kind, int x, int y, Player player) {
        public Token(String id, String label, String kind, int x, int y) { this(id,label,kind,x,y,null); }
        public String displayLabel() { return player == null ? label : player.displayName() + " · " + label; }
    }
}
