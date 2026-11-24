package baseup.model;

/** 사용자별 즐겨찾기 메모. 규칙 북마크와 구분해 저장한다. */
public record NoteFavorite(String id, String userId, String noteId) implements Identifiable {
    public NoteFavorite {
        if (!id.equals(userId + ":" + noteId))
            throw new IllegalArgumentException("메모 즐겨찾기 ID 오류");
    }
}
