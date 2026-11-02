package baseup.repository;

import baseup.model.Identifiable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;

/** UTF-8 텍스트 저장소. 임시 파일을 완성한 뒤 교체하여 저장 실패 시 기존 파일을 보존한다. */
public final class FileRepository<T extends Identifiable> implements Repository<T> {
    private final Path path;
    private final String header;
    private final Function<String[], T> decode;
    private final Function<T, String> encode;

    public FileRepository(Path path, String header, Function<String[], T> decode, Function<T, String> encode) {
        this.path = path; this.header = header; this.decode = decode; this.encode = encode;
    }
    @Override public List<T> load() throws IOException {
        List<T> result = new ArrayList<>();
        if (!Files.exists(path)) return result;
        Set<String> ids = new HashSet<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String first = reader.readLine();
            if (first != null && first.startsWith("\uFEFF")) first = first.substring(1);
            if (!header.equals(first)) throw new IOException(path.getFileName() + ": 파일 헤더가 올바르지 않습니다.");
            int row = 1;
            for (String line; (line = reader.readLine()) != null;) {
                row++;
                if (line.isBlank()) continue;
                try {
                    String[] fields = Tsv.fields(line);
                    if (fields.length != header.split("\t", -1).length) throw new IllegalArgumentException("열 개수 오류");
                    T item = decode.apply(fields);
                    if (item.id().isBlank() || !ids.add(item.id())) throw new IllegalArgumentException("빈 ID 또는 중복 ID");
                    result.add(item);
                } catch (RuntimeException e) {
                    throw new IOException(path.getFileName() + " " + row + "행: " + e.getMessage(), e);
                }
            }
        }
        return result;
    }
    @Override public void save(List<T> items) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        Path pending = Files.createTempFile(path.toAbsolutePath().getParent(), ".baseup-", ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(pending, StandardCharsets.UTF_8)) {
                writer.write(header); writer.newLine();
                for (T item : items) { writer.write(encode.apply(item)); writer.newLine(); }
            }
            try { Files.move(pending, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(pending, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(pending); }
    }
}
