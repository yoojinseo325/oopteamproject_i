package baseup.shortapp.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 12열 단답형 데이터와 17열 혼합형 데이터를 안전하게 이관한다. */
public final class QuestionMigration {
    private QuestionMigration() { }
    /** 기존 12열(전부 단답형) 사용자 데이터를 보존하며 17열 혼합형으로 업그레이드한다. */
    public static void ensureCurrentData(Path root) throws IOException {
        Path target = root.resolve("data/short_questions.tsv");
        if (!Files.isRegularFile(target)) {
            try (InputStream in = QuestionMigration.class.getResourceAsStream("/baseup-assets/short_questions.tsv")) {
                if (in == null) throw new IOException("혼합형 문제 기본 데이터가 없습니다");
                Files.createDirectories(target.getParent());
                Files.copy(in, target);
            }
            return;
        }
        List<String> existing = Files.readAllLines(target, StandardCharsets.UTF_8);
        boolean hasOld = false;
        for (String line:existing) {
            if (line.isBlank() || line.startsWith("#")) continue;
            int columns = line.split("\t",-1).length;
            if (columns == 12) hasOld=true;
            else if (columns != 17) throw new IOException("문제 데이터 열 수 오류: " + columns);
        }
        if (!hasOld) return;
        Map<String,String[]> defaults = new HashMap<>();
        try (InputStream in = QuestionMigration.class.getResourceAsStream("/baseup-assets/short_questions.tsv")) {
            if (in == null) throw new IOException("혼합형 문제 기본 데이터가 없습니다");
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            for (String line:text.split("\\R")) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] fields = line.split("\t", -1);
                if (fields.length == 17) defaults.put(fields[0],fields);
            }
        }
        ArrayList<String> migrated = new ArrayList<>();
        migrated.add("# 기존 데이터 보존 + 객관식/단답형 혼합 모드 자동 마이그레이션");
        for (String line:existing) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] fields = line.split("\t", -1);
            if (fields.length == 17) { migrated.add(line); continue; }
            String[] template = defaults.get(fields[0]);
            if (template != null && fields[8].equals(template[8])) {
                migrated.add(line + "\t" + String.join("\t", java.util.Arrays.copyOfRange(template,12,17)));
            } else {
                // 사용자가 수정한 정답은 멋대로 보기와 연결하지 않고 단답형으로 보존
                migrated.add(line + "\tSHORT_ANSWER\t\t\t\t");
            }
        }
        Path backup = target.resolveSibling("short_questions.pre-mixed.tsv");
        if (!Files.exists(backup)) Files.copy(target, backup);
        Path tmp = target.resolveSibling("short_questions.migration.tmp");
        Files.write(tmp,migrated,StandardCharsets.UTF_8);
        Files.move(tmp,target,StandardCopyOption.REPLACE_EXISTING);
    }

}
