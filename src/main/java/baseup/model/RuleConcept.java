package baseup.model;

public record RuleConcept(String id, String topic, String title, String summary,
                          String detail, String source) implements Identifiable {
    @Override public String toString() { return title; }
}
