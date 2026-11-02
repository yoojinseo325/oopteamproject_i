package baseup.model;

public record UserProfile(String id, String nickname) implements Identifiable {
    @Override public String toString() { return nickname; }
}
