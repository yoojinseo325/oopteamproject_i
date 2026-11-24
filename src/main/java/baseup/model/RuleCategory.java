package baseup.model;

import java.util.Arrays;

/** 규칙 백과사전의 큰 분류. 선언 순서가 화면 목차 순서다. rules.tsv에는 code를 저장한다. */
public enum RuleCategory {
    BASIC("basic", "기본 규칙", "경기 구성, 카운트, 아웃, 포지션"),
    BATTING("batting", "타격 규칙", "파울, 번트, 인필드 플라이, 삼진"),
    RUNNING("running", "주루 규칙", "포스·태그, 태그업, 도루, 병살"),
    PITCHING("pitching", "투수 규칙", "투구 자세, 견제, 보크"),
    UMPIRE("umpire", "심판 판정", "세이프·파울 판정, 방해, 몸에 맞는 공"),
    RECORD("record", "기록과 용어", "타율, 출루율, 평균자책점 등 기록 읽기");

    private final String code;
    private final String label;
    private final String description;

    RuleCategory(String code, String label, String description) {
        this.code = code;
        this.label = label;
        this.description = description;
    }

    public String code() { return code; }
    public String label() { return label; }
    public String description() { return description; }

    public static RuleCategory fromCode(String code) {
        return Arrays.stream(values()).filter(c -> c.code.equals(code)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 규칙 분류: " + code));
    }

    @Override public String toString() { return label; }
}
