package baseup.model;

public record StudyNote(String id, String userId, String ruleId, String text) implements Identifiable { }
