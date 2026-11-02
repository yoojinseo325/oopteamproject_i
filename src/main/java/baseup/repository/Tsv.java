package baseup.repository;

import java.util.ArrayList;
import java.util.List;

/** 탭, 줄바꿈, 역슬래시를 손실 없이 저장하는 작은 TSV 코덱. */
public final class Tsv {
    private Tsv() { }
    public static String line(String... fields) {
        List<String> encoded = new ArrayList<>();
        for (String value : fields) encoded.add(value.replace("\\", "\\\\")
                .replace("\t", "\\t").replace("\r", "\\r").replace("\n", "\\n"));
        return String.join("\t", encoded);
    }
    public static String[] fields(String line) {
        String[] parts = line.split("\t", -1);
        for (int p = 0; p < parts.length; p++) {
            StringBuilder out = new StringBuilder();
            String value = parts[p];
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c == '\\') {
                    if (++i == value.length()) throw new IllegalArgumentException("잘못된 이스케이프");
                    char escaped = value.charAt(i);
                    switch (escaped) {
                        case 't' -> out.append('\t');
                        case 'n' -> out.append('\n');
                        case 'r' -> out.append('\r');
                        case '\\' -> out.append('\\');
                        default -> throw new IllegalArgumentException("지원하지 않는 이스케이프: " + escaped);
                    }
                } else out.append(c);
            }
            parts[p] = out.toString();
        }
        return parts;
    }
}
