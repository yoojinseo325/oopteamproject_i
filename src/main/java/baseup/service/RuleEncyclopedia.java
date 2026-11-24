package baseup.service;

import baseup.common.TextNormalizer;
import baseup.model.RuleCategory;
import baseup.model.RuleConcept;
import baseup.repository.Catalog;
import java.util.*;

/**
 * 규칙 백과사전. 문제 풀이 진행과 관계없이 전체 규칙을 분류별로 보여 주고 검색한다.
 * 화면 상태를 갖지 않는 순수 로직이라 UI 어디서든 만들어 쓸 수 있다.
 *
 * <pre>
 * RuleEncyclopedia book = new RuleEncyclopedia(service.catalog());
 * book.byCategory();                       // 목차: 분류 → 규칙 목록
 * book.search("인필드플라이");              // 공백·대소문자·동의어 무시, 관련도 순
 * book.search("보크", RuleCategory.PITCHING); // 분류 안에서만 검색
 * book.related("balk", 3);                 // 같이 보면 좋은 규칙
 * </pre>
 */
public final class RuleEncyclopedia {
    /** 검색어가 맞은 위치. 위에 있을수록 점수가 높다. */
    public enum MatchField {
        TITLE("제목"), KEYWORD("키워드"), SUMMARY("요약"), DETAIL("설명"), CATEGORY("분류"), SOURCE("출처"), NONE("");
        private final String label;
        MatchField(String label) { this.label = label; }
        public String label() { return label; }
    }

    /** 검색 결과 한 건. score가 클수록 관련도가 높다. 빈 검색어일 때 score는 0, field는 NONE. */
    public record SearchHit(RuleConcept rule, int score, MatchField field) { }

    private record Score(int value, MatchField field) {
        static final Score NONE = new Score(0, MatchField.NONE);
        Score max(Score other) { return other.value > value ? other : this; }
    }

    /** 정규화된 필드를 미리 계산해 둔 규칙. 검색할 때마다 다시 정규화하지 않는다. */
    private record Entry(int order, RuleConcept rule, String title, List<String> keywords,
                         String summary, String detail, String category, String source) { }

    private static final int MIN_REVERSE_LENGTH = 2;
    private final List<Entry> entries;

    public RuleEncyclopedia(Catalog catalog) { this(catalog.rules()); }

    public RuleEncyclopedia(List<RuleConcept> rules) {
        List<Entry> list = new ArrayList<>();
        for (RuleConcept r : rules) {
            list.add(new Entry(list.size(), r, TextNormalizer.normalize(r.title()),
                    r.keywords().stream().map(TextNormalizer::normalize).filter(k -> !k.isEmpty()).distinct().toList(),
                    TextNormalizer.normalize(r.summary()), TextNormalizer.normalize(r.detail()),
                    TextNormalizer.normalize(r.category().label() + r.topic()), TextNormalizer.normalize(r.source())));
        }
        list.sort(Comparator.comparingInt((Entry e) -> e.rule().category().ordinal()).thenComparingInt(Entry::order));
        entries = List.copyOf(list);
    }

    /** 전체 규칙 수. */
    public int size() { return entries.size(); }

    /** 규칙이 하나 이상 있는 분류만, 목차 순서대로. */
    public List<RuleCategory> categories() {
        return entries.stream().map(e -> e.rule().category()).distinct().toList();
    }

    /** 목차: 분류 → 그 분류의 규칙(파일 순서). 비어 있는 분류는 넣지 않는다. */
    public Map<RuleCategory, List<RuleConcept>> byCategory() {
        Map<RuleCategory, List<RuleConcept>> map = new LinkedHashMap<>();
        for (RuleCategory c : categories()) map.put(c, rulesIn(c));
        return Collections.unmodifiableMap(map);
    }

    /** 한 분류의 규칙. null이면 전체. */
    public List<RuleConcept> rulesIn(RuleCategory category) {
        return entries.stream().map(Entry::rule).filter(r -> category == null || r.category() == category).toList();
    }

    public Optional<RuleConcept> find(String id) {
        return entries.stream().map(Entry::rule).filter(r -> r.id().equals(id)).findFirst();
    }

    /** 전체 분류에서 검색. 빈 검색어면 전체 목록을 목차 순서로 돌려준다. */
    public List<RuleConcept> search(String query) { return search(query, null); }

    /** 한 분류 안에서 검색(category가 null이면 전체). 관련도가 높은 순서. */
    public List<RuleConcept> search(String query, RuleCategory category) {
        return searchHits(query, category).stream().map(SearchHit::rule).toList();
    }

    /**
     * 점수와 맞은 위치까지 포함한 검색 결과.
     * 검색어 전체를 붙여 쓴 형태로 비교하고("포스 아웃" = "포스아웃"),
     * 여러 단어라면 모든 단어가 어딘가에 들어 있는지도 따져서 더 높은 점수를 쓴다("포스 득점").
     */
    public List<SearchHit> searchHits(String query, RuleCategory category) {
        List<Entry> scope = entries.stream().filter(e -> category == null || e.rule().category() == category).toList();
        String whole = TextNormalizer.normalize(query);
        if (whole.isEmpty()) return scope.stream().map(e -> new SearchHit(e.rule(), 0, MatchField.NONE)).toList();

        List<String> terms = TextNormalizer.terms(query);
        List<SearchHit> hits = new ArrayList<>();
        for (Entry e : scope) {
            Score s = score(e, whole, true);
            if (terms.size() > 1) s = s.max(allTerms(e, terms));
            if (s.value() > 0) hits.add(new SearchHit(e.rule(), s.value(), s.field()));
        }
        Map<String, Entry> byId = new HashMap<>();
        for (Entry e : entries) byId.put(e.rule().id(), e);
        hits.sort(Comparator.comparingInt(SearchHit::score).reversed()
                .thenComparingInt(h -> h.rule().category().ordinal())
                .thenComparingInt(h -> byId.get(h.rule().id()).order()));
        return List.copyOf(hits);
    }

    /**
     * 같이 보면 좋은 규칙. 키워드가 겹칠수록, 같은 분류일수록 앞에 온다. 자기 자신은 제외.
     * 없는 ID면 빈 목록.
     */
    public List<RuleConcept> related(String ruleId, int limit) {
        Entry base = entries.stream().filter(e -> e.rule().id().equals(ruleId)).findFirst().orElse(null);
        if (base == null || limit <= 0) return List.of();
        Set<String> keys = new HashSet<>(base.keywords());
        keys.add(base.title());
        record Candidate(Entry entry, int score) { }
        return entries.stream().filter(e -> e != base).map(e -> {
            int shared = (int) e.keywords().stream().filter(keys::contains).count() + (keys.contains(e.title()) ? 1 : 0);
            int score = shared * 10 + (e.rule().category() == base.rule().category() ? 5 : 0);
            return new Candidate(e, score);
        }).filter(c -> c.score() > 0)
                .sorted(Comparator.comparingInt(Candidate::score).reversed().thenComparingInt(c -> c.entry().order()))
                .limit(limit).map(c -> c.entry().rule()).toList();
    }

    /** 모든 단어가 어딘가에 맞아야 점수를 준다. 한 단어만 맞은 규칙보다 앞에 오도록 10점을 더한다. */
    private static Score allTerms(Entry e, List<String> terms) {
        int total = 0;
        MatchField first = MatchField.NONE;
        for (String term : terms) {
            Score s = score(e, term, false);
            if (s.value() == 0) return Score.NONE;
            total += s.value();
            if (first == MatchField.NONE) first = s.field();
        }
        return new Score(Math.min(99, total / terms.size() + 10), first);
    }

    private static Score score(Entry e, String q, boolean allowReverse) {
        Score best = Score.NONE;
        if (e.title().equals(q)) best = best.max(new Score(100, MatchField.TITLE));
        else if (e.title().startsWith(q)) best = best.max(new Score(90, MatchField.TITLE));
        else if (e.title().contains(q)) best = best.max(new Score(80, MatchField.TITLE));
        for (String k : e.keywords()) {
            // 동의어와 정확히 같으면 제목 앞부분 일치("투구" → "투구 이닝 표기")보다 우선한다.
            if (k.equals(q)) best = best.max(new Score(95, MatchField.KEYWORD));
            else if (k.contains(q)) best = best.max(new Score(70, MatchField.KEYWORD));
        }
        // "보크가 뭐야"처럼 문장으로 물어도 제목이나 키워드를 찾는다.
        if (allowReverse) {
            if (e.title().length() >= MIN_REVERSE_LENGTH && q.contains(e.title())) best = best.max(new Score(60, MatchField.TITLE));
            for (String k : e.keywords())
                if (k.length() >= MIN_REVERSE_LENGTH && q.contains(k)) best = best.max(new Score(55, MatchField.KEYWORD));
        }
        if (e.summary().contains(q)) best = best.max(new Score(40, MatchField.SUMMARY));
        if (e.detail().contains(q)) best = best.max(new Score(25, MatchField.DETAIL));
        if (e.category().contains(q)) best = best.max(new Score(20, MatchField.CATEGORY));
        if (e.source().contains(q)) best = best.max(new Score(15, MatchField.SOURCE));
        return best;
    }
}
