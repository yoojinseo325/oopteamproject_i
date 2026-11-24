# BASEUP 담당 백엔드 4/7/8번

**이 폴더는 GitHub 신규 브랜치에 독립 폴더로 올리는 용도입니다.** 팀원들이 작성한 기존 src를 바로 덮어쓰지 마세요.

- 4. 통계: 사용자별 누적 정오답/풀이 수, 도전 횟수, 난이도별 회차 평균 점수, 최근 학습 기록.
- 7. 혼합 퀴즈: 기존 총 40문항, 난이도별 객관식 5 + 단답형 5 (총 20 + 20), 동일 세션에서 채점/점수/답안 저장.
- 8. 동적 시나리오: 기존 U03/U08/U09 문제에 영상 경로를 제공하고 QuizSession으로 채점. 영상 파일 자체는 풀 프로젝트에 있습니다.

**이 백엔드 패치는 이전 BASEUP_Launcher40_Integrated 버전(`baseup.shortapp`)의 구조입니다.** 팀 GitHub의 다른 패키지/프로젝트에 병합할 때는 `BACKEND_INTEGRATION_GUIDE.md`를 참고하세요.

테스트: Windows는 JDK 17+ 설치 후 `TEST_BACKEND.bat` 실행, Linux/macOS는 `bash test_backend.sh`.
