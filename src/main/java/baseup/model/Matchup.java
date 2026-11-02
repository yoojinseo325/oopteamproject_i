package baseup.model;

/** 실제 경기 결과가 아닌 학습용 상황의 공격/수비 구단 배정. */
public record Matchup(String id, Team offense, Team defense) implements Identifiable { }
