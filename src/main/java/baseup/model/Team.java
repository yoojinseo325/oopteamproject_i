package baseup.model;

/** UI 도구에 의존하지 않는 구단 정보와 대표 RGB 색상. */
public record Team(String id, String name, String shortName, int color, int accent) implements Identifiable { }
