# BASEUP — 야구 학습 · 4지선다 퀴즈 (CardLayout 단계별 버전)

## 1. 실행하기

Windows에서 Java **JDK 17 이상**을 설치하고 프로젝트 폴더를 압축 해제한 다음:

- **`start.bat`**: 미리 빌드된 `BASEUP.jar` 실행 (Java 17 이상 실행 환경 필요)
- **`run.bat`**: `.java` 소스를 다시 컴파일하고 실행 (JDK 17 이상 필요)
- **`test.bat`**: 데이터 및 채점 자동 검사

VS Code에서 `BASEUP_Java_Project` 폴더를 연 뒤 `src/baseup/Main.java`를 실행해도 됩니다.
이때 **프로젝트 폴더를 작업 디렉터리로 유지**해야 `data/`, `images/`가 정상적으로 인식됩니다.

macOS/Linux에서는 `bash start.sh` 혹은 `bash run.sh`로 실행할 수 있습니다.

## 2. 교수님 예시를 적용한 화면 구조

- 메인 화면 전환: `src/baseup/ui/MainFrame.java` — `CardLayout`으로 홈, 학습, 퀴즈, 결과, 기록, 문제 관리를 교체
- 단계 선택 버튼: `src/baseup/ui/StageDeckPanel.java` — 상단에 **쉬움/중간/어려움/심판** 버튼 배치
- 단계 화면 전환: `StageDeckPanel` 내부의 두 번째 `CardLayout`으로 해당 단계의 화면 표시
- 학습 화면: `StudyPanel.java` — 이미지 클릭 시 용어와 설명 공개
- 퀴즈 화면: `QuizPanel.java` — 4지선다, 정답 제출, 해설 표시, 다음 문제
- 단계별 상태: 학습과 퀴즈에서 다른 단계로 이동했다가 돌아오면 기존 진행 지점 유지
- 완료 후 기록: 결과 자동 저장 후 결과 카드로 이동. 다른 단계의 진행 상태는 유지

*`JTabbedPane` 대신 교수님이 안내한 **상단 단계 버튼 + `CardLayout`** 대안으로 구현했습니다.*

## 3. 코드 담당별 구분

```
BASEUP_Java_Project/
├─ src/baseup/Main.java                  # 프로그램 시작
├─ src/baseup/ui/                        # 프론트엔드: Swing GUI
│  ├─ MainFrame.java                      # 메인 CardLayout
│  ├─ StageDeckPanel.java                 # 단계별 CardLayout + 선택 버튼
│  ├─ HomePanel.java, StudyPanel.java, QuizPanel.java
│  ├─ ResultPanel.java, HistoryPanel.java, QuestionManagePanel.java
│  └─ ImageView.java, Theme.java, GuiIntegrationTest.java
├─ src/baseup/model/                     # 백엔드: 객체 모델
│  └─ QuizQuestion.java, QuizResult.java
├─ src/baseup/data/                      # 백엔드: TXT 파일 입출력
│  └─ QuestionRepository.java, ResultRepository.java
├─ src/baseup/service/                   # 백엔드: 진행/채점
│  └─ QuizSession.java
├─ src/baseup/SmokeTest.java             # 자동 기능 검사
├─ data/questions.txt                    # 기획서 기반 총 40문항
├─ data/questions_backup.txt             # 원본 백업
├─ data/results.txt                      # 퀴즈 완료 시 자동 생성
├─ images/                               # 사용자가 제공한 이미지 40장
├─ BASEUP.jar                             # 빌드된 실행 JAR
└─ start.bat / run.bat / test.bat
```

## 4. 데이터 구조 및 변경법

`data/questions.txt`는 UTF-8 탭(`\t`) 구분 텍스트로, 1줄이 문제 하나입니다.
필드 순서는 ID, 유형, 이미지 파일명, 이미지 가이드, 학습 용어, 학습 설명,
상황 설명, 퀴즈 질문, 보기 1~4, 정답 번호(1~4), 해설의 14개입니다.

- `images/e-1.png`~`e-10.png`, `m-*`, `h-*`, `u-*` 총 40장이 연결됩니다.
- 문제 관리 화면의 검색/추가/삭제는 파일에 저장됩니다. 삭제 전에 백업하세요.
- 새 문제를 추가할 경우 ID(E11 등)와 이미지 파일명을 알맞게 정하고 `images/`에 넣으세요.
- 완료한 퀴즈는 `data/results.txt`로 저장됩니다.
- 학습/퀴즈 단계는 프로그램 내부에 총 4개 카드로 등록되어 있습니다.
- 문제 추가/삭제 후 학습/퀴즈 화면을 열면 데이터가 갱신됩니다. 이때 해당 모드에서 풀던 문제의 진행 상태는 초기화됩니다.

## 5. 테스트

소스 컴파일 후 프로젝트 폴더에서:

```
java -cp out baseup.SmokeTest
java -cp out baseup.ui.GuiIntegrationTest
```

두 번째 검사는 GUI가 표시되는 환경에서 실행해야 합니다.

## 6. 코드를 나누어 작업할 때

- **프론트**: `src/baseup/ui/` 안에서 화면/버튼/이벤트 수정
- **백엔드**: `src/baseup/model/`, `src/baseup/data/`, `src/baseup/service/`에서 문제/점수/파일 관리
- 인터페이스 협의: 프론트는 `QuestionRepository.getByCategory`, `QuizSession.submit`, `QuizSession.next` 등을 호출

Java Swing은 일반적으로 웹 프론트/서버 백이 아니라 한 프로그램 안에서 GUI와 데이터 처리 계층을 분리한 구조입니다.
