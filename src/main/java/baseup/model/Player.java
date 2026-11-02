package baseup.model;

public record Player(String id, String name, Team team) implements Identifiable {
    public String displayName() { return team.shortName() + " " + name; }
}
