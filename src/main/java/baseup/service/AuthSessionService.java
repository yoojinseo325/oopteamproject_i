package baseup.service;

import baseup.model.Attempt;
import baseup.model.UserProfile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 로그인 상태 관리(기능 3). 회원가입·로그인·로그아웃과 "지금 누가 로그인했는가"를 담당한다.
 *
 * 비밀번호 검사와 계정 저장은 AuthService(feature/login-mypage, ywmh00 작성)가 하고,
 * 이 클래스는 그 위에서 세션 상태를 관리한다. 로그인하면 LearningService의 현재 사용자가 바뀌므로
 * 이후의 퀴즈 제출·북마크·메모가 모두 그 사용자 기록으로 저장된다(사용자별 퀴즈 기록 연결).
 *
 * <pre>
 * AuthSessionService session = new AuthSessionService(new AuthService(learning, userDir), learning);
 * session.signUp("tester01", password, "닉네임");   // 가입 후 바로 로그인
 * session.login("tester01", password);             // 실패하면 IllegalArgumentException
 * session.isLoggedIn(); session.currentUser();
 * session.addListener(() -> refreshScreen());      // 로그인/로그아웃 때 화면 갱신
 * session.logout();
 * </pre>
 *
 * 전달받은 비밀번호 배열은 성공·실패와 관계없이 사용 후 0으로 지운다.
 */
public final class AuthSessionService {
    private final AuthService auth;
    private final LearningService learning;
    private final List<Runnable> listeners = new ArrayList<>();
    private String loginId;

    public AuthSessionService(AuthService auth, LearningService learning) {
        this.auth = auth;
        this.learning = learning;
    }

    /** 새 계정을 만들고 바로 로그인한다. 학습 기록은 새 프로필로 시작한다. */
    public UserProfile signUp(String id, char[] password, String nickname) throws IOException {
        try {
            auth.registerNew(id, password, nickname);
            return start(id, password);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    /** 아이디·비밀번호가 맞으면 로그인하고 그 사용자의 기록으로 전환한다. 틀리면 기존 로그인 상태를 유지한다. */
    public UserProfile login(String id, char[] password) {
        try {
            return start(id, password);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    /** 로그아웃. 학습 기록은 파일에 남아 있어 다시 로그인하면 이어서 볼 수 있다. */
    public void logout() {
        if (loginId == null) return;
        loginId = null;
        notifyListeners();
    }

    public boolean isLoggedIn() { return loginId != null; }

    /** 로그인한 아이디(소문자로 정리된 값). */
    public Optional<String> loginId() { return Optional.ofNullable(loginId); }

    /** 로그인한 사용자의 학습 프로필. 로그아웃 상태면 비어 있다. */
    public Optional<UserProfile> currentUser() {
        return isLoggedIn() ? Optional.of(learning.user()) : Optional.empty();
    }

    /** 로그인이 필요한 기능에서 호출한다. 로그아웃 상태면 IllegalStateException. */
    public UserProfile requireUser() {
        return currentUser().orElseThrow(() -> new IllegalStateException("로그인이 필요합니다."));
    }

    /** 로그인한 사용자의 퀴즈 제출 기록(오래된 순). */
    public List<Attempt> myAttempts() {
        requireUser();
        return learning.attempts();
    }

    /** 로그인·로그아웃 때마다 실행할 작업(화면 갱신 등)을 등록한다. */
    public void addListener(Runnable listener) { listeners.add(listener); }

    private UserProfile start(String id, char[] password) {
        // AuthService.authenticate는 성공했을 때만 현재 학습 사용자를 바꾼다.
        UserProfile profile = auth.authenticate(id, password);
        loginId = id.strip().toLowerCase(java.util.Locale.ROOT);
        notifyListeners();
        return profile;
    }

    private void notifyListeners() { List.copyOf(listeners).forEach(Runnable::run); }
}
