package baseup.common;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * 검색어·답안 비교용 문자열 정규화.
 * 공백, 대소문자, 문장부호 차이를 무시한다. "인필드 플라이"와 "인필드플라이", "Infield-Fly"는 같은 값이 된다.
 * 규칙 검색(RuleEncyclopedia)과 단답형 채점(AnswerChecker)이 함께 쓴다.
 */
public final class TextNormalizer {
    private TextNormalizer() { }

    /** 전각/반각 통일(NFKC) → 소문자 → 글자와 숫자만 남긴다. null은 빈 문자열이다. */
    public static String normalize(String text) {
        if (text == null) return "";
        String unified = Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(unified.length());
        unified.codePoints().filter(Character::isLetterOrDigit).forEach(out::appendCodePoint);
        return out.toString();
    }

    /** 정규화한 뒤 text가 query를 포함하는지. 빈 검색어는 아무것도 포함하지 않는 것으로 본다. */
    public static boolean contains(String text, String query) {
        String q = normalize(query);
        return !q.isEmpty() && normalize(text).contains(q);
    }

    /** 정규화한 값이 같은지. 둘 중 하나라도 비어 있으면 false. */
    public static boolean sameText(String a, String b) {
        String x = normalize(a);
        return !x.isEmpty() && x.equals(normalize(b));
    }

    /** 입력이 허용 답안(동의어 포함) 중 하나와 같은지. 단답형 채점에 쓸 수 있다. */
    public static boolean matchesAny(String input, Collection<String> accepted) {
        return accepted.stream().anyMatch(answer -> sameText(input, answer));
    }

    /** 공백으로 나눈 검색어 조각들(정규화 후 빈 조각 제외). "2아웃 포스" → [2아웃, 포스]. */
    public static List<String> terms(String query) {
        if (query == null) return List.of();
        return Arrays.stream(query.strip().split("\\s+")).map(TextNormalizer::normalize)
                .filter(t -> !t.isEmpty()).distinct().toList();
    }
}
