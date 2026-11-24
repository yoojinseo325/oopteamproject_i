package baseup.model;

/** 로그인 ID와 기존 학습 프로필 ID를 연결한다. 비밀번호 원문은 저장하지 않는다. */
public record UserAccount(String id, String profileId, String salt, String passwordHash)
        implements Identifiable { }
