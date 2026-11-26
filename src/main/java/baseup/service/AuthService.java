package baseup.service;

import baseup.model.UserAccount;
import baseup.model.UserProfile;
import baseup.repository.FileRepository;
import baseup.repository.Repository;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import static baseup.repository.Tsv.line;

/** 서버 없이 로컬 사용자 ID/비밀번호를 기존 학습 프로필에 연결한다. */
public final class AuthService {
    /** 관리자 로그인 ID. 이 계정으로 로그인하면 모든 STEP이 열린다(시연·점검용). */
    public static final String ADMIN_ID = "admin";
    /** 관리자 계정이 처음 만들어질 때의 비밀번호. 실행 옵션 -Dbaseup.adminPassword=... 로 바꿀 수 있다. */
    public static final String DEFAULT_ADMIN_PASSWORD = "baseup1234";
    static final String ADMIN_NICKNAME = "관리자";
    private static final int ITERATIONS = 210_000;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();
    private final LearningService learning;
    private final Repository<UserAccount> repository;
    private final List<UserAccount> accounts;

    public AuthService(LearningService learning, Path userDir) throws IOException {
        this.learning = learning;
        repository = new FileRepository<>(userDir.resolve("accounts.tsv"),
                "id\tprofileId\tsalt\tpasswordHash",
                f -> new UserAccount(f[0], f[1], f[2], f[3]),
                a -> line(a.id(), a.profileId(), a.salt(), a.passwordHash()));
        accounts = new ArrayList<>(repository.load());
        for (UserAccount account : accounts) {
            if (learning.profiles().stream().noneMatch(p -> p.id().equals(account.profileId())))
                throw new IOException("계정에 연결된 학습 프로필이 없습니다: " + account.id());
        }
    }

    public List<UserProfile> unlinkedProfiles() {
        return learning.profiles().stream().filter(p -> accounts.stream()
                .noneMatch(a -> a.profileId().equals(p.id()))).toList();
    }

    public UserProfile authenticate(String loginId, char[] password) {
        String id = loginId.strip().toLowerCase(Locale.ROOT);
        UserAccount account = accounts.stream().filter(a -> a.id().equals(id)).findFirst().orElse(null);
        if (account == null || !MessageDigest.isEqual(
                hash(password, Base64.getDecoder().decode(account.salt())),
                Base64.getDecoder().decode(account.passwordHash()))) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        // 비밀번호가 검증된 경우에만 현재 학습 사용자를 변경한다.
        learning.selectUser(account.profileId());
        learning.setAllStepsUnlocked(isAdmin(id));
        return learning.user();
    }

    public static boolean isAdmin(String loginId) {
        return loginId != null && ADMIN_ID.equals(loginId.strip().toLowerCase(Locale.ROOT));
    }
    public boolean adminExists() { return accounts.stream().anyMatch(a -> a.id().equals(ADMIN_ID)); }

    /**
     * 관리자 계정(admin)이 없으면 만든다. 이미 있으면 아무것도 바꾸지 않는다(비밀번호도 그대로).
     * 닉네임 "관리자" 프로필이 계정 없이 남아 있으면 그 기록에 연결하고, 없으면 새 프로필을 만든다.
     * 현재 학습 사용자는 바꾸지 않는다.
     */
    public void ensureAdmin(char[] password) throws IOException {
        if (adminExists()) return;
        UserProfile before = learning.user();
        boolean unlockedBefore = learning.allStepsUnlocked();
        try {
            UserProfile spare = unlinkedProfiles().stream()
                    .filter(p -> p.nickname().equals(ADMIN_NICKNAME)).findFirst().orElse(null);
            if (spare != null) linkExisting(ADMIN_ID, password, spare.id());
            else {
                String name = learning.profiles().stream().anyMatch(p -> p.nickname().equalsIgnoreCase(ADMIN_NICKNAME))
                        ? ADMIN_NICKNAME + "_admin" : ADMIN_NICKNAME;
                registerNew(ADMIN_ID, password, name);
            }
        } finally {
            learning.selectUser(before.id());
            learning.setAllStepsUnlocked(unlockedBefore);
        }
    }

    public UserProfile registerNew(String loginId, char[] password, String nickname) throws IOException {
        String id = validate(loginId, password);
        String name = nickname.strip();
        if (name.isEmpty() || name.length() > 16 || name.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("닉네임은 1~16자로 입력해 주세요.");
        // 저장 실패 전에 유효성·ID 중복·암호 해시를 최대한 검증한다.
        String[] credential = credential(password);
        UserProfile profile = learning.addProfile(name);
        try {
            saveAccount(new UserAccount(id, profile.id(), credential[0], credential[1]));
        } catch (IOException e) {
            throw new IOException("계정 저장 실패: 학습 프로필은 생성되었습니다. 다시 실행한 뒤 '기존 기록 연결'로 복구해 주세요.", e);
        }
        return profile;
    }

    /** 기존 userdata/profiles.tsv의 사용자별 학습 기록을 변경하지 않고 로그인 ID만 연결한다. */
    public UserProfile linkExisting(String loginId, char[] password, String profileId) throws IOException {
        String id = validate(loginId, password);
        UserProfile profile = unlinkedProfiles().stream().filter(p -> p.id().equals(profileId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("이미 연결되었거나 없는 학습 프로필입니다."));
        String[] credential = credential(password);
        saveAccount(new UserAccount(id, profile.id(), credential[0], credential[1]));
        return profile;
    }

    private String validate(String raw, char[] password) {
        String id = raw.strip().toLowerCase(Locale.ROOT);
        if (!id.matches("[a-z0-9_]{4,20}"))
            throw new IllegalArgumentException("아이디는 영문, 숫자, 밑줄(_)로 4~20자 입력해 주세요.");
        if (accounts.stream().anyMatch(a -> a.id().equals(id)))
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        if (password.length < 8 || password.length > 128)
            throw new IllegalArgumentException("비밀번호는 8~128자로 입력해 주세요.");
        return id;
    }

    private void saveAccount(UserAccount account) throws IOException {
        List<UserAccount> next = new ArrayList<>(accounts);
        next.add(account);
        repository.save(next); // 디스크 저장 성공 후 메모리에 반영
        accounts.add(account);
    }

    private static String[] credential(char[] password) {
        byte[] salt = new byte[16]; RANDOM.nextBytes(salt);
        byte[] bytes = hash(password, salt);
        try {
            return new String[]{Base64.getEncoder().encodeToString(salt),
                    Base64.getEncoder().encodeToString(bytes)};
        } finally { Arrays.fill(bytes, (byte) 0); }
    }

    private static byte[] hash(char[] password, byte[] salt) {
        KeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("비밀번호 해시 처리가 실패했습니다.", e);
        } finally { ((PBEKeySpec) spec).clearPassword(); }
    }
}
