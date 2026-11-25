package baseup;

import baseup.common.TextNormalizer;
import baseup.model.RuleCategory;
import baseup.model.RuleConcept;
import baseup.repository.Catalog;
import baseup.service.RuleEncyclopedia;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** 규칙 백과사전 분류·검색과 공용 문자열 정규화 검사. JUnit 없이 실행한다. */
public final class RuleEncyclopediaTest {
    private static int checks;
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); checks++; }
    private static String first(List<RuleConcept> rules) { return rules.isEmpty() ? "(없음)" : rules.get(0).id(); }
    private static List<String> ids(List<RuleConcept> rules) { return rules.stream().map(RuleConcept::id).toList(); }

    public static void main(String[] args) throws Exception {
        normalizer();
        Catalog catalog = new Catalog(Path.of("data"));
        RuleEncyclopedia book = new RuleEncyclopedia(catalog);
        tableOfContents(book);
        search(book);
        categoryFilter(book);
        related(book);
        rejectsBadCategory();
        System.out.println("PASS: " + checks + " rule encyclopedia/search checks");
    }

    private static void normalizer() {
        check(TextNormalizer.normalize(" Infield-Fly ").equals("infieldfly"), "normalize removes space/case/punctuation");
        check(TextNormalizer.normalize("５.０９（ｂ）").equals("509b"), "normalize full-width");
        check(TextNormalizer.normalize(null).isEmpty(), "normalize null");
        check(TextNormalizer.sameText("인필드 플라이", "인필드플라이"), "same text ignoring space");
        check(!TextNormalizer.sameText("", " "), "blank is never an answer");
        check(TextNormalizer.matchesAny("  보크 ", List.of("balk", "보크")), "synonym answer");
        check(TextNormalizer.matchesAny("BALK", List.of("balk", "보크")), "case-insensitive answer");
        check(!TextNormalizer.matchesAny("보크아님", List.of("balk", "보크")), "partial answer rejected");
        check(!TextNormalizer.contains("abc", "  "), "blank query matches nothing");
        check(TextNormalizer.terms(" 2아웃   포스 ").equals(List.of("2아웃", "포스")), "split terms");
        extraDetail();
    }

    /** 백과사전이 같은 문장을 두 번 보여 주지 않도록, 요약에 없는 설명만 꺼낸다. */
    private static void extraDetail() {
        check(rule("요약입니다.", "요약입니다.").extraDetail().isEmpty(), "same text hidden");
        check(rule("요약입니다.", " 요약 입니다. ").extraDetail().isEmpty(), "spacing ignored");
        check(rule("요약입니다.", "요약입니다. 더 자세히.").extraDetail().equals("더 자세히."), "summary prefix removed");
        check(rule("요약입니다.", "전혀 다른 설명").extraDetail().equals("전혀 다른 설명"), "different detail kept");
        check(rule("", "설명만").extraDetail().equals("설명만"), "blank summary keeps detail");
    }
    private static RuleConcept rule(String summary, String detail) {
        return new RuleConcept("x", "topic", "제목", summary, detail, "출처", RuleCategory.BASIC, List.of());
    }

    private static void tableOfContents(RuleEncyclopedia book) {
        check(book.size() == 40, "40 rules");
        check(book.categories().equals(List.of(RuleCategory.values())), "all categories in declared order");
        Map<RuleCategory, List<RuleConcept>> toc = book.byCategory();
        check(toc.values().stream().mapToInt(List::size).sum() == 40, "every rule in exactly one category");
        check(toc.values().stream().noneMatch(List::isEmpty), "no empty category");
        check(ids(book.rulesIn(RuleCategory.PITCHING)).containsAll(List.of("balk", "pickoff", "pitching-position")), "pitching rules");
        check(book.rulesIn(RuleCategory.RECORD).stream().allMatch(r -> r.category() == RuleCategory.RECORD), "rulesIn filters");
        check(book.rulesIn(null).size() == 40, "rulesIn(null) = all");
        check(book.find("infield").map(RuleConcept::title).orElse("").equals("인필드 플라이"), "find by id");
        check(book.find("nope").isEmpty(), "unknown id");
        try { book.byCategory().clear(); throw new AssertionError("table of contents must be read-only"); }
        catch (UnsupportedOperationException expected) { checks++; }
    }

    private static void search(RuleEncyclopedia book) {
        for (String q : List.of("인필드 플라이", "인필드플라이", "INFIELD FLY", "infield-fly"))
            check(first(book.search(q)).equals("infield"), "infield fly first for: " + q);
        for (String q : List.of("보크", "balk", "BALK", "보크가 뭐야?"))
            check(first(book.search(q)).equals("balk"), "balk first for: " + q);
        check(first(book.search("태그업")).equals("tagup"), "exact title ranks first");
        check(first(book.search("방어율")).equals("era"), "keyword synonym");
        check(first(book.search("낫아웃")).equals("dropped-third"), "nickname keyword");
        check(first(book.search("견제")).equals("pickoff"), "pickoff");
        check(first(book.search("투구")).equals("pitching-position"), "pitching keyword");
        check(ids(book.search("스트라이크")).containsAll(List.of("foul", "count", "foul-tip")), "strike-related rules");
        check(ids(book.search("세이프")).contains("overrun"), "safe call");
        check(ids(book.search("6.02")).containsAll(List.of("balk", "pickoff")), "search by rule number");
        check(ids(book.search("포스 득점")).contains("force-run"), "multi-word fallback");
        check(book.search("없는단어xyz").isEmpty(), "no result");
        check(book.search("").size() == 40 && book.search("   ").size() == 40, "blank query lists all");
        check(ids(book.search("")).equals(ids(book.rulesIn(null))), "blank query keeps table order");

        List<RuleEncyclopedia.SearchHit> hits = book.searchHits("아웃", null);
        for (int i = 1; i < hits.size(); i++) check(hits.get(i - 1).score() >= hits.get(i).score(), "sorted by score");
        check(hits.get(0).rule().id().equals("outs") && hits.get(0).field() == RuleEncyclopedia.MatchField.KEYWORD, "exact keyword on top");
        check(hits.stream().filter(h -> h.field() == RuleEncyclopedia.MatchField.TITLE).count() >= 3, "title matches follow");
        RuleEncyclopedia.SearchHit detailOnly = book.searchHits("스윙하다가", null).stream()
                .filter(h -> h.rule().id().equals("hbp")).findFirst().orElseThrow();
        check(detailOnly.field() == RuleEncyclopedia.MatchField.DETAIL, "detail match reported");
    }

    private static void categoryFilter(RuleEncyclopedia book) {
        List<RuleConcept> umpire = book.search("방해", RuleCategory.UMPIRE);
        check(ids(umpire).containsAll(List.of("obstruction", "interference")), "umpire search");
        check(umpire.stream().allMatch(r -> r.category() == RuleCategory.UMPIRE), "filter keeps category");
        check(book.search("보크", RuleCategory.RECORD).isEmpty(), "filter excludes other categories");
        check(book.search("", RuleCategory.BATTING).size() == book.rulesIn(RuleCategory.BATTING).size(), "browse one category");
    }

    private static void related(RuleEncyclopedia book) {
        List<String> balk = ids(book.related("balk", 3));
        check(balk.size() <= 3 && !balk.contains("balk"), "related excludes self");
        check(balk.containsAll(List.of("pickoff", "pitching-position")), "related pitching rules");
        check(ids(book.related("obstruction", 5)).contains("interference"), "related by shared keyword");
        check(book.related("nope", 3).isEmpty() && book.related("balk", 0).isEmpty(), "related edge cases");
    }

    private static void rejectsBadCategory() throws Exception {
        Path temp = Files.createTempDirectory(Path.of("build"), "encyclopedia-test-");
        try (var paths = Files.walk(Path.of("data"))) {
            for (Path path : paths.toList()) {
                Path target = temp.resolve(Path.of("data").relativize(path).toString());
                if (Files.isDirectory(path)) Files.createDirectories(target); else Files.copy(path, target);
            }
        }
        Path rules = temp.resolve("rules.tsv");
        List<String> lines = new ArrayList<>(Files.readAllLines(rules, StandardCharsets.UTF_8));
        String[] cells = lines.get(1).split("\t", -1);
        cells[6] = "unknown";
        lines.set(1, String.join("\t", cells));
        Files.write(rules, lines, StandardCharsets.UTF_8);
        try { new Catalog(temp); throw new AssertionError("unknown category must be rejected"); }
        catch (IOException expected) { check(expected.getMessage().contains("rules.tsv"), "error names the file"); }
    }
}
