# BASEUP 백엔드 작업물 — 담당 4·7·8번

이 코드는 이전에 제작된 `BASEUP_Launcher40_Integrated` (package `baseup.shortapp`)를 기반으로 고친 버전입니다. **현재 팀 GitHub `main`의 52문항 프로젝트(package 구조 별도)와는 파일을 그대로 덮어쓰는 방식으로 통합하지 마세요.** 우선 새로운 브랜치의 별도 폴더에 올리고, 프론트와 데이터를 연결하면서 기존 팀 코드에 이식하세요.

## 변경사항

- **4번: 마이페이지 통계** `StatisticsManager`, `UserStatistics`: 사용자별 퀴즈 풀이 수, 정답/오답, 도전 횟수, 난이도별 **회차 평균 정답률**, 최근 5회 점수, 최근 학습 기록을 계산합니다.
- **7번: 혼합형 40문항** E/M/H/U 각각 10문항, 그중 **객관식 5개 + 단답형 5개**. `QuestionType`, `QuizQuestion`, `AnswerChecker`, `QuizSession`, `QuizResult`, `QuestionRepository`가 관련 클래스입니다. 사용자별 퀴즈 기록은 기존 `ResultRepository`를 그대로 사용합니다.
- **8번: 영상형 시나리오** 기존 문항 중 U03(객관식), U08(단답형), U09(객관식)에 교육용 애니메이션을 연결. `ScenarioManager`가 안전한 파일 경로·메타데이터를 제공하며 UI `ScenarioPlayer`가 재생하고 **채점은 일반 `QuizSession`과 동일**하게 수행합니다.
- **기존 기능 유지**: 로그인, 규칙 검색, 백과사전, 오늘의 규칙, CardLayout은 삭제하거나 교체하지 않았습니다.

## 프론트팀과 합의할 공개 메서드

```java
BackendFacade backend = new BackendFacade(questionRepository, userHomeDirectory, assetsDirectory);
QuizSession session = backend.startQuiz("U");
QuizQuestion question = session.getCurrentQuestion();

if (question.isMultipleChoice()) {
    // question.getOptions() : 보기 4개, 1부터 4까지 번호를 받습니다.
    boolean correct = session.submitChoice(2);
} else {
    // 문제별 대표 정답 / 허용 정답은 백엔드에서 관리합니다.
    boolean correct = session.submit("태그업");
}

if (question.hasScenario()) {
    var media = backend.getScenario(question);
    // media.gifPath(), media.mp4Path()를 프론트에서 렌더링
}

session.next(); // 정답 제출한 뒤 호출
// 10문제 전부 끝난 뒤에만:
QuizResult result = backend.saveFinishedQuiz(session);
UserStatistics stats = backend.getStatistics();
```

**중요**: 현재 로그인 계정의 폴더를 `userHomeDirectory`로 전달해야 다른 사용자의 퀴즈 기록과 섞이지 않습니다. 백엔드 `BackendFacade`를 사용하지 않아도 기존 `MainFrame`의 결과 저장/통계 로직과 연동됩니다.

## 파일 형식

`data/short_questions.tsv`: UTF-8, **17열**, 첫 12열은 구버전과 호환됩니다.

1~12열 = ID, 난이도, 이미지, 이미지 설명, 규칙명, 학습 설명, 상황 설명, 문제 본문, 대표 정답, 허용 정답(`|` 구분), 해설, 시나리오 GIF

13열 = `MULTIPLE_CHOICE` 또는 `SHORT_ANSWER`, 14~17열 = 보기 1~4 (단답형은 공백)

- 객관식은 대표 정답 문자열과 **완전히 일치하는 보기**가 하나 있어야 하며, UI에서 선택 번호(1~4)를 백엔드에 보냅니다.
- 단답형은 `정답 문자열`과 `허용 표현`을 비교하고 공백·기호·대소문자 차이를 처리합니다. 오탈자를 무조건 정답으로 인정하지는 않습니다.
- 기존 12열 단답형 데이터를 사용하는 경우 첫 실행 시 **원본 백업 파일을 보존**하면서 17열 혼합형으로 변환합니다. 기존 정답이 바뀐 문항은 임의로 객관식이 되지 않고 단답형으로 보존됩니다.

## 테스트 및 빌드

- Windows: `TEST_BACKEND.bat` 실행 (JDK 17 이상 필요)
- Bash: `bash test_backend.sh`
- Windows 실행: `START_BASEUP.bat`
- Windows 코드 재빌드: `BUILD_AND_RUN.bat`
- Bash 빌드: `bash build.sh` 이후 `java -jar BASEUP.jar`

`src/test/java/baseup/shortapp/MixedBackendTest.java`는 문항 40개, 문제유형 20:20, 채점, 시나리오 3개, 결과 저장, 대시보드 및 사용자 기록 분리를 검사합니다.

## GitHub에서 합칠 때

1. Git Bash에서 팀 프로젝트 폴더(`oopteamproject_i`)로 이동한 뒤 `git switch main && git pull`.
2. 기존 팀원 코드 보존을 위해 `git switch -c feature/backend-stats-mixed-scenario`.
3. 이 배포 파일의 `BACKEND_MERGE_FILES` 하위 내용을 팀 저장소의 새 폴더로 복사합니다. 기존 `src` 폴더를 덮어쓰지 않습니다.
4. `git add BACKEND_MERGE_FILES`, `git commit -m "Add stats mixed quiz and scenario backend"`, `git push -u origin feature/backend-stats-mixed-scenario`.
5. 프론트팀과 패키지/공개 메서드 계약을 맞춘 후 실제 `src`로 병합하고 PR을 만듭니다.

이 가이드는 업로드 절차를 안내하며, **사용자의 GitHub로 자동 푸시하지는 않습니다**.
